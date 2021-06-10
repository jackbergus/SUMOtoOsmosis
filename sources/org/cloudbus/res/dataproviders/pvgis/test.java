package org.cloudbus.res.dataproviders.pvgis;

import java.io.IOException;

public class test {
    public static void main(String[] args) throws IOException {
        PVGisParse pv=new PVGisParse();
        pv.parse("C:\\Users\\Amadeusz\\Desktop\\IoTSim-Osmosis-RES\\inputFiles\\Example_RES_config.json");
    }
}
