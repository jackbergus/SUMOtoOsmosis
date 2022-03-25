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
import uk.ncl.giacomobergami.utils.CartesianPoint;

import java.util.Objects;

public class VehicleRecord implements CartesianPoint {
    public String id;
    public double x;
    public double y;
    public double angle;
    public String type;
    public double speed;
    public double pos;
    public String lane;
    public double slope;

    public ConfiguationEntity.IotDeviceEntity asIoDevice(double bw,
                                                         double max_battery_capacity,
                                                         double battery_sensing_rate,
                                                         double battery_sending_rate,
                                                         String network_type,
                                                         String protocol) {
        ConfiguationEntity.IotDeviceEntity result = new ConfiguationEntity.IotDeviceEntity();
        result.setName(id);
        result.setBw(bw);
        result.setMax_battery_capacity(max_battery_capacity);
        result.setBattery_sensing_rate(battery_sensing_rate);
        result.setBattery_sending_rate(battery_sending_rate);
        result.setIoTClassName("org.cloudbus.cloudsim.edge.iot.CarSensor");
        ConfiguationEntity.MobilityEntity me = new ConfiguationEntity.MobilityEntity();
        me.setMovable(false);
        me.setLocation(new Mobility.Location(this.x, this.y, 0));
        result.setMobilityEntity(me);
        var nme = new ConfiguationEntity.NetworkModelEntity();
        nme.setNetworkType(network_type);
        nme.setCommunicationProtocol(protocol);
        result.setNetworkModelEntity(nme);
        return result;
        /*
        				"bw":100,
				"max_battery_capacity":100.0,
				"battery_sensing_rate":0.001,
				"battery_sending_rate":0.001,
				"ioTClassName":"org.cloudbus.cloudsim.edge.iot.TemperatureSensor",
				 "mobilityEntity":{
					"movable":false,
					"location":{
					   "x":0.0,
					   "y":0.0,
					   "z":0.0
					}
				 },
				 "networkModelEntity":{
					"communicationProtocol":"xmpp",
					"networkType":"wifi"
				 }
         */
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

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }
}
