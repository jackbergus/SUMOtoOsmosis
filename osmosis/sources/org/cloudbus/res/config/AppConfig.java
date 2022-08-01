package org.cloudbus.res.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudbus.res.dataproviders.pvgis.PVGisParser;
import org.cloudbus.res.dataproviders.res.RESParser;

import java.time.format.DateTimeFormatter;

public class AppConfig {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd:HHmm");
    public static final ObjectMapper MAPPER = new ObjectMapper();
    public static final RESParser RES_PARSER = new RESParser();
    public static final PVGisParser PVGIS_PARSER = new PVGisParser();
    public static final String PVGIS_CONFIG_FILES_DIR = "inputFiles/pvgis";
}
