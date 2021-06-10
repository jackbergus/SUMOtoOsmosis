package org.cloudbus.res.dataproviders.pvgis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.io.IOException;

public class PVGisParse {
    ObjectMapper om=new ObjectMapper();

    public PVGisResponse parse(String jsonString) throws IOException {
        return om.readValue(new File(jsonString),PVGisResponse.class);
    }

}
