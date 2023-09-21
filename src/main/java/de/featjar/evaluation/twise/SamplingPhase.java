/*
 * Copyright (C) 2023 Sebastian Krieter
 *
 * This file is part of evaluation-sampling-algorithms.
 *
 * evaluation-sampling-algorithms is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * evaluation-sampling-algorithms is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with evaluation-sampling-algorithms. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <> for further information.
 */
package de.featjar.evaluation.twise;

import de.featjar.clauses.CNF;
import de.featjar.clauses.CNFProvider;
import de.featjar.clauses.solutions.SolutionList;
import de.featjar.clauses.solutions.io.PartialListFormat;
import de.featjar.evaluation.EvaluationPhase;
import de.featjar.evaluation.Evaluator;
import de.featjar.evaluation.process.Algorithm;
import de.featjar.evaluation.process.ProcessRunner;
import de.featjar.evaluation.process.Result;
import de.featjar.evaluation.twise.algorithms.ACTS_IPOF2;
import de.featjar.evaluation.twise.algorithms.ACTS_IPOF_CSP;
import de.featjar.evaluation.twise.algorithms.ACTS_IPOF_FT;
import de.featjar.evaluation.twise.algorithms.ACTS_IPOGD;
import de.featjar.evaluation.twise.algorithms.ACTS_IPOG_CSP;
import de.featjar.evaluation.twise.algorithms.ACTS_IPOG_FT;
import de.featjar.evaluation.twise.algorithms.Dummy;
import de.featjar.evaluation.twise.algorithms.FIDEChvatal;
import de.featjar.evaluation.twise.algorithms.FIDEICPL;
import de.featjar.evaluation.twise.algorithms.FIDEIncLing;
import de.featjar.evaluation.twise.algorithms.FIDEYASA;
import de.featjar.evaluation.twise.algorithms.YASA;
import de.featjar.evaluation.util.ModelReader;
import de.featjar.formula.ModelRepresentation;
import de.featjar.formula.io.FormulaFormatManager;
import de.featjar.formula.io.dimacs.DIMACSFormatCNF;
import de.featjar.formula.structure.Formula;
import de.featjar.formula.structure.atomic.literal.VariableMap;
import de.featjar.util.io.IO;
import de.featjar.util.io.csv.CSVWriter;
import de.featjar.util.logging.Logger;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Sebastian Krieter
 */
public class SamplingPhase implements EvaluationPhase {

    private final DIMACSFormatCNF modelFormat = new DIMACSFormatCNF();
    private final PartialListFormat sampleFormat = new PartialListFormat();

    private List<Algorithm<SolutionList>> algorithmList;

    private CSVWriter featureGroupWriter, dataWriter, modelWriter, algorithmWriter;
    private int algorithmIndex, algorithmIteration;
    private Result<SolutionList> result;
    private CNF modelCNF;

    private TWiseSampleEvaluator tWiseEvaluator;

    final int numOfFeatureGroups = 10;
    final int maxMemberships = 3;
    List<String>[] featureGroups = new List[numOfFeatureGroups];
    HashMap<String, HashSet<Integer>> featureGroupMap = new HashMap<>();

    @Override
    public void run(Evaluator evaluator) {
        tWiseEvaluator = (TWiseSampleEvaluator) evaluator;

        featureGroupWriter = evaluator.addCSVWriter("feature_groups.csv", "Feature group", "Features");
        modelWriter = evaluator.addCSVWriter("models.csv", "ModelID", "Name", "#Variables", "#Clauses");
        algorithmWriter = evaluator.addCSVWriter("algorithms.csv", "AlgorithmID", "Name", "Settings");
        dataWriter = evaluator.addCSVWriter(
                "data.csv",
                "ModelID",
                "AlgorithmID",
                "SystemIteration",
                "AlgorithmIteration",
                "InTime",
                "Success",
                "Time",
                "SampleSize");

        featureGroupWriter.setLineWriter(this::writeFeatureGroups);
        modelWriter.setLineWriter(this::writeModel);
        algorithmWriter.setLineWriter(this::writeAlgorithm);
        dataWriter.setLineWriter(this::writeData);

        final ModelReader<Formula> mr = new ModelReader<>();
        mr.setPathToFiles(tWiseEvaluator.modelPath);
//        Logger.logInfo("modelPath: " + tWiseEvaluator.modelPath);
        mr.setFormatSupplier(FormulaFormatManager.getInstance());

        if (evaluator.systemIterations.getValue() > 0) {
            evaluator.tabFormatter.setTabLevel(0);
            Logger.logInfo("Start");

            final ProcessRunner processRunner = new ProcessRunner();
            processRunner.setTimeout(evaluator.timeout.getValue());

            prepareAlgorithms();

            systemLoop:
            for (evaluator.systemIndex = 0; evaluator.systemIndex < evaluator.systemIndexMax; evaluator.systemIndex++) {
                evaluator.tabFormatter.setTabLevel(1);
                evaluator.logSystem();

                if (!readModel(mr)) {
                    continue systemLoop;
                }

                for (evaluator.systemIteration = 1;
                        evaluator.systemIteration <= evaluator.systemIterations.getValue();
                        evaluator.systemIteration++) {
                    if (!adaptModel()) {
                        continue systemLoop;
                    }

                    algorithmIndex = -1;
                    algorithmLoop:
                    for (final Algorithm<SolutionList> algorithm : algorithmList) {
                        algorithmIndex++;
                        for (algorithmIteration = 1;
                                algorithmIteration <= algorithm.getIterations();
                                algorithmIteration++) {

                            evaluator.tabFormatter.setTabLevel(2);
                            logRun();
                            evaluator.tabFormatter.setTabLevel(3);

                            String sampleFileName = tWiseEvaluator.getSystemID() + "_" + tWiseEvaluator.systemIteration
                                    + "_" + algorithmIndex + "_" + algorithmIteration + "_sample."
                                    + sampleFormat.getFileExtension();
                            try {
                                result = processRunner.run(algorithm);
                                dataWriter.writeLine();
                                final SolutionList sample = result.getResult();
                                if (sample != null) {
                                    IO.save(sample, tWiseEvaluator.outputPath.resolve(sampleFileName), sampleFormat);
                                }
                            } catch (final Exception e) {
                                Logger.logError("Could not save sample file " + sampleFileName);
                                Logger.logError(e);
                                continue algorithmLoop;
                            }
                        }
                    }
                }
            }
            evaluator.tabFormatter.setTabLevel(0);
            Logger.logInfo("Finished");
        } else {
            Logger.logInfo("Nothing to do");
        }
    }

    private boolean readModel(final ModelReader<Formula> mr) {
        modelCNF = mr.read(tWiseEvaluator.getSystemName())
                .map(ModelRepresentation::new)
                .map(m -> m.get(CNFProvider.fromFormula()))
                .orElse(Logger::logProblems);


        createFeatureGroupMap();
        updateFeatureGroupAttributes();
        featureGroupWriter.writeLine();
        logCNF();

        if (modelCNF == null) {
            Logger.logError("Could not read file " + tWiseEvaluator.getSystemName());
            return false;
        }
        final String orgModelFileName = tWiseEvaluator.getSystemID() + "_org_model." + modelFormat.getFileExtension();
        try {
            IO.save(modelCNF, tWiseEvaluator.outputPath.resolve(orgModelFileName), modelFormat);
        } catch (IOException e) {
            Logger.logError("Could not save model file " + orgModelFileName);
            Logger.logError(e);
            return false;
        }
        modelWriter.writeLine();
        return true;
    }

    protected void prepareAlgorithms() {
        algorithmList = new ArrayList<>();

        for (final String algorithmName : tWiseEvaluator.algorithmsProperty.getValue()) {
            for (final Integer tValue : tWiseEvaluator.tProperty.getValue()) {
                final Path sampleFile = tWiseEvaluator.tempPath.resolve("sample.csv");
                final Path modelFile = tWiseEvaluator.tempPath.resolve("model.dimacs");
                switch (algorithmName) {
                    case "DUMMY": {
                        algorithmList.add(new Dummy());
                        break;
                    }
                    case "IPOG_FT": {
                        algorithmList.add(new ACTS_IPOG_FT(tValue, sampleFile, modelFile));
                        break;
                    }
                    case "IPOG_CSP": {
                        algorithmList.add(new ACTS_IPOG_CSP(tValue, sampleFile, modelFile));
                        break;
                    }
                    case "IPOG_D": {
                        algorithmList.add(new ACTS_IPOGD(tValue, sampleFile, modelFile));
                        break;
                    }
                    case "IPOF_FT": {
                        algorithmList.add(new ACTS_IPOF_FT(tValue, sampleFile, modelFile));
                        break;
                    }
                    case "IPOF_CSP": {
                        algorithmList.add(new ACTS_IPOF_CSP(tValue, sampleFile, modelFile));
                        break;
                    }
                    case "IPOF2": {
                        algorithmList.add(new ACTS_IPOF2(tValue, sampleFile, modelFile));
                        break;
                    }
                    case "IC": {
                        algorithmList.add(new FIDEICPL(tValue, sampleFile, modelFile));
                        break;
                    }
                    case "CH": {
                        algorithmList.add(new FIDEChvatal(tValue, sampleFile, modelFile));
                        break;
                    }
                    case "IL": {
                        if (tValue == 2) {
                            final FIDEIncLing incLing = new FIDEIncLing(sampleFile, modelFile);
                            incLing.setSeed(tWiseEvaluator.randomSeed.getValue());
                            algorithmList.add(incLing);
                        }
                        break;
                    }
                    case "YA": {
                        final FIDEYASA yasa = new FIDEYASA(sampleFile, modelFile);
                        yasa.setT(tValue);
                        yasa.setM(1);
                        yasa.setSeed(tWiseEvaluator.randomSeed.getValue());
                        algorithmList.add(yasa);
                        break;
                    }
                    case "YA3": {
                        final FIDEYASA yasa = new FIDEYASA(sampleFile, modelFile);
                        yasa.setT(tValue);
                        yasa.setM(3);
                        yasa.setSeed(tWiseEvaluator.randomSeed.getValue());
                        algorithmList.add(yasa);
                        break;
                    }
                    case "YA5": {
                        final FIDEYASA yasa = new FIDEYASA(sampleFile, modelFile);
                        yasa.setT(tValue);
                        yasa.setM(5);
                        yasa.setSeed(tWiseEvaluator.randomSeed.getValue());
                        algorithmList.add(yasa);
                        break;
                    }
                    case "YA10": {
                        final FIDEYASA yasa = new FIDEYASA(sampleFile, modelFile);
                        yasa.setT(tValue);
                        yasa.setM(10);
                        yasa.setSeed(tWiseEvaluator.randomSeed.getValue());
                        algorithmList.add(yasa);
                        break;
                    }
                    case "NYA": {
                        final YASA yasa = new YASA(sampleFile, modelFile);
                        yasa.setT(tValue);
                        yasa.setM(1);
                        yasa.setSeed(tWiseEvaluator.randomSeed.getValue());
                        algorithmList.add(yasa);
                        break;
                    }
                    case "NYA3": {
                        final YASA yasa = new YASA(sampleFile, modelFile);
                        yasa.setT(tValue);
                        yasa.setM(3);
                        yasa.setSeed(tWiseEvaluator.randomSeed.getValue());
                        algorithmList.add(yasa);
                        break;
                    }
                    case "NYA5": {
                        final YASA yasa = new YASA(sampleFile, modelFile);
                        yasa.setT(tValue);
                        yasa.setM(5);
                        yasa.setSeed(tWiseEvaluator.randomSeed.getValue());
                        algorithmList.add(yasa);
                        break;
                    }
                    case "NYA10": {
                        final YASA yasa = new YASA(sampleFile, modelFile);
                        yasa.setT(tValue);
                        yasa.setM(10);
                        yasa.setSeed(tWiseEvaluator.randomSeed.getValue());
                        algorithmList.add(yasa);
                        break;
                    }
                }
            }
        }
        algorithmIndex = 0;
        for (final Algorithm<SolutionList> algorithm : algorithmList) {
            algorithm.setIterations(tWiseEvaluator.algorithmIterations.getValue());
            algorithmWriter.writeLine();
            algorithmIndex++;
        }
        algorithmIndex = 0;
    }

    protected boolean adaptModel() {
        final CNF randomCNF =
                modelCNF.randomize(new Random(tWiseEvaluator.randomSeed.getValue() + tWiseEvaluator.systemIteration));
        try {
            IO.save(
                    randomCNF,
                    tWiseEvaluator.tempPath.resolve("model" + "." + modelFormat.getFileExtension()),
                    modelFormat);
            IO.save(
                    randomCNF,
                    tWiseEvaluator.outputPath.resolve(tWiseEvaluator.getSystemID() + "_"
                            + tWiseEvaluator.systemIteration + "_rnd_model." + modelFormat.getFileExtension()),
                    modelFormat);
        } catch (IOException e) {
            Logger.logError(e);
            return false;
        }
        return true;
    }

    protected void createFeatureGroupMap() {
        List<String> variableNames = modelCNF.getVariableMap().getVariableNames();
        Random rnd = new Random();

        for (String variableName : variableNames) {
            int numOfMemberships = rnd.nextInt(maxMemberships) + 1;
            HashSet<Integer> memberships = new HashSet<>();


            for (int i = 0; i < numOfMemberships; i++) {
                int randomValue = rnd.nextInt(numOfFeatureGroups) + 1;
                memberships.add(randomValue);
            }

            featureGroupMap.put(variableName, memberships);
        }
    }

    protected void updateFeatureGroupAttributes() {
        for (String variableName : featureGroupMap.keySet()) {
            Optional<VariableMap.Variable> var = modelCNF.getVariableMap().getVariable(variableName);

            if(var.isEmpty())  { throw new IllegalArgumentException("Invalid variableName: " + variableName);}

            var.get().setFeatureGroups(featureGroupMap.get(variableName));
        }
    }

    protected void writeFeatureGroups(CSVWriter featureGroupWriter) {
        createFeatureGroupArrays();
        for (int i = 0; i < numOfFeatureGroups; i++) {
            featureGroupWriter.addValue(i + 1);

            for (String featureLabel : featureGroups[i]) {
                featureGroupWriter.addValue(featureLabel);
            }
            featureGroupWriter.createNewLine();
        }
    }

    protected void createFeatureGroupArrays() {
        for (int i = 0; i < numOfFeatureGroups; i++) {
            featureGroups[i] = new ArrayList<>();
        }

        for (String variableName : featureGroupMap.keySet()) {
            for (int group : featureGroupMap.get(variableName)) {
                if (group < 1 || group > featureGroups.length) {
                    throw new IllegalArgumentException("Invalid group number: " + group);
                }
                featureGroups[group - 1].add(variableName);
            }
        }
    }

    protected void writeModel(CSVWriter modelCSVWriter) {
        modelCSVWriter.addValue(tWiseEvaluator.getSystemID());
        modelCSVWriter.addValue(tWiseEvaluator.getSystemName());
        modelCSVWriter.addValue(modelCNF.getVariableMap().getVariableCount());
        modelCSVWriter.addValue(modelCNF.getClauses().size());
    }

    protected void writeAlgorithm(CSVWriter algorithmCSVWriter) {
        final Algorithm<?> algorithm = algorithmList.get(algorithmIndex);
        algorithmCSVWriter.addValue(algorithmIndex);
        algorithmCSVWriter.addValue(algorithm.getName());
        algorithmCSVWriter.addValue(algorithm.getParameterSettings());
    }

    protected void writeData(CSVWriter dataCSVWriter) {
        dataCSVWriter.addValue(tWiseEvaluator.getSystemID());
        dataCSVWriter.addValue(algorithmIndex);
        dataCSVWriter.addValue(tWiseEvaluator.systemIteration);
        dataCSVWriter.addValue(algorithmIteration);
        dataCSVWriter.addValue(result.isTerminatedInTime());
        dataCSVWriter.addValue(result.isNoError());
        dataCSVWriter.addValue(result.getTime());
        SolutionList sample = result.getResult();
        dataCSVWriter.addValue(sample != null ? sample.getSolutions().size() : 0);
    }

    private void logRun() {
        final StringBuilder sb = new StringBuilder();
        sb.append(tWiseEvaluator.getSystemName());
        sb.append(" (");
        sb.append(tWiseEvaluator.systemIndex + 1);
        sb.append("/");
        sb.append(tWiseEvaluator.systemNames.size());
        sb.append(") ");
        sb.append(tWiseEvaluator.systemIteration);
        sb.append("/");
        sb.append(tWiseEvaluator.systemIterations.getValue());
        sb.append(" | ");
        sb.append(algorithmList.get(algorithmIndex).getFullName());
        sb.append(" (");
        sb.append(algorithmIndex + 1);
        sb.append("/");
        sb.append(algorithmList.size());
        sb.append(") ");
        sb.append(algorithmIteration);
        sb.append("/");
        sb.append(algorithmList.get(algorithmIndex).getIterations());
        Logger.logInfo(sb.toString());
    }

    private void logCNF() {
        Logger.logInfo("modelCNF: " + modelCNF);

        Logger.logInfo("modelCNF.getVariableMap().getVariableCount(): " + modelCNF.getVariableMap().getVariableCount());
        Logger.logInfo("modelCNF.getVariableMap().getVariableNames(): " + modelCNF.getVariableMap().getVariableNames());
        Logger.logInfo("modelCNF.getVariableMap().getConstantCount(): " + modelCNF.getVariableMap().getConstantCount());
        Logger.logInfo("modelCNF.getVariableMap().getConstantNames(): " + modelCNF.getVariableMap().getConstantNames());

        Logger.logInfo("modelCNF.getVariableMap().getVariable(1): " + modelCNF.getVariableMap().getVariable(1));
        Logger.logInfo("modelCNF.getVariableMap().getVariable(2): " + modelCNF.getVariableMap().getVariable(2));
    }
}
