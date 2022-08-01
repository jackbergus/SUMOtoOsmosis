package org.cloudbus.res.model.pvgis.input;


import lombok.Data;

@Data
public class MeteoData {
    private String radiation_db;
    private String meteo_db;
    private int year_min;
    private int year_max;
    private boolean use_horizon;
    private Object horizon_db;
    private String horizon_data;
}
