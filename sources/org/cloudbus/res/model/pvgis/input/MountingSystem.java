package org.cloudbus.res.model.pvgis.input;

import lombok.Data;

@Data
public class MountingSystem {
    private Fixed fixed;
    private String description;
    private String choices;
    private Fields fields;
}
