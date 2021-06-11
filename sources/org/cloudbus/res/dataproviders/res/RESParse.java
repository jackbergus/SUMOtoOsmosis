package org.cloudbus.res.dataproviders.res;

import org.cloudbus.res.config.AppConfig;

import java.io.File;
import java.io.IOException;

public class RESParse {
    public RESResponse parse(String filePath) throws IOException {
        return AppConfig.MAPPER.readValue(new File(filePath), RESResponse.class);
    }

}
