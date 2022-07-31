package uk.ncl.giacomobergami.solver;

import uk.ncl.giacomobergami.sumo.TrafficLightInformation;
import java.util.Objects;

public class RSU extends TrafficLightInformation {
    public double communication_radius;
    public double max_vehicle_communication;

    public RSU(String id, double x, double y, double communication_radius,
               double max_vehicle_communication) {
        super.id = id;
        super.x = x;
        super.y = y;
        this.communication_radius = communication_radius;
        this.max_vehicle_communication = max_vehicle_communication;
    }

    @Override
    public String toString() {
        return "RSU{" +
                "id='" + id + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", communication_radius=" + communication_radius +
                ", max_vehicle_communication=" + max_vehicle_communication +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RSU rsu = (RSU) o;
        return Double.compare(rsu.communication_radius, communication_radius) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), communication_radius);
    }
}
