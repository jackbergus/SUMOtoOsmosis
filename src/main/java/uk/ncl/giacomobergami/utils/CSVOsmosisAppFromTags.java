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
import com.opencsv.CSVWriter;
import org.cloudbus.cloudsim.edge.core.edge.EdgeLet;
import org.cloudbus.osmosis.core.Flow;
import org.cloudbus.osmosis.core.OsmesisBroker;
import org.cloudbus.osmosis.core.WorkflowInfo;

import java.io.*;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Collection;

public class CSVOsmosisAppFromTags {

    static String[] headerApp = new String[]{"App_ID"
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


    public static void dump_current_conf(File parent, String prefix, double current_time) throws IOException {
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
        File file = Paths.get(parent.getAbsolutePath(), prefix+"t"+current_time+".csv").toFile();
        FileOutputStream fos = new FileOutputStream(file);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        bw.write(String.join(",", headerApp));
        bw.newLine();
        for(var x : map.asMap().entrySet()){
            printOsmesisApp(bw, x.getValue());
        }
        bw.close();
        fos.close();
    }

    public static void printOsmesisApp(BufferedWriter bw, Collection<WorkflowInfo> tags) throws IOException {
        double transactionTransmissionTime = 0;
        double transactionOsmosisLetTime = 0;
        double transactionTotalTime;
        for (var workflowTag : tags) {
            transactionTransmissionTime = 0;
            transactionOsmosisLetTime = 0;
            transactionTotalTime = 0;
            for(int i = 0; i < workflowTag.getFlowLists().size(); i++){
                Flow flow = workflowTag.getOsmosisFlow(i);
                transactionTransmissionTime += flow.getTransmissionTime();
            }

            for(int x =0; x < workflowTag.getOsmosisLetSize(); x++){
                EdgeLet let = workflowTag.getOsmosislet(x);
                transactionOsmosisLetTime += let.getActualCPUTime();
            }
            transactionTotalTime = transactionTransmissionTime +  transactionOsmosisLetTime;
            bw.write(String.join(",", new String[]{String.valueOf(workflowTag.getAppId())
                    , workflowTag.getAppName()
                    , String.valueOf(workflowTag.getWorkflowId())
                    , new DecimalFormat("0.00000").format(workflowTag.getSartTime())
                    , new DecimalFormat("0.00000").format(workflowTag.getFinishTime())
                    , workflowTag.getOsmosisFlow(0).getAppNameSrc()
                    , workflowTag.getOsmosisFlow(0).getAppNameDest() + " (" +workflowTag.getDCName(0) + ")"
                    , String.valueOf(workflowTag.getOsmosisFlow(0).getSize())
                    , new DecimalFormat("0.00000").format(workflowTag.getOsmosisFlow(0).getTransmissionTime())
                    , String.valueOf(workflowTag.getOsmosislet(0).getCloudletLength())
                    , new DecimalFormat("0.00000").format(workflowTag.getOsmosislet(0).getExecStartTime())
                    , new DecimalFormat("0.00000").format(workflowTag.getOsmosislet(0).getFinishTime())
                    , new DecimalFormat("0.00000").format(workflowTag.getOsmosislet(0).getActualCPUTime())
                    , workflowTag.getOsmosisFlow(1).getAppNameDest() + " (" +workflowTag.getDCName(1) + ")"
                    , String.valueOf(workflowTag.getOsmosisFlow(1).getSize())
                    , new DecimalFormat("0.00000").format(workflowTag.getOsmosisFlow(1).getTransmissionTime())
                    , String.valueOf(workflowTag.getOsmosislet(1).getCloudletLength())
                    , new DecimalFormat("0.00000").format(workflowTag.getOsmosislet(1).getActualCPUTime())
                    , new DecimalFormat("0.00000").format(transactionTotalTime)}));
            bw.newLine();
        }
    }

}
