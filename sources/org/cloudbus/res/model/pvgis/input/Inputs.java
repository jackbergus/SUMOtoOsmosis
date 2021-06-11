package org.cloudbus.res.model.pvgis.input;

import lombok.Data;
import org.cloudbus.res.model.Location;

@Data
public class Inputs {
    private Location location;
    private MeteoData meteo_data;
    private MountingSystem mounting_system;
    private PvModule pv_module;
}
