package org.cloudbus.res.model.pvgis.input;

import lombok.Data;
import org.cloudbus.res.model.Location;

@Data
public class Inputs {
    public Location location;
    public MeteoData meteo_data;
    public MountingSystem mounting_system;
    public PvModule pv_module;
}
