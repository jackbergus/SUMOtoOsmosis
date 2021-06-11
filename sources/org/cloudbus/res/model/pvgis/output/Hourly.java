package org.cloudbus.res.model.pvgis.output;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.cloudbus.res.config.AppConfig;

import java.beans.ConstructorProperties;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Hourly {
    private LocalDateTime dateTime;
    private double systemPower;

    @ConstructorProperties({"time", "P"})
    public Hourly(String dateTime, double systemPower) {
        this.dateTime = LocalDateTime.parse(dateTime, AppConfig.FORMATTER);
        this.systemPower = systemPower;
    }
}
