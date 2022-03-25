/*
 * TrafficLightInformation.java
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
import uk.ncl.giacomobergami.utils.CartesianPoint;

import java.util.Objects;

public class TrafficLightInformation implements CartesianPoint {
    public String id;
    public double x;
    public double y;

    public ConfiguationEntity.VMEntity asVMEntity(int pes,
                                                  double mips,
                                                  int ram,
                                                  double storage,
                                                  long bw,
                                                  String cloudletPolicy) {
        ConfiguationEntity.VMEntity result = new ConfiguationEntity.VMEntity();
        result.setBw(bw);
        result.setCloudletPolicy(cloudletPolicy);
        result.setPes(pes);
        result.setMips(mips);
        result.setRam(ram);
        result.setStorage(storage);
        result.setName(id);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrafficLightInformation that = (TrafficLightInformation) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0 && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y);
    }

    @Override
    public String toString() {
        return "TrafficLightInformation{" +
                "id='" + id + '\'' +
                ", x=" + x +
                ", y=" + y +
                '}';
    }


    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }
}
