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

import de.featjar.analysis.sat4j.solver.Sat4JSolver;
import de.featjar.analysis.sat4j.twise.CoverageStatistic;
import de.featjar.analysis.sat4j.twise.TWiseConfigurationUtil;
import de.featjar.analysis.sat4j.twise.TWiseStatisticGenerator;
import de.featjar.analysis.sat4j.twise.TWiseStatisticGenerator.ConfigurationScore;
import de.featjar.analysis.sat4j.twise.ValidityStatistic;
import de.featjar.clauses.CNF;
import de.featjar.clauses.Clauses;
import de.featjar.clauses.LiteralList;
import de.featjar.clauses.solutions.SolutionList;
import de.featjar.clauses.solutions.io.PartialListFormat;
import de.featjar.evaluation.EvaluationPhase;
import de.featjar.evaluation.Evaluator;
import de.featjar.formula.io.dimacs.DIMACSFormatCNF;
import de.featjar.util.io.IO;
import de.featjar.util.io.csv.CSVWriter;
import de.featjar.util.logging.Logger;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Sebastian Krieter
 */
public class CoverageTestingPhase implements EvaluationPhase {

    private final DIMACSFormatCNF modelFormat = new DIMACSFormatCNF();
    private final PartialListFormat sampleFormat = new PartialListFormat();

    private CSVWriter dataWriter;
    private int algorithmIndex, algorithmIteration;
    private CNF modelCNF;

    private ValidityStatistic validity;
    private CoverageStatistic coverageStatistic;

    private TWiseSampleEvaluator tWiseEvaluator;

    @Override
    public void run(Evaluator evaluator) {
        tWiseEvaluator = (TWiseSampleEvaluator) evaluator;

        dataWriter = evaluator.addCSVWriter(
                "data2.csv",
                "ModelID",
                "AlgorithmID",
                "SystemIteration",
                "AlgorithmIteration",
                "ValidityRatio",
                "Coverage",
                "NumberOfUncoveredConditions",
                "NumberOfInvalidConditions");

        dataWriter.setLineWriter(this::writeData);

        if (evaluator.systemIterations.getValue() > 0) {
            evaluator.tabFormatter.setTabLevel(0);
            Logger.logInfo("Start");

            for (evaluator.systemIndex = 0; evaluator.systemIndex < evaluator.systemIndexMax; evaluator.systemIndex++) {
                evaluator.tabFormatter.setTabLevel(1);
                evaluator.logSystem();

                for (evaluator.systemIteration = 1;
                        evaluator.systemIteration <= evaluator.systemIterations.getValue();
                        evaluator.systemIteration++) {

                    readModel();
                    if (modelCNF != null) {
                        TWiseConfigurationUtil util = new TWiseConfigurationUtil(modelCNF, new Sat4JSolver(modelCNF));
                        TWiseStatisticGenerator gen = new TWiseStatisticGenerator(util);

                        algorithmIndex = -1;
                        for (final String algorithmName : tWiseEvaluator.algorithmsProperty.getValue()) {
                            for (final Integer tValue : tWiseEvaluator.tProperty.getValue()) {
                                algorithmIndex++;
                                for (algorithmIteration = 1;
                                        algorithmIteration <= tWiseEvaluator.algorithmIterations.getValue();
                                        algorithmIteration++) {

                                    evaluator.tabFormatter.setTabLevel(2);
                                    logRun();
                                    evaluator.tabFormatter.setTabLevel(3);

                                    String sampleFileName = tWiseEvaluator.getSystemID() + "_"
                                            + tWiseEvaluator.systemIteration + "_" + algorithmIndex + "_"
                                            + algorithmIteration + "_sample." + sampleFormat.getFileExtension();

                                    SolutionList sample = IO.load(
                                                    tWiseEvaluator.outputPath.resolve(sampleFileName), sampleFormat)
                                            .orElse(Logger::logProblems);
                                    if (sample != null) {
                                        List<List<LiteralList>> samples = List.of(sample.getSolutions());
                                        validity = gen.getValidity(samples).get(0);
                                        List<List<LiteralList>> literals =
                                                de.featjar.analysis.sat4j.twise.YASA.convertLiterals(
                                                        Clauses.getLiterals(sample.getVariableMap()));
                                        coverageStatistic = gen.getCoverage(
                                                        samples,
                                                        List.of(literals),
                                                        tValue,
                                                        ConfigurationScore.NONE,
                                                        true)
                                                .get(0);
                                    } else {
                                        validity = null;
                                        coverageStatistic = null;
                                    }

                                    dataWriter.writeLine();
                                }
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

    protected void readModel() {
        Path modelPath = tWiseEvaluator.outputPath.resolve(tWiseEvaluator.getSystemID() + "_"
                + tWiseEvaluator.systemIteration + "_rnd_model." + modelFormat.getFileExtension());
        modelCNF = IO.load(modelPath, modelFormat).orElse(Logger::logProblems);
    }

    protected void writeData(CSVWriter dataCSVWriter) {
        dataCSVWriter.addValue(tWiseEvaluator.getSystemID());
        dataCSVWriter.addValue(algorithmIndex);
        dataCSVWriter.addValue(tWiseEvaluator.systemIteration);
        dataCSVWriter.addValue(algorithmIteration);
        dataCSVWriter.addValue(validity != null ? validity.getValidInvalidRatio() : -1);
        dataCSVWriter.addValue(coverageStatistic != null ? coverageStatistic.getCoverage() : -1);
        dataCSVWriter.addValue(coverageStatistic != null ? coverageStatistic.getNumberOfUncoveredConditions() : -1);
        dataCSVWriter.addValue(coverageStatistic != null ? coverageStatistic.getNumberOfInvalidConditions() : -1);
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
        sb.append(algorithmIndex + 1);
        sb.append(" | ");
        sb.append(algorithmIteration);
        Logger.logInfo(sb.toString());
    }
}
