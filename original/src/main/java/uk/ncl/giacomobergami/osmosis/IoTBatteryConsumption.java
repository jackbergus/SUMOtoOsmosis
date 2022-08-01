package uk.ncl.giacomobergami.osmosis;

import org.apache.commons.math3.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class IoTBatteryConsumption extends Pair<String, Double> {

    public IoTBatteryConsumption(String s, Double aDouble) {
        super(s, aDouble);
    }

    public static Map<String, Double> consumptionMap(Collection<IoTBatteryConsumption> consumptions) {
        Map<String, Double> m = new HashMap<>();
        consumptions.forEach(x -> m.put(x.getKey(), x.getValue()));
        return m;
    }


}
