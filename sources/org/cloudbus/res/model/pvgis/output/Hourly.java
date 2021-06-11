package org.cloudbus.res.model.pvgis.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hourly {
    private String time;
    @JsonProperty("G(i)")
    private double gI;

}
