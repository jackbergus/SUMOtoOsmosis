package org.cloudbus.res.model.agent;

import org.cloudbus.cloudsim.edge.iot.IoTDevice;

public class DeviceAgent extends AbstractAgent{
    private IoTDevice ioTDevice;

    @Override
    public void notifyUpdateState() {
        //probably do nothing
    }
}
