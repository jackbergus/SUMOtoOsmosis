/*
 * Title:        IoTSim-Osmosis-RES 1.0
 * Description:  IoTSim-Osmosis-RES enables the testing and validation of osmotic computing applications
 * 			     over heterogeneous edge-cloud SDN-aware environments powered by the Renewable Energy Sources.
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2021, Newcastle University (UK) and Saudi Electronic University (Saudi Arabia) and
 *                     AGH University of Science and Technology (Poland)
 *
 */

package org.cloudbus.cloudsim.osmesis.examples;

import org.cloudbus.agent.AgentBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.edge.core.edge.ConfiguationEntity;
import org.cloudbus.cloudsim.edge.core.edge.MEL;
import org.cloudbus.cloudsim.edge.utils.LogUtil;
import org.cloudbus.cloudsim.osmesis.examples.uti.LogPrinter;
import org.cloudbus.cloudsim.osmesis.examples.uti.PrintResults;
import org.cloudbus.cloudsim.osmesis.examples.uti.RESPrinter;
import org.cloudbus.cloudsim.sdn.Switch;
import org.cloudbus.osmosis.core.*;
import org.cloudbus.res.EnergyController;
import org.cloudbus.res.config.AppConfig;
import org.cloudbus.res.dataproviders.res.RESResponse;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A simple example that use Osmotic Agents.
 *
 * Osmotic Agents are implemented following MAPE (Monitor-Analyze-Plan-Execute) control loop principle.
 * The loop is triggered no more often than every 15 minutes.
 */

public class RES_example6 {
    //Workload and infrastructure configuration are the same as in the example 2.
    public static final String configurationFile = "inputFiles/res/RES_example6_infrastructure_2edges.json";
    public static final String osmesisAppFile =  "inputFiles/res/RES_example2_workload_single_day.csv";
    //RES configuration is the same as in the example 1.
    public static final String RES_CONFIG_FILE =  "inputFiles/res/RES_example1_energy_config.json";

    OsmosisBuilder topologyBuilder;
    OsmesisBroker osmesisBroker;
    List<OsmesisDatacenter> datacenters;
    List<MEL> melList;
    EdgeSDNController edgeSDNController;
    List<Vm> vmList;

    public static void main(String[] args) throws Exception {
        RES_example6 osmosis = new RES_example6();
        osmosis.start();
    }

    public void start() throws Exception{

        int num_user = 1; // number of users
        Calendar calendar = Calendar.getInstance();
        boolean trace_flag = false; // mean trace events

        // Set Agent and Message classes
        AgentBroker agentBroker = AgentBroker.getInstance();
        agentBroker.setDcAgentClass(RES_example6_DCAgent.class);
        agentBroker.setDeviceAgentClass(RES_example6_DeviceAgent.class);
        agentBroker.setAgentMessageClass(RES_example6_AgentMessage.class);

        //Simulation is not started yet thus there is not any MELs.
        //Links for Agents between infrastructure elements.
        agentBroker.addAgentLink("temperature_1", "Edge_1");
        agentBroker.addAgentLink("temperature_1", "Edge_2");

        //Osmotic Agents time interval
        agentBroker.setMAPEInterval(15*60);

        //Create Energy Controllers
        Map<String, EnergyController> energyControllers = getEnergyControllers();
        System.out.println(energyControllers);

        agentBroker.setEnergyControllers(energyControllers);

        //Set the simulation start time
        agentBroker.setSimulationStartTime("20160501:0000");

        // Initialize the CloudSim library
        CloudSim.init(num_user, calendar, trace_flag);
        osmesisBroker  = new OsmesisBroker("OsmesisBroker");
        topologyBuilder = new OsmosisBuilder(osmesisBroker);

        ConfiguationEntity config = buildTopologyFromFile(configurationFile);
        //
        if(config !=  null) {
            topologyBuilder.buildTopology(config);
        }

        OsmosisOrchestrator maestro = new OsmosisOrchestrator();

        OsmesisAppsParser.startParsingExcelAppFile(osmesisAppFile);
        List<SDNController> controllers = new ArrayList<>();
        for(OsmesisDatacenter osmesisDC : topologyBuilder.getOsmesisDatacentres()){
            osmesisBroker.submitVmList(osmesisDC.getVmList(), osmesisDC.getId());
            controllers.add(osmesisDC.getSdnController());
            osmesisDC.getSdnController().setWanOorchestrator(maestro);
        }
        controllers.add(topologyBuilder.getSdWanController());
        maestro.setSdnControllers(controllers);
        osmesisBroker.submitOsmesisApps(OsmesisAppsParser.appList);
        osmesisBroker.setDatacenters(topologyBuilder.getOsmesisDatacentres());

        double startTime = CloudSim.startSimulation();

        LogUtil.simulationFinished();
        PrintResults pr = new PrintResults();
        pr.printOsmesisNetwork();

        Log.printLine();

        for(OsmesisDatacenter osmesisDC : topologyBuilder.getOsmesisDatacentres()){
            List<Switch> switchList = osmesisDC.getSdnController().getSwitchList();
            LogPrinter.printEnergyConsumption(osmesisDC.getName(), osmesisDC.getSdnhosts(), switchList, startTime);
            Log.printLine();
        }

        Log.printLine();
        LogPrinter.printEnergyConsumption(topologyBuilder.getSdWanController().getName(), null, topologyBuilder.getSdWanController().getSwitchList(), startTime);
        Log.printLine();
        Log.printLine("Simulation Finished!");

        Log.printLine();
        Log.printLine("Post-mortem RES energy analysis!");
        RESPrinter res_printer = new RESPrinter();
        res_printer.postMortemAnalysis(energyControllers,"20160101:0000", true,36);
        res_printer.postMortemAnalysis(energyControllers,"20160501:0000", false, 36);
        res_printer.postMortemAnalysis(energyControllers,"20160901:0000", false, 36);
        Log.printLine("End of RES analysis!");
    }

    private Map<String, EnergyController> getEnergyControllers() throws IOException {
        RESResponse resResponse = AppConfig.RES_PARSER.parse(RES_CONFIG_FILE);
        return resResponse.getDatacenters()
                .stream()
                .map(EnergyController::fromDatacenter)
                .collect(Collectors.toMap(EnergyController::getEdgeDatacenterId, Function.identity()));
    }

    private ConfiguationEntity buildTopologyFromFile(String filePath) throws Exception {
        System.out.println("Creating topology from file " + filePath);
        ConfiguationEntity conf  = null;
        try (FileReader jsonFileReader = new FileReader(filePath)){
            conf = topologyBuilder.parseTopology(jsonFileReader);
        } catch (FileNotFoundException e) {
            System.out.println("ERROR: input configuration file not found");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Topology built:");
        return conf;
    }

    public void setEdgeSDNController(EdgeSDNController edc) {
        this.edgeSDNController = edc;
    }
}
