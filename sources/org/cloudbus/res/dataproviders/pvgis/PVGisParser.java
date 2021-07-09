package org.cloudbus.res.dataproviders.pvgis;

import org.cloudbus.res.config.AppConfig;

import java.io.File;
import java.io.IOException;

public class PVGisParser {
    public PVGisResponse parse(String filePath) throws IOException {
        return AppConfig.MAPPER.readValue(new File(filePath), PVGisResponse.class);
    }
}
