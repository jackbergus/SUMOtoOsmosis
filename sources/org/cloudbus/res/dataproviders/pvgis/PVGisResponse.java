package org.cloudbus.res.dataproviders.pvgis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.cloudbus.res.model.pvgis.input.Inputs;
import org.cloudbus.res.model.pvgis.output.Outputs;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PVGisResponse {
    private Inputs inputs;
    private Outputs outputs;
}
