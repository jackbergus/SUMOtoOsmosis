package org.cloudbus.res.model.pvgis.input;

import lombok.Data;

@Data
public class PvModule {
    private String technology;
    private double peak_power;
    private double system_loss;
    private String description;
}
