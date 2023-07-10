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

import java.nio.file.*;

public class ACTS_IPOG_CSP extends AACTSSampling {

    public ACTS_IPOG_CSP(int t, Path outputFile, Path fmFile) {
        super(outputFile, fmFile, t);
    }

    @Override
    public String getName() {
        return "ACTS-IPOG";
    }

    @Override
    protected String getAlgorithmName() {
        return "ipog";
    }

    @Override
    protected String getConstraintHandler() {
        return "solver";
    }
}
