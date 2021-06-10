package org.cloudbus.res.model.pvgis.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Hourly {
    public String time;
    @JsonProperty("G(i)")
    public double gI;

}
