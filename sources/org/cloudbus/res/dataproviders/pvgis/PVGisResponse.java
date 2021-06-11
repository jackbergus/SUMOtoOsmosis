package org.cloudbus.res.dataproviders.pvgis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.cloudbus.res.dataproviders.EnergyData;
import org.cloudbus.res.model.pvgis.input.Inputs;
import org.cloudbus.res.model.pvgis.output.Outputs;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PVGisResponse implements EnergyData {
    private Inputs inputs;
    private Outputs outputs;

    @Override
    public int getCurrentPower(long timestamp) {
        return 0;
    }
}
