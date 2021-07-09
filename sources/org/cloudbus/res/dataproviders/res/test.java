package org.cloudbus.res.dataproviders.res;

import java.io.IOException;

public class test {
    public static void main(String[] args) throws IOException {
        RESParser pv=new RESParser();
        System.out.println(pv.parse("inputFiles/Example_RES_config.json"));
        System.out.println(pv.parse("inputFiles/Example_RES_config.json").getDatacenters().get(0).getEnergySources().get(0).getEnergyData());
    }
}
