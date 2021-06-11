package org.cloudbus.res.dataproviders;

/**
 * Class that would hold energy data (power units in specific time) for energy source
 */
public interface EnergyData {
    //TODO: check which time of parameter will fit
    int getCurrentPower(long timestamp);
}
