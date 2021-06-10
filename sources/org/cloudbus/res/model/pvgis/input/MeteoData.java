package org.cloudbus.res.model.pvgis.input;


import lombok.Data;

@Data
public class MeteoData {
    public String radiation_db;
    public String meteo_db;
    public int year_min;
    public int year_max;
    public boolean use_horizon;
    public Object horizon_db;
    public String horizon_data;
}
