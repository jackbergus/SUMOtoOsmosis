/*
 * CartesianDistanceFunction.java
 * This file is part of RunSimulator
 *
 * Copyright (C) 2022 - Giacomo Bergami
 *
 * RunSimulator is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * RunSimulator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RunSimulator. If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ncl.giacomobergami.utils;

import com.eatthepath.jvptree.DistanceFunction;

public class CartesianDistanceFunction implements DistanceFunction<CartesianPoint> {

    public double getDistance(final CartesianPoint firstPoint, final CartesianPoint secondPoint) {
        final double deltaX = firstPoint.getX() - secondPoint.getX();
        final double deltaY = firstPoint.getY() - secondPoint.getY();

        return ((deltaX * deltaX) + (deltaY * deltaY));
    }
}