package org.cloudbus.res.dataproviders.pvgis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.cloudbus.res.dataproviders.EnergyData;
import org.cloudbus.res.model.pvgis.input.Inputs;
import org.cloudbus.res.model.pvgis.output.Hourly;
import org.cloudbus.res.model.pvgis.output.Outputs;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PVGisResponse implements EnergyData {
    private Inputs inputs;
    private Outputs outputs;

    @Override
    public double getCurrentPower(long timestamp) {
        //it is assumed that output data from PVGIS is sorted
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.of("UTC"));
        return outputs.getHourly().get(dateTime.getDayOfYear()*24+dateTime.getHour()).getSystemPower();
    }

    public double getCurrentPower(LocalDateTime time) {
        //it is assumed that output data from PVGIS is sorted
        return outputs.getHourly().get((time.getDayOfYear()-1)*24+time.getHour()).getSystemPower();
    }


    @Override
    public double getAnnualEnergy() {
        float annual = 0;
        for(Hourly hour: outputs.getHourly()){
            annual += hour.getSystemPower();
        }
        return annual;
    }
}
