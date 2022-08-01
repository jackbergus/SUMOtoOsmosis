package org.cloudbus.res;
import lombok.NoArgsConstructor;
import org.cloudbus.res.EnergyManagementPolicy;

import java.time.LocalDateTime;

@NoArgsConstructor
public class GridOnlyPolicy extends EnergyManagementPolicy {
    @Override
    public void update() {
    }

    @Override
    public double getRESCurrentPower() {
        return 0;
    }

    @Override
    public double getRESCurrentPower(long timestamp) {
        return 0;
    }

    @Override
    public double getRESCurrentPower(LocalDateTime time) {
        return 0;
    }

    @Override
    public double getRESMaximumPower() {
        return 0;
    }
}

