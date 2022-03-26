/*
 * SimulatorConf.java
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

package uk.ncl.giacomobergami.utils;

public class SimulatorConf {
    public String sumo_configuration_file_path;
    public String sumo_program;
    public String logger_file;
    public String trace_file;
    public long begin;
    public long end;
    public double maximum_tl_distance_in_meters;
    public double bw;
    public double max_battery_capacity;
    public double battery_sensing_rate;
    public double battery_sending_rate;
    public String network_type;
    public String protocol;
    public double DataRate_Sec;
    public double StopDataGeneration_Sec;
    public long OsmesisIoTLet_MI;
    public long IoTDeviceOutputData_Mb;
    public String datacenter;
    public long OsmesisEdgelet_MI;
    public long MELOutputData_Mb;
    public String datacenter_2;
    public long OsmesisCloudlet_MI;
    public String OsmosisOutput;
    public String experimentName;
    public String OsmosisConfFiles;
    public String default_json_conf_file;
    public int VM_pes;
    public double VM_mips;
    public int VM_ram;
    public double VM_storage;
    public long VM_bw;
    public  String VM_cloudletPolicy;
    public double terminate_simulation_at;

    public double getTerminate_simulation_at() {
        return terminate_simulation_at;
    }

    public void setTerminate_simulation_at(double terminate_simulation_at) {
        this.terminate_simulation_at = terminate_simulation_at;
    }

    public double getVM_mips() {
        return VM_mips;
    }

    public void setVM_mips(double VM_mips) {
        this.VM_mips = VM_mips;
    }

    public int getVM_ram() {
        return VM_ram;
    }

    public void setVM_ram(int VM_ram) {
        this.VM_ram = VM_ram;
    }

    public double getVM_storage() {
        return VM_storage;
    }

    public void setVM_storage(double VM_storage) {
        this.VM_storage = VM_storage;
    }

    public long getVM_bw() {
        return VM_bw;
    }

    public void setVM_bw(long VM_bw) {
        this.VM_bw = VM_bw;
    }

    public String getVM_cloudletPolicy() {
        return VM_cloudletPolicy;
    }

    public void setVM_cloudletPolicy(String VM_cloudletPolicy) {
        this.VM_cloudletPolicy = VM_cloudletPolicy;
    }

    public int getVM_pes() {
        return VM_pes;
    }

    public void setVM_pes(int VM_pes) {
        this.VM_pes = VM_pes;
    }

    public String getDefault_json_conf_file() {
        return default_json_conf_file;
    }

    public void setDefault_json_conf_file(String default_json_conf_file) {
        this.default_json_conf_file = default_json_conf_file;
    }

    public String getOsmosisConfFiles() {
        return OsmosisConfFiles;
    }

    public void setOsmosisConfFiles(String osmosisConfFiles) {
        OsmosisConfFiles = osmosisConfFiles;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public String getOsmosisOutput() {
        return OsmosisOutput;
    }

    public void setOsmosisOutput(String osmosisOutput) {
        OsmosisOutput = osmosisOutput;
    }

    public double getDataRate_Sec() {
        return DataRate_Sec;
    }

    public void setDataRate_Sec(double dataRate_Sec) {
        DataRate_Sec = dataRate_Sec;
    }

    public double getStopDataGeneration_Sec() {
        return StopDataGeneration_Sec;
    }

    public void setStopDataGeneration_Sec(double stopDataGeneration_Sec) {
        StopDataGeneration_Sec = stopDataGeneration_Sec;
    }

    public long getOsmesisIoTLet_MI() {
        return OsmesisIoTLet_MI;
    }

    public void setOsmesisIoTLet_MI(long osmesisIoTLet_MI) {
        OsmesisIoTLet_MI = osmesisIoTLet_MI;
    }

    public long getIoTDeviceOutputData_Mb() {
        return IoTDeviceOutputData_Mb;
    }

    public void setIoTDeviceOutputData_Mb(long ioTDeviceOutputData_Mb) {
        IoTDeviceOutputData_Mb = ioTDeviceOutputData_Mb;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public long getOsmesisEdgelet_MI() {
        return OsmesisEdgelet_MI;
    }

    public void setOsmesisEdgelet_MI(long osmesisEdgelet_MI) {
        OsmesisEdgelet_MI = osmesisEdgelet_MI;
    }

    public long getMELOutputData_Mb() {
        return MELOutputData_Mb;
    }

    public void setMELOutputData_Mb(long MELOutputData_Mb) {
        this.MELOutputData_Mb = MELOutputData_Mb;
    }

    public String getDatacenter_2() {
        return datacenter_2;
    }

    public void setDatacenter_2(String datacenter_2) {
        this.datacenter_2 = datacenter_2;
    }

    public long getOsmesisCloudlet_MI() {
        return OsmesisCloudlet_MI;
    }

    public void setOsmesisCloudlet_MI(long osmesisCloudlet_MI) {
        OsmesisCloudlet_MI = osmesisCloudlet_MI;
    }

    public double getBw() {
        return bw;
    }

    public void setBw(double bw) {
        this.bw = bw;
    }

    public double getMax_battery_capacity() {
        return max_battery_capacity;
    }

    public void setMax_battery_capacity(double max_battery_capacity) {
        this.max_battery_capacity = max_battery_capacity;
    }

    public double getBattery_sensing_rate() {
        return battery_sensing_rate;
    }

    public void setBattery_sensing_rate(double battery_sensing_rate) {
        this.battery_sensing_rate = battery_sensing_rate;
    }

    public double getBattery_sending_rate() {
        return battery_sending_rate;
    }

    public void setBattery_sending_rate(double battery_sending_rate) {
        this.battery_sending_rate = battery_sending_rate;
    }

    public String getNetwork_type() {
        return network_type;
    }

    public void setNetwork_type(String network_type) {
        this.network_type = network_type;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public double getMaximum_tl_distance_in_meters() {
        return maximum_tl_distance_in_meters;
    }

    public void setMaximum_tl_distance_in_meters(double maximum_tl_distance_in_meters) {
        this.maximum_tl_distance_in_meters = maximum_tl_distance_in_meters;
    }

    public String getTrace_file() {
        return trace_file;
    }

    public void setTrace_file(String trace_file) {
        this.trace_file = trace_file;
    }

    public long getBegin() {
        return begin;
    }

    public void setBegin(long begin) {
        this.begin = begin;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public String getSumo_configuration_file_path() {
        return sumo_configuration_file_path;
    }

    public void setSumo_configuration_file_path(String sumo_configuration_file_path) {
        this.sumo_configuration_file_path = sumo_configuration_file_path;
    }

    public String getSumo_program() {
        return sumo_program;
    }

    public void setSumo_program(String sumo_program) {
        this.sumo_program = sumo_program;
    }

    public String getLogger_file() {
        return logger_file;
    }

    public void setLogger_file(String logger_file) {
        this.logger_file = logger_file;
    }

}
