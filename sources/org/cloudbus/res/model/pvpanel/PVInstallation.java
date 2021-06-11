package org.cloudbus.res.model.pvpanel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cloudbus.res.model.RenewableEnergySource;

@Data
@EqualsAndHashCode(callSuper = true)
public class PVInstallation extends RenewableEnergySource {
    @JsonProperty("technology")
    private PVTechnology pvTechnology;
    @JsonProperty("angle")
    private double tiltAngle;
    private int peakPower;
    @JsonProperty("loss")
    private double systemLoss;
}
