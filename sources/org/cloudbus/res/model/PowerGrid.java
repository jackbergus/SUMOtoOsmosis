package org.cloudbus.res.model;

import lombok.Data;

/**
 * Represents price of energy from the grid (energia z sieci) in particular country
 */

@Data
public class PowerGrid {
    private String country;
    private double priceForEnergy;
}
