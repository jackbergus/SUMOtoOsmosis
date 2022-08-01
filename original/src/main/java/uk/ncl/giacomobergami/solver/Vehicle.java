package uk.ncl.giacomobergami.solver;

import org.cloudbus.cloudsim.edge.core.edge.ConfiguationEntity;
import org.cloudbus.cloudsim.edge.core.edge.Mobility;
import uk.ncl.giacomobergami.utils.CartesianPoint;

import java.util.Objects;

public class Vehicle implements CartesianPoint {
    public String id;
    public double x;
    public double y;

    public Vehicle(String id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public Vehicle copy() {
        return new Vehicle(id, x, y);
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "id='" + id + '\'' +
                ", x=" + x +
                ", y=" + y +
                '}';
    }

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vehicle vehicle = (Vehicle) o;
        return Double.compare(vehicle.x, x) == 0 && Double.compare(vehicle.y, y) == 0 && Objects.equals(id, vehicle.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y);
    }
}
