package org.cloudbus.res.model.pvpanel;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cloudbus.res.model.RenewableEnergySource;

@Data
@EqualsAndHashCode(callSuper = true)
public class PVInstallation extends RenewableEnergySource {
    private PVTechnology pvTechnology;
    private double tiltAngle;
    private int peakPower;
    private double systemLoss;
}
