package org.cloudbus.res.dataproviders.pvgis;

import org.cloudbus.res.dataproviders.res.RESParse;

import java.io.IOException;

public class test {
    public static void main(String[] args) throws IOException {
        PVGisParse pv=new PVGisParse();
        PVGisResponse pvg =pv.parse("C:\\Users\\Amadeusz\\Desktop\\IoTSim-Osmosis-RES\\inputFiles\\pvgis\\Edge_1-PV_panels_1-2016.json");
        System.out.println(pvg);
    }
}
