package org.cloudbus.res.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;

public class AppConfig {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd:HHmm");
    public static final ObjectMapper MAPPER = new ObjectMapper();
}
