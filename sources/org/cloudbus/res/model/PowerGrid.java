package org.cloudbus.res.model;

import lombok.Data;

/**
 * Represents price of energy from the power grid in particular country
 */

@Data
public class PowerGrid {
    private String country;
    private double priceForEnergy;

    private double emissionCO2;
    private double lowEmission;
    private double renewable;
}
