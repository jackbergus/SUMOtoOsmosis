package org.cloudbus.res.policies;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "className",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ExportLimitEnergyPolicy.class, name = "ExportLimitEnergyManagementPolicy"),
        @JsonSubTypes.Type(value = ExportLimitWithEnergyStoragePolicy.class, name = "ExportLimitWithEnergyStoragePolicy"),
        @JsonSubTypes.Type(value = ExportLimitWithoutEnergyStoragePolicy.class, name = "ExportLimitWithoutEnergyStoragePolicy")
})
public abstract class EnergyManagementPolicy {
    private String className;
}
