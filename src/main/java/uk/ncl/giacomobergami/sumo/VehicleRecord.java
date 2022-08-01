/*
 * VehicleRecord.java
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

package uk.ncl.giacomobergami.sumo;

import org.cloudbus.cloudsim.edge.core.edge.ConfiguationEntity;
import org.cloudbus.cloudsim.edge.core.edge.Mobility;
import uk.ncl.giacomobergami.solver.Vehicle;
import uk.ncl.giacomobergami.utils.CartesianPoint;

import java.util.Objects;

public class VehicleRecord extends Vehicle {
    public double angle;
    public String type;
    public double speed;
    public double pos;
    public String lane;
    public double slope;

    public VehicleRecord() {
        super("", 0, 0);
    }

    public String getId() {
        return super.id;
    }

    public void setId(String id) {
        super.id = id;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getPos() {
        return pos;
    }

    public void setPos(double pos) {
        this.pos = pos;
    }

    public String getLane() {
        return lane;
    }

    public void setLane(String lane) {
        this.lane = lane;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    @Override
    public String toString() {
        return "VehicleRecord{" +
                "id='" + id + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", angle=" + angle +
                ", type='" + type + '\'' +
                ", speed=" + speed +
                ", pos=" + pos +
                ", lane='" + lane + '\'' +
                ", slope=" + slope +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleRecord that = (VehicleRecord) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0 && Double.compare(that.angle, angle) == 0 && Double.compare(that.speed, speed) == 0 && Double.compare(that.pos, pos) == 0 && Double.compare(that.slope, slope) == 0 && Objects.equals(id, that.id) && Objects.equals(type, that.type) && Objects.equals(lane, that.lane);
    }



    @Override
    public int hashCode() {
        return Objects.hash(id, x, y, angle, type, speed, pos, lane, slope);
    }

}
