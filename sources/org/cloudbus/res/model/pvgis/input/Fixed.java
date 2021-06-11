package org.cloudbus.res.model.pvgis.input;

import lombok.Data;

@Data
public class Fixed {
    private Slope slope;
    private Azimuth azimuth;
    private String type;
}
