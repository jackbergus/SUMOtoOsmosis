package org.cloudbus.res.dataproviders;

import java.time.LocalDateTime;

/**
 * Class that would hold energy data (power units in specific time) for energy source
 */
public interface EnergyData {
    //TODO: check which time of parameter will fit
    double getCurrentPower(long timestamp);

    double getCurrentPower(LocalDateTime time);

    double getAnnualEnergy();
}
