package org.cloudbus.res.dataproviders.pvgis;

import org.cloudbus.res.config.AppConfig;

import java.io.File;
import java.io.IOException;

public class PVGisParse {
    public PVGisResponse parse(String jsonString) throws IOException {
        return AppConfig.MAPPER.readValue(new File(jsonString), PVGisResponse.class);
    }
}
