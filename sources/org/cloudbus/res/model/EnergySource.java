package org.cloudbus.res.model;

import lombok.Data;

@Data
public class EnergySource {
    private String name;
    private String type;
    private String technology;
    private int peakPower;
    private int loss;
    private Location location;
    private String angle;
}
