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
package de.featjar.evaluation.twise.algorithms;

import de.featjar.clauses.LiteralList;
import de.featjar.clauses.LiteralList.Order;
import de.featjar.clauses.solutions.SolutionList;
import de.featjar.evaluation.process.Algorithm;
import de.featjar.formula.io.FormulaFormatManager;
import de.featjar.formula.io.textual.ACTSFormat;
import de.featjar.formula.structure.Formula;
import de.featjar.formula.structure.atomic.literal.VariableMap;
import de.featjar.util.io.IO;
import de.featjar.util.logging.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public abstract class AACTSSampling extends Algorithm<SolutionList> {

    private final Path outputFile;
    private final Path fmFile;
    private Path tempFile;
    private VariableMap variableMap;

    private final int t;

    public AACTSSampling(Path outputFile, Path fmFile, int t) {
        this.outputFile = outputFile;
        this.fmFile = fmFile;
        this.t = t;
    }

    protected abstract String getAlgorithmName();

    protected abstract String getConstraintHandler();

    @Override
    protected void addCommandElements() {
        addCommandElement("java");
        addCommandElement("-da");
        addCommandElement("-Xmx14g");
        addCommandElement("-Dalgo=" + getAlgorithmName());
        addCommandElement("-Ddoi=" + t);
        addCommandElement("-Doutput=csv");
        addCommandElement("-Dchandler=" + getConstraintHandler()); // other option: solver
        //        addCommandElement("-Dchandler=solver"); // other option: forbiddentuples
        addCommandElement("-Drandstar=off");
        addCommandElement("-jar");
        addCommandElement("tools/ACTS3.2/acts_3.2.jar");
        addCommandElement(tempFile.toString());
        addCommandElement(outputFile.toString());
    }

    @Override
    public void preProcess() throws Exception {
        Formula fm = IO.load(fmFile, FormulaFormatManager.getInstance()).orElse(Logger::logProblems);
        variableMap = fm.getVariableMap().orElse(null);
        if (variableMap == null) {
            throw new RuntimeException();
        }
        ACTSFormat actsFormat = new ACTSFormat();
        tempFile = Files.createTempFile("acts", "." + actsFormat.getFileExtension());
        IO.save(fm, tempFile, actsFormat);
        super.preProcess();
    }

    @Override
    public void postProcess() {
        try {
            Files.deleteIfExists(tempFile);
            Files.deleteIfExists(outputFile);
        } catch (final IOException e) {
            Logger.logError(e);
        }
    }

    @Override
    public SolutionList parseResults() throws IOException {
        //    	Files.lines(outputFile).limit(7).forEach(Logger::logInfo);
        //    	Files.lines(outputFile).forEach(Logger::logInfo);
        return new SolutionList(
                variableMap,
                Files.lines(outputFile) //
                        .skip(7)
                        .map(AACTSSampling::convertOutput) //
                        .collect(Collectors.toList()));
    }

    private static LiteralList convertOutput(String line) {
        final String[] values = line.split(",");
        final int[] literals = new int[values.length];
        for (int i = 0; i < literals.length; i++) {
            switch (values[i]) {
                case "true":
                    literals[i] = i + 1;
                    break;
                case "false":
                    literals[i] = -(i + 1);
                    break;
                case "*":
                    literals[i] = 0;
                    break;
                default:
                    throw new RuntimeException();
            }
        }
        return new LiteralList(literals, Order.INDEX, false);
    }

    @Override
    public String getParameterSettings() {
        return "t" + t;
    }
}
