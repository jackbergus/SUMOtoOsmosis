package org.cloudbus.res.model.datacenter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.cloudbus.res.config.AppConfig;
import org.cloudbus.res.model.PowerGrid;
import org.cloudbus.res.model.RenewableEnergySource;
import org.cloudbus.res.model.storage.EnergyStorage;
import org.cloudbus.res.policies.EnergyManagementPolicy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EdgeDatacenter.class, name = "edge"),
        @JsonSubTypes.Type(value = CloudDatacenter.class, name = "cloud")
})
public abstract class Datacenter {
    private String name;
    private String type;
    private EnergyManagementPolicy energyManagementPolicy;
    private List<EnergyStorage> energyStorage;
    private List<PowerGrid> powerGrid;
    private List<RenewableEnergySource> energySources;

    public Datacenter enrich() {
        List<String> filePaths = getSourceConfigFiles();
        if (!filePaths.isEmpty()) {
            energySources
                    .forEach(energySource -> findFileName(filePaths, energySource)
                            .ifPresent(energySource::setEnergyData));
        }
        return this;
    }

    private Optional<String> findFileName(List<String> filePaths, RenewableEnergySource energySource) {
        return filePaths.stream().filter(path -> path.contains(energySource.getName())).findAny();
    }

    private List<String> getSourceConfigFiles() {
        try (Stream<Path> paths = Files.list(Paths.get(System.getProperty("user.dir") + File.separator + AppConfig.PVGIS_CONFIG_FILES_DIR))) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(s -> s.contains(name))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
