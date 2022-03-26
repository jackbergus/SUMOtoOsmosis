/*
 * CSVOsmosisAppFromTags.java
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

import com.google.common.collect.HashMultimap;
import org.cloudbus.cloudsim.edge.core.edge.EdgeLet;
import org.cloudbus.osmosis.core.Flow;
import org.cloudbus.osmosis.core.OsmesisBroker;
import org.cloudbus.osmosis.core.WorkflowInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CSVOsmosisAppFromTags {

    public static class Record {
        public int App_ID;
        public String AppName;
        public int Transaction;
        public double StartTime, FinishTime;
        public String IoTDeviceName, MELName;
        public long DataSizeIoTDeviceToMEL_Mb;
        public double TransmissionTimeIoTDeviceToMEL;
        public double EdgeLetMISize, EdgeLet_MEL_StartTime, EdgeLet_MEL_FinishTime, EdgeLetProccessingTimeByMEL;
        public String DestinationVmName;
        public long DataSizeMELToVM_Mb;
        public double TransmissionTimeMELToVM, CloudLetMISize, CloudLetProccessingTimeByVM, TransactionTotalTime;

        @Override
        public String toString() {
            return App_ID +
                    "," + AppName  +
                    "," + Transaction +
                    "," + StartTime +
                    "," + FinishTime +
                    "," + IoTDeviceName  +
                    "," + MELName  +
                    "," + DataSizeIoTDeviceToMEL_Mb +
                    "," + TransmissionTimeIoTDeviceToMEL +
                    "," + EdgeLetMISize +
                    "," + EdgeLet_MEL_StartTime +
                    "," + EdgeLet_MEL_FinishTime +
                    "," + EdgeLetProccessingTimeByMEL +
                    "," + DestinationVmName  +
                    "," + DataSizeMELToVM_Mb +
                    "," + TransmissionTimeMELToVM +
                    "," + CloudLetMISize +
                    "," + CloudLetProccessingTimeByVM +
                    "," + TransactionTotalTime ;
        }

        public Record(WorkflowInfo workflowTag, double transactionTotalTime) {
            Flow f = null;
            int size = workflowTag.getOsmosisLetSize();
            try {
                f = workflowTag.getOsmosisFlow(1);
            } catch (IndexOutOfBoundsException e) { }
            App_ID = (workflowTag.getAppId());
            AppName = workflowTag.getAppName();
            Transaction = (workflowTag.getWorkflowId());
            StartTime = workflowTag.getSartTime();
            FinishTime = workflowTag.getFinishTime();
            IoTDeviceName = workflowTag.getOsmosisFlow(0).getAppNameSrc();
            MELName = workflowTag.getOsmosisFlow(0).getAppNameDest() + " (" +workflowTag.getDCName(0) + ")";
            DataSizeIoTDeviceToMEL_Mb = workflowTag.getOsmosisFlow(0).getSize();
            TransmissionTimeIoTDeviceToMEL = workflowTag.getOsmosisFlow(0).getTransmissionTime();
            EdgeLetMISize = workflowTag.getOsmosislet(0).getCloudletLength();
            EdgeLet_MEL_StartTime = workflowTag.getOsmosislet(0).getExecStartTime();
            EdgeLet_MEL_FinishTime = workflowTag.getOsmosislet(0).getFinishTime();
            EdgeLetProccessingTimeByMEL = workflowTag.getOsmosislet(0).getActualCPUTime();
            DestinationVmName = f.getAppNameDest() + " (" +workflowTag.getDCName(1) + ")";
            DataSizeMELToVM_Mb = f.getSize();
            TransmissionTimeMELToVM = f.getTransmissionTime();
            CloudLetMISize = workflowTag.getOsmosislet(1).getCloudletLength();
            CloudLetProccessingTimeByVM = workflowTag.getOsmosislet(1).getActualCPUTime();
            TransactionTotalTime = transactionTotalTime;
        }

        void normalize(double max, double min, double newMax, double offset, long initTransact) {
            Transaction += initTransact;
            StartTime = offset+(StartTime - min)/(max-min) * newMax;
            FinishTime = offset+(FinishTime - min)/(max-min) * newMax;
            TransmissionTimeIoTDeviceToMEL = (TransmissionTimeIoTDeviceToMEL - min)/(max-min) * newMax;
            EdgeLet_MEL_StartTime = offset+(EdgeLet_MEL_StartTime-min)/(max-min) * newMax;
            EdgeLet_MEL_FinishTime = offset+(EdgeLet_MEL_FinishTime-min)/(max-min) * newMax;
            EdgeLetProccessingTimeByMEL = (EdgeLetProccessingTimeByMEL-min)/(max-min) * newMax;
            TransmissionTimeMELToVM = (TransmissionTimeMELToVM-min)/(max-min) * newMax;
            CloudLetProccessingTimeByVM = (CloudLetProccessingTimeByVM-min)/(max-min) * newMax;
            TransactionTotalTime = (TransactionTotalTime-min)/(max-min) * newMax;
        }
    }

    public static String[] headerApp = new String[]{"App_ID"
            ,"AppName"
            ,"Transaction"
            ,"StartTime"
            ,"FinishTime"
            ,"IoTDeviceName"
            ,"MELName"
            ,"DataSizeIoTDeviceToMEL_Mb"
            ,"TransmissionTimeIoTDeviceToMEL"
            ,"EdgeLetMISize"
            ,"EdgeLet_MEL_StartTime"
            ,"EdgeLet_MEL_FinishTime"
            ,"EdgeLetProccessingTimeByMEL"
            ,"DestinationVmName"
            ,"DataSizeMELToVM_Mb"
            ,"TransmissionTimeMELToVM"
            ,"CloudLetMISize"
            ,"CloudLetProccessingTimeByVM"
            , "TransactionTotalTime"};


    public static long dump_current_conf(ArrayList<Record> xyz, File parent, String prefix, double max_time, double offset, long initTransact) throws IOException {
        if (! parent.exists()){
            parent.mkdirs();
        } else if (parent.isFile())  {
            System.err.println("ERROR: the current file exists, and it is a file: a folder was expected. " + parent);
            System.exit(1);
        }
        HashMultimap<Integer, WorkflowInfo> map = HashMultimap.create();
        for(WorkflowInfo workflowTag : OsmesisBroker.workflowTag){
            map.put(workflowTag.getAppId(), workflowTag);
        }
        ArrayList<Record> ls = new ArrayList<>();
        for(var x : map.asMap().entrySet()){
            printOsmesisApp(ls, x.getValue());
        }
        double max = ls.stream().map(x -> x.FinishTime).max(Double::compare).get();
        for (var x : ls)
            x.normalize(max, 0, max_time, offset, initTransact);
        ls.sort((o1, o2) -> {
            int val = Double.compare(o1.StartTime, o2.StartTime);
            if (val == 0) {
                val = Double.compare(o1.FinishTime, o2.FinishTime);
                if (val == 0) {
                    val = o1.IoTDeviceName.compareTo(o2.IoTDeviceName);
                    if (val == 0) {
                        return o1.DestinationVmName.compareTo(o2.DestinationVmName);
                    } else
                        return val;
                } else
                    return val;
            } else
                return val;
        });
        xyz.addAll(ls);
        return (long)ls.size();
    }

    public static void printOsmesisApp(List<Record> bw, Collection<WorkflowInfo> tags) throws IOException {
        double transactionTransmissionTime = 0;
        double transactionOsmosisLetTime = 0;
        double transactionTotalTime;
        for (var workflowTag : tags) {
            transactionTransmissionTime = 0;
            transactionOsmosisLetTime = 0;
            for(int i = 0; i < workflowTag.getFlowLists().size(); i++){
                Flow flow = workflowTag.getOsmosisFlow(i);
                transactionTransmissionTime += flow.getTransmissionTime();
            }

            for(int x =0; x < workflowTag.getOsmosisLetSize(); x++){
                EdgeLet let = workflowTag.getOsmosislet(x);
                transactionOsmosisLetTime += let.getActualCPUTime();
            }

            bw.add(new Record(workflowTag, transactionTransmissionTime +  transactionOsmosisLetTime));
        }
    }

}
