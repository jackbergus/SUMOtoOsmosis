package org.cloudbus.res.dataproviders.pvgis;

import java.io.IOException;

public class test {
    public static void main(String[] args) throws IOException {
        PVGisParser pv=new PVGisParser();
        PVGisResponse pvg =pv.parse("inputFiles/pvgis/Edge_2-PV_panels_1-2016.json");
        System.out.println(pvg);
    }
}
