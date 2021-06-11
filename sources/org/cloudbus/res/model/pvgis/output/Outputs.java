package org.cloudbus.res.model.pvgis.output;

import lombok.Data;

import java.util.List;
@Data
public class Outputs {
    private List<Hourly> hourly;
}
