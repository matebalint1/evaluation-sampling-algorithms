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

import de.featjar.evaluation.Evaluator;
import de.featjar.evaluation.properties.ListProperty;
import de.featjar.evaluation.properties.Property;

public class TWiseSampleEvaluator extends Evaluator {

    ListProperty<Integer> tProperty = new ListProperty<>("t", Property.IntegerConverter);
    ListProperty<String> algorithmsProperty = new ListProperty<>("algorithm", Property.StringConverter);

    int maxT;

    @Override
    protected void initConstants() {
        super.initConstants();
        maxT = tProperty.getValue().stream().mapToInt(Integer::intValue).max().getAsInt();
    }

    @Override
    public String getName() {
        return "twise-sampler";
    }
}
