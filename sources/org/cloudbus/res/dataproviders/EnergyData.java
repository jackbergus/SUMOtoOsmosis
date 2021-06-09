package org.cloudbus.res.dataproviders;

/**
 * Class that would hold energy data (power units in specific time) for energy source
 */
public interface EnergyData {
    //TODO: implement this
    int getCurrentPower(long timestamp);
}
