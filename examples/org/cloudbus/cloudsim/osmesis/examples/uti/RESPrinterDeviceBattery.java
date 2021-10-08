package org.cloudbus.cloudsim.osmesis.examples.uti;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.osmosis.core.OsmesisAppDescription;
import org.cloudbus.osmosis.core.OsmesisAppsParser;
import org.cloudbus.osmosis.core.OsmesisBroker;
import org.cloudbus.osmosis.core.WorkflowInfo;
import org.cloudbus.res.EnergyController;
import org.cloudbus.res.config.AppConfig;
import org.cloudbus.res.model.RenewableEnergySource;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RESPrinterDeviceBattery {
    Map<String, Double> RESannual;
    Map<String, Double> RESaverage_power;
    Map<String, Double> average_power;
    Map<String, Double> RESutilization;
    Map<String, Integer> statistics;

    private LocalDateTime timeStartRES;
    int print_step;

    Map<String, EnergyController> energyControllers;

    public RESPrinterDeviceBattery() {
        RESannual = new HashMap<>();
        RESaverage_power = new HashMap<>();
        average_power = new HashMap<>();
        RESutilization = new HashMap<>();
        statistics = new HashMap<>();
    }

    public void postMortemAnalysis(Map<String, EnergyController> energyControllers, String time_s, boolean sources_details, int print_step) {
        RESannual.clear();
        RESaverage_power.clear();
        RESutilization.clear();
        statistics.clear();

        this.energyControllers = energyControllers;
        this.print_step = print_step;

        timeStartRES = LocalDateTime.parse(time_s, AppConfig.FORMATTER);

        //calculate RES parameters for each datacenter
        energyControllers.keySet().forEach(dc -> {
            RESutilization.put(dc,energyControllers.get(dc).getUtilization());
            if (sources_details) {
                Log.printLine(dc + " RES utilisation:\t " + energyControllers.get(dc).getUtilization() + "%");
                Log.printLine(dc + " RES sources:\t " + energyControllers.get(dc).getEnergySources().size());
            }
            double annual_dc=0;
            for(RenewableEnergySource resSource: energyControllers.get(dc).getEnergySources()){
                double annual = resSource.getEnergyData().getAnnualEnergy();
                annual_dc += annual;
                if (sources_details) {
                    Log.printLine(dc + " " + resSource.getName() + " annual RES energy:\t" + annual + " Wh");
                    Log.printLine(dc + " " + resSource.getName() + " average RES power:\t" + annual / 365 / 24 + " W");
                }
            }
            RESannual.put(dc,annual_dc);
            RESaverage_power.put(dc,annual_dc/365/24);
            average_power.put(dc,annual_dc/365/24 / (energyControllers.get(dc).getUtilization() / 100.0) );

            if (sources_details) {
                Log.printLine(dc + " average power consumption:\t" + average_power.get(dc) + " W");
            }
        });

        //collect all osmotic flows
        List<WorkflowInfo> tags = new ArrayList<>();
        for (OsmesisAppDescription app : OsmesisAppsParser.appList) {
            for (WorkflowInfo workflowTag : OsmesisBroker.workflowTag) {
                workflowTag.getAppId();
                if (app.getAppID() == workflowTag.getAppId()) {
                    tags.add(workflowTag);
                }
            }
            AnalyseFlowsRES(tags);
            tags.clear();
        }
    }

    private void AnalyseFlowsRES(List<WorkflowInfo> tags) {
        Log.printLine();
        Log.printLine("=========================== Osmosis App Results RES (START = "+timeStartRES+") (step = "+print_step+")========================");
        Log.printLine(String.format("%1s\t%11s\t%18s\t%13s\t%19s\t%22s\t%15s\t%22s\t%23s\t%22s\t%22s"
                ,"App_ID"
                ,"AppName"
                ,"Transaction"
                ,"Edglet DC"
                ,"Edglet CPU Time"
                ,"Edglet Start Time"
                ,"Cloudlet DC"
                ,"Cloudlet CPU Time"
                ,"Cloudlet Start Time"
                ,"CPU RES utilisation"
                ,"CPU low-emission utilisation"));

        double transactionTotalTime;
        double transactionTotalCpuTime;

        double transaction_total_CPU_RES_utilization=0;
        double transaction_total_CPU_lowEmission_utilization=0;

        double transaction_CPU_RES_utilization = 0;
        double transaction_CPU_lowEmission_utilization = 0;


        for(WorkflowInfo workflowTag : tags){
            transactionTotalTime =  workflowTag.getIotDeviceFlow().getTransmissionTime() + workflowTag.getEdgeLet().getActualCPUTime()
                    + workflowTag.getEdgeToCloudFlow().getTransmissionTime() + workflowTag.getCloudLet().getActualCPUTime();
            transactionTotalCpuTime = workflowTag.getEdgeLet().getActualCPUTime() + workflowTag.getCloudLet().getActualCPUTime();

            int app_id = workflowTag.getAppId();
            String app_name = workflowTag.getAppName();

            int worflow_id = workflowTag.getWorkflowId();

            String edglet_dc = workflowTag.getSourceDCName();
            double edglet_cpu_time = workflowTag.getEdgeLet().getActualCPUTime();
            double edglet_start_time = workflowTag.getEdgeLet().getExecStartTime();

            String cloudlet_dc = workflowTag.getDestinationDCName();
            double cloudlet_cpu_time = workflowTag.getCloudLet() .getActualCPUTime();
            double cloudlet_start_time = workflowTag.getCloudLet().getExecStartTime();

            double edglet_power = energyControllers.get(edglet_dc).getRESCurrentPower(timeStartRES.plusNanos((long) (edglet_start_time*1000000000)));
            double cloudlet_power = energyControllers.get(cloudlet_dc).getRESCurrentPower(timeStartRES.plusNanos((long) (cloudlet_start_time*1000000000)));

            double edglet_lowEmission = energyControllers.get(edglet_dc).getPowerGrids().get(0).getLowEmission();
            double cloudlet_lowEmission = energyControllers.get(cloudlet_dc).getPowerGrids().get(0).getLowEmission();

            if (edglet_power > RESaverage_power.get(edglet_dc)){
                edglet_power = RESaverage_power.get(edglet_dc);
            }

            if (cloudlet_power > RESaverage_power.get(cloudlet_dc)){
                cloudlet_power = RESaverage_power.get(cloudlet_dc);
            }

            double ed_part=0;
            double cl_part=0;

            double ed_alpha = 0;
            double cl_alpha = 0;
            transaction_CPU_RES_utilization = 0;
            transaction_CPU_lowEmission_utilization = 0;
            if (edglet_power > 0.0 && cloudlet_power > 0.0){
                //if there is energy from PV panels
                ed_part = (edglet_power/average_power.get(edglet_dc)) * edglet_cpu_time; // * RESutilization.get(edglet_dc)
                cl_part = (cloudlet_power/average_power.get(cloudlet_dc)) * cloudlet_cpu_time; // * RESutilization.get(cloudlet_dc)

                ed_alpha = (edglet_power/average_power.get(edglet_dc));
                cl_alpha = (cloudlet_power/average_power.get(cloudlet_dc));

                transaction_CPU_RES_utilization = (ed_part + cl_part) / (edglet_cpu_time+cloudlet_cpu_time) * 100;
            }

            {
                ed_part = (edglet_lowEmission/100.0*(1-ed_alpha) + ed_alpha) * edglet_cpu_time; // * RESutilization.get(edglet_dc)
                cl_part = (cloudlet_lowEmission/100.0*(1-cl_alpha) + cl_alpha) * cloudlet_cpu_time; // * RESutilization.get(cloudlet_dc)

                transaction_CPU_lowEmission_utilization = (ed_part + cl_part) / (edglet_cpu_time+cloudlet_cpu_time) * 100;
            }

            if (transaction_CPU_RES_utilization > 100.0){
                transaction_CPU_RES_utilization = 100.0;
            }

            transaction_total_CPU_RES_utilization+=transaction_CPU_RES_utilization;

            transaction_total_CPU_lowEmission_utilization+=transaction_CPU_lowEmission_utilization;


            if (worflow_id % print_step == 0) {
                Log.printLine(String.format("%1s\t%15s\t%15s\t%18s\t%18s\t%21s\t%15s\t%21s\t%20s\t%20s\t%20s"
                        , app_id
                        , app_name
                        , worflow_id
                        , edglet_dc
                        , new DecimalFormat("0.00").format(edglet_cpu_time)
                        , new DecimalFormat("0.00").format(edglet_start_time)
                        , cloudlet_dc
                        , new DecimalFormat("0.00").format(cloudlet_cpu_time)
                        , new DecimalFormat("0.00").format(cloudlet_start_time)
                        , new DecimalFormat("0.00").format(transaction_CPU_RES_utilization)
                        , new DecimalFormat("0.00").format(transaction_CPU_lowEmission_utilization)) );
            }

            statistics.put(edglet_dc,statistics.getOrDefault(edglet_dc,0)+1);
        }

        Log.printLine(String.format("Self-consumed RES Utilization for workload CPU processing: %s",transaction_total_CPU_RES_utilization/tags.size()));
        Log.printLine(String.format("Low carbon ES utilization for workload CPU processing: %s",transaction_total_CPU_lowEmission_utilization/tags.size()));

        for(String s:statistics.keySet()){
            Log.printLine(String.format("Datacenter: %s  count:%d", s,statistics.get(s)));
        }
    }

}
