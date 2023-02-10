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

import de.featjar.clauses.solutions.SolutionList;
import de.featjar.clauses.solutions.io.ListFormat;
import de.featjar.evaluation.process.Algorithm;
import de.featjar.util.data.Result;
import de.featjar.util.io.IO;
import de.featjar.util.logging.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AFeatJARSampling extends Algorithm<SolutionList> {

    private final Path outputFile;
    private final Path fmFile;

    protected Long seed;
    protected int limit;

    public AFeatJARSampling(Path outputFile, Path fmFile) {
        this.outputFile = outputFile;
        this.fmFile = fmFile;
    }

    @Override
    protected void addCommandElements() {
        addCommandElement("java");
        addCommandElement("-da");
        addCommandElement("-Xmx14g");
        addCommandElement("-Xms2g");
        addCommandElement("-cp");
        addCommandElement("tools/FeatJAR/*");
        addCommandElement("de.featjar.util.cli.CLI");
        addCommandElement("genconfig");
        addCommandElement("-o");
        addCommandElement(outputFile.toString());
        addCommandElement("-i");
        addCommandElement(fmFile.toString());
    }

    @Override
    public void postProcess() {
        try {
            Files.deleteIfExists(outputFile);
        } catch (final IOException e) {
            Logger.logError(e);
        }
    }

    @Override
    public SolutionList parseResults() throws IOException {
        final Result<SolutionList> parse = IO.load(outputFile, new ListFormat());
        if (parse.isEmpty()) {
            Logger.logProblems(parse.getProblems());
            throw new IOException();
        }
        return parse.get();
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
