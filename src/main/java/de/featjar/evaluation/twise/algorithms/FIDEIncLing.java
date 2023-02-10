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

public class FIDEIncLing extends AFIDESampling {

    public FIDEIncLing(Path outputFile, Path fmFile) {
        super(outputFile, fmFile);
    }

    @Override
    protected void addCommandElements() {
        super.addCommandElements();
        addCommandElement("-a");
        addCommandElement("Incling");
        addCommandElement("-t");
        addCommandElement(Integer.toString(2));
        if (seed != null) {
            addCommandElement("-s");
            addCommandElement(seed.toString());
        }
    }

    @Override
    public String getName() {
        return "Incling";
    }

    @Override
    public String getParameterSettings() {
        return "t2";
    }
}
