/*
 * CSVOsmosisRecord.java
 * This file is part of RunSimulator
 *
 * Copyright (C) 2022 - Giacomo Bergami
 *
 * RunSimulator is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * RunSimulator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RunSimulator. If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ncl.giacomobergami.osmosis;

import com.opencsv.CSVWriter;
import uk.ncl.giacomobergami.sumo.TrafficLightInformation;
import uk.ncl.giacomobergami.sumo.VehicleRecord;
import uk.ncl.giacomobergami.utils.SimulatorConf;

import java.io.*;
import java.util.Collection;

public class CSVOsmosisRecord {

    /**
     * App name
     */
    public String OsmesisApp;

    /**
     * App ID
     */
    public long ID;
    public long NumOfLayer;

    /**
     * Data generation rate
     */
    public double DataRate_Sec;

    /**
     * Time to stop generating data
     */
    public double StopDataGeneration_Sec;

    /**
     * The name of IoT device (e.g. temperature_1). Note that IoT devices’
     * name must be defined in the infrastructure file (Example1_configuration)
     */
    public String IoTDevice;
    public String IoTLayerName;
    public long OsmesisIoTLet_MI;

    /**
     * The data size in Mb an IoT device generates according to the data
     * generation rate (e.g. sending 100 Mb every 2 seconds)
     */
    public long IoTDeviceOutputData_Mb;

    /**
     * MEL destination that receives data from an IoT device
     */
    public String MELName;
    public String datacenter;

    /**
     * Size of a task in million instructions (MI), which is executed in a MEL
     */
    public long OsmesisEdgelet_MI;

    /**
     * Every MEL generates new data in Mb (e.g. filtering, sorting, calculating)
     * and sends the data to a VM in cloud datacentre via SD-WAN network
     * layer
     */
    public long MELOutputData_Mb;

    /**
     * VM’s name that receives data from a MEL
     */
    public String VmName;
    public String datacenter_2;

    /**
     * Finally, VM processes received data in a form of MI
     */
    public String OsmesisCloudlet_MI;

    public static String[] header = new String[]{ "OsmesisApp","ID","\"NumOfLayer (IoT, edges, clouds)\"","DataRate_Sec","StopDataGeneration_Sec",
    "IoTDevice","IoTLayerName","OsmesisIoTLet_MI","IoTDeviceOutputData_Mb","MELName","datacenter","OsmesisEdgelet_MI","MELOutputData_Mb","VmName",
    "datacenter","OsmesisCloudlet_MI"};

    public CSVOsmosisRecord(int semaphoreIdx,
                            TrafficLightInformation semaphore,
                            VehicleRecord veh,
                            SimulatorConf conf,
                            String aMelName) {
        OsmesisApp = semaphore.id;
        ID = semaphoreIdx;
        NumOfLayer = 3;
        DataRate_Sec = conf.DataRate_Sec;
        StopDataGeneration_Sec = conf.StopDataGeneration_Sec;
        IoTDevice = veh.id;
        IoTLayerName = "IoT_1";
        OsmesisIoTLet_MI = conf.OsmesisIoTLet_MI;
        IoTDeviceOutputData_Mb = conf.IoTDeviceOutputData_Mb;
        MELName = aMelName;
        datacenter = conf.datacenter;
        OsmesisEdgelet_MI = conf.OsmesisEdgelet_MI;
        MELOutputData_Mb = conf.MELOutputData_Mb;
        VmName = semaphore.id;
        datacenter_2 = conf.datacenter_2;
        OsmesisCloudlet_MI = String.valueOf(conf.OsmesisCloudlet_MI);
    }

    public String[] toCSVArray() {
        return new String[]{
                OsmesisApp,
                Long.toString(ID),
                Long.toString(NumOfLayer),
                Double.toString(DataRate_Sec),
                Double.toString(StopDataGeneration_Sec),
                IoTDevice,
                IoTLayerName,
                Long.toString(OsmesisIoTLet_MI),
                Long.toString(IoTDeviceOutputData_Mb),
                MELName,
                datacenter,
                Long.toString(OsmesisEdgelet_MI),
                Long.toString(MELOutputData_Mb),
                VmName,
                datacenter_2,
                OsmesisCloudlet_MI
        };
    }

    public static void WriteCsv(File filename, Collection<CSVOsmosisRecord> collection) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        bw.write(String.join(",", header));
        bw.newLine();
        for (var x : collection) {
            bw.write(String.join(",", x.toCSVArray()));
            bw.newLine();
        }
        bw.close();
        fos.close();
    }

}
