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
import de.featjar.evaluation.process.Algorithm;
import java.io.IOException;
import java.util.Random;

public class Dummy extends Algorithm<SolutionList> {

    private long id = new Random().nextLong();

    @Override
    public String getName() {
        return "Dummy";
    }

    @Override
    public String getParameterSettings() {
        return Long.toString(id);
    }

    @Override
    public void postProcess() throws Exception {}

    @Override
    public SolutionList parseResults() throws IOException {
        return new SolutionList();
    }

    @Override
    protected void addCommandElements() throws Exception {}
}
