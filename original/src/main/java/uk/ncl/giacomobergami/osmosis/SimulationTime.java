package uk.ncl.giacomobergami.osmosis;

public class SimulationTime {

    public static double maximum_link_propagation_delay(double meter_distance,
                                                        double meter_per_second_propagation_speed,
                                                        double packet_size_bytes,
                                                        double bandwidth) {
        double maximum_link_propagation_delay = meter_distance / meter_per_second_propagation_speed;
        double transmit = packet_size_bytes / bandwidth;
        return maximum_link_propagation_delay + transmit;
    }

}
