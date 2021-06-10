package org.cloudbus.res.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.cloudbus.res.dataproviders.EnergyData;
import org.cloudbus.res.model.pvpanel.PVInstallation;

/**
 * Abstract class that provides template for renewable energy sources which dataCenter can contain.
 * The getCurrentPower method is responsible for getting information about how much power/energy can energy source
 * give us at certain point of time.
 */
@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PVInstallation.class, name = "PV_PANELS")
})
public abstract class RenewableEnergySource {
    private String name;
    private Location location;
    @JsonProperty("type")
    private EnergySourceType energySourceType;
    @JsonIgnore
    private EnergyData energyData;
    // change input depends on energy data implementations
    int getCurrentPower(long timestamp) {
        return energyData.getCurrentPower(timestamp);
    }
}
