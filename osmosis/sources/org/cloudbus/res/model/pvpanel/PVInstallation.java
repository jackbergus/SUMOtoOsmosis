package org.cloudbus.res.model.pvpanel;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cloudbus.res.model.RenewableEnergySource;

import java.beans.ConstructorProperties;

@Data
@EqualsAndHashCode(callSuper = true)
public class PVInstallation extends RenewableEnergySource {
    private PVTechnology pvTechnology;
    private double tiltAngle;
    private int peakPower;
    private double systemLoss;

    @ConstructorProperties({"technology", "angle", "peakPower", "loss"})
    public PVInstallation(PVTechnology pvTechnology, double tiltAngle, int peakPower, double systemLoss) {
        this.pvTechnology = pvTechnology == null ? PVTechnology.crystSi : pvTechnology;
        this.tiltAngle = tiltAngle;
        this.peakPower = peakPower;
        this.systemLoss = systemLoss;
    }
}
