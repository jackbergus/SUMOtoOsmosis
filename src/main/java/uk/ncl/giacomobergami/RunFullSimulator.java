/*
 * RunSimulator.java
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



package uk.ncl.giacomobergami;

import com.eatthepath.jvptree.VPTree;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.edge.core.edge.ConfiguationEntity;
import org.cloudbus.cloudsim.edge.utils.LogUtil;
import org.cloudbus.osmosis.core.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import uk.ncl.giacomobergami.osmosis.CSVOsmosisRecord;
import uk.ncl.giacomobergami.sumo.TrafficLightInformation;
import uk.ncl.giacomobergami.sumo.VehicleRecord;
import uk.ncl.giacomobergami.utils.CSVOsmosisAppFromTags;
import uk.ncl.giacomobergami.utils.CartesianDistanceFunction;
import uk.ncl.giacomobergami.utils.SimulatorConf;
import uk.ncl.giacomobergami.utils.XPathUtil;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class RunFullSimulator {

    private boolean hasRun;
    private final SimulatorConf conf;
    private final DocumentBuilderFactory dbf;
    private final DocumentBuilder db;
    private final String aMel;
    private ConfiguationEntity canvas;
    private final Gson gson;
    private final SortedMap<Double, HashMap<String, Double>> time_to_consumption;

    private void runSumo() throws IOException {
        if (new File(conf.trace_file).exists()) {
            System.out.println("Skipping the sumo running: the trace_file already exists");
            return;
        }
        File fout = new File(conf.logger_file);
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(conf.sumo_program, "-c", conf.sumo_configuration_file_path, "--begin", Long.toString(conf.begin), "--end", Long.toString(conf.end), "--fcd-output", conf.trace_file);
        try {
            Process process = processBuilder.start();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                bw.write(line);
                bw.newLine();
            }
            int exitCode = process.waitFor();
            bw.write("\nExited with error code : ");
            bw.write(exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        bw.close();
    }

    public long runOsmosis(ArrayList<CSVOsmosisAppFromTags.TransactionRecord> x, double time, String json, String csv, long initTransact) throws Exception{
        OsmosisBuilder topologyBuilder;
        OsmesisBroker osmesisBroker;
        OsmesisAppsParser.appList.clear();
        OsmesisBroker.workflowTag.clear();
        int num_user = 1; // number of users
        Calendar calendar = Calendar.getInstance();
        // mean trace events
        // Initialize the CloudSim library
        CloudSim.init(num_user, calendar, false);
        if (conf.terminate_simulation_at > 0)
            CloudSim.terminateSimulation(conf.terminate_simulation_at);
        osmesisBroker  = new OsmesisBroker("OsmesisBroker");
        topologyBuilder = new OsmosisBuilder(osmesisBroker);
        ConfiguationEntity config = buildTopologyFromFile(json);
        topologyBuilder.buildTopology(config);
        OsmosisOrchestrator maestro = new OsmosisOrchestrator();
        OsmesisAppsParser.startParsingExcelAppFile(csv);
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
        CloudSim.startSimulation();
        LogUtil.simulationFinished();
        new File(csv).delete();
        new File(json).delete();
        return CSVOsmosisAppFromTags.dump_current_conf(x, new File(conf.OsmosisOutput), conf.experimentName, 1.0, time, initTransact, time_to_consumption);
    }

    public static void decompressGzip(Path source, Path target) throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(
                new FileInputStream(source.toFile()));
             FileOutputStream fos = new FileOutputStream(target.toFile())) {

            // copy GZIPInputStream to FileOutputStream
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }

        }
    }

    public void dumpSumo() throws Exception {
        long initTransact = 0;
        HashMap<Double, HashMap<String, List<String>>> inCurrentTime = new HashMap<>();
        ArrayList<CSVOsmosisAppFromTags.TransactionRecord> xyz = new ArrayList<>();
        File file = new File(conf.sumo_configuration_file_path);
        File folderOut = new File(conf.OsmosisConfFiles);
        if (! folderOut.exists()){
            folderOut.mkdirs();
        } else if (folderOut.isFile())  {
            System.err.println("ERROR: the current file exists, and it is a file: a folder was expected. " + folderOut);
            System.exit(1);
        }
        Document configurationFile = db.parse(file);
        double distanceSquared = conf.maximum_tl_distance_in_meters * conf.maximum_tl_distance_in_meters;

        File network_python = Paths.get(file.getParent(), XPathUtil.evaluate(configurationFile, "/configuration/input/net-file/@value"))
                .toFile();
        if (!network_python.exists()) {
            System.err.println("ERR: file " + network_python.getAbsolutePath() + " from " + file.getAbsolutePath() + " does not exists!");
            System.exit(1);
        } else if (network_python.getAbsolutePath().endsWith(".gz")) {
            String ap = network_python.getAbsolutePath();
            ap = ap.substring(0, ap.lastIndexOf('.'));
            decompressGzip(network_python.toPath(), new File(ap).toPath());
            network_python = new File(ap);
        }
        System.out.println("Loading the traffic light information...");
        Document networkFile = db.parse(network_python);
        ArrayList<TrafficLightInformation> tls = new ArrayList<>();
        var traffic_lights = XPathUtil.evaluateNodeList(networkFile, "/net/junction[@type='traffic_light']");
        for (int i = 0, N = traffic_lights.getLength(); i<N; i++) {
            var curr = traffic_lights.item(i).getAttributes();
            TrafficLightInformation tlInfo = new TrafficLightInformation();
            tlInfo.tl_id = curr.getNamedItem("id").getTextContent();
            tlInfo.tl_x = Double.parseDouble(curr.getNamedItem("x").getTextContent());
            tlInfo.tl_y = Double.parseDouble(curr.getNamedItem("y").getTextContent());
            tls.add(tlInfo);
        }
        //System.out.println(ls);

        CartesianDistanceFunction f = new CartesianDistanceFunction();
        File trajectory_python = new File(conf.trace_file);
        if (!trajectory_python.exists()) {
            System.err.println("ERROR: sumo has not built the trace file: " + trajectory_python.getAbsolutePath());
            System.exit(1);
        }
        System.out.println("Loading the vehicle information...");
        Document trace_document = db.parse(trajectory_python);
        ArrayList<CSVOsmosisRecord> csvFile = new ArrayList<>();
        var timestamp_eval = XPathUtil.evaluateNodeList(trace_document, "/fcd-export/timestep");
        for (int i = 0, N = timestamp_eval.getLength(); i<N; i++) {
            csvFile.clear();
            var curr = timestamp_eval.item(i);
            Double currTime = Double.valueOf(curr.getAttributes().getNamedItem("time").getTextContent());
            System.out.println(currTime);
            var tag = timestamp_eval.item(i).getChildNodes();
            ArrayList<VehicleRecord> vehs = new ArrayList<>(tag.getLength());
            for (int j = 0, M = tag.getLength(); j<M; j++) {
                var veh = tag.item(j);
                if (veh.getNodeType()== Node.ELEMENT_NODE) {
                    assert (Objects.equals(veh.getNodeName(), "vehicle"));
                    var attrs = veh.getAttributes();
                    VehicleRecord rec = new VehicleRecord();
                    rec.angle = Double.parseDouble(attrs.getNamedItem("angle").getTextContent());
                    rec.x = Double.parseDouble(attrs.getNamedItem("x").getTextContent());
                    rec.y = Double.parseDouble(attrs.getNamedItem("y").getTextContent());
                    rec.speed = Double.parseDouble(attrs.getNamedItem("speed").getTextContent());
                    rec.pos = Double.parseDouble(attrs.getNamedItem("pos").getTextContent());
                    rec.slope = Double.parseDouble(attrs.getNamedItem("slope").getTextContent());
                    rec.id = (attrs.getNamedItem("id").getTextContent());
                    rec.type = (attrs.getNamedItem("type").getTextContent());
                    rec.lane = (attrs.getNamedItem("lane").getTextContent());
                    vehs.add(rec);
                }
            }
            if (vehs.isEmpty()) continue;
            var tree = new VPTree<>(f, vehs);
            List<ConfiguationEntity.IotDeviceEntity> allDevices = new ArrayList<>();
            List<ConfiguationEntity.VMEntity> allDestinations = new ArrayList<>();
            boolean hasSomeResult = false;
            for (int j = 0, M = tls.size(); j < M; j++) {
                TrafficLightInformation x = tls.get(j);
                var distanceQueryResult = tree.getAllWithinDistance(x, distanceSquared);
                if (!distanceQueryResult.isEmpty()) {
                    hasSomeResult = true;
                    inCurrentTime.putIfAbsent(currTime, new HashMap<>());
                    var lsx = new ArrayList<String>();
                    inCurrentTime.get(currTime).put(x.tl_id, lsx);
                    for (var veh : distanceQueryResult) {
                        lsx.add(veh.id);
                        allDevices.add(veh.asIoDevice(conf.bw,
                                conf.max_battery_capacity,
                                conf.battery_sensing_rate,
                                conf.battery_sending_rate,
                                conf.network_type,
                                conf.protocol));
                        csvFile.add(new CSVOsmosisRecord(j, x, veh, conf, aMel));
                    }
                    allDestinations.add(x.asVMEntity(conf.VM_pes, conf.VM_mips, conf.VM_ram, conf.VM_storage, conf.VM_bw, conf.VM_cloudletPolicy));
                }
            }
            if (hasSomeResult) {
                canvas.getCloudDatacenter().get(0).setVMs(allDestinations);
                canvas.getEdgeDatacenter().get(0).setIoTDevices(allDevices);
                String confCURR = conf.experimentName+"_t"+currTime;
                File jsonFile =  Paths.get(folderOut.getAbsolutePath(), confCURR+".json").toFile();
                var fw = new FileWriter(jsonFile);
                gson.toJson(canvas, fw);
                fw.flush();
                fw.close();
                File CSV_CONF_FILE = Paths.get(folderOut.getAbsolutePath(), confCURR+".csv").toFile();
                CSVOsmosisRecord.WriteCsv(CSV_CONF_FILE, csvFile);
                initTransact += runOsmosis(xyz, currTime, jsonFile.getAbsolutePath(), CSV_CONF_FILE.getAbsolutePath(), initTransact);
            }
        }

        Path intersection_file_python = Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_tracesMatch.json");
        Files.writeString(intersection_file_python, gson.toJson(inCurrentTime));

        {
            FileOutputStream fos = new FileOutputStream(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+".csv").toFile());
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(String.join(",", CSVOsmosisAppFromTags.headerApp));
            bw.newLine();
            for (var x : xyz) {
                bw.write(String.join(",", x.toString()));
                bw.newLine();
            }
            bw.close();
            fos.close();
        }

        {
            FileOutputStream batt = new FileOutputStream(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_batt.csv").toFile());
            BufferedWriter b2 = new BufferedWriter(new OutputStreamWriter(batt));
            b2.write("Time,Sem,BatteryConsumption");
            b2.newLine();
            for (var x : time_to_consumption.entrySet()) {
                var t = x.getKey();
                for (var y : x.getValue().entrySet()) {
                    b2.write(t+","+y.getKey()+","+y.getValue());
                    b2.newLine();
                }
            }
            b2.close();
            batt.close();
        }

        {
            FileOutputStream tlsF = new FileOutputStream(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_tls.csv").toFile());
            BufferedWriter flsF2 = new BufferedWriter(new OutputStreamWriter(tlsF));
            flsF2.write("Id,X,Y");
            flsF2.newLine();
            for (var x : tls) {
                flsF2.write(x.tl_id +","+x.tl_x +","+x.tl_y);
                flsF2.newLine();
            }
            flsF2.close();
            tlsF.close();
        }
    }

    public void run() {
        if (!hasRun) {
            try {
                runSumo();
                dumpSumo();
            } catch (Exception e) {
                e.printStackTrace();
            }
            hasRun = true;
        }
    }

    public RunFullSimulator() throws Exception {
        File f = new File("sumo_configuration_file_path.yaml");
        if (!f.exists()) {
            System.err.println("ERROR: the current folder should contain a file called 'sumo_configuration_file_path.yaml'");
            System.exit(1);
        }
        var mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        conf = mapper.readValue(f, SimulatorConf.class);
        dbf = DocumentBuilderFactory.newInstance();
        db = dbf.newDocumentBuilder();
        gson = new GsonBuilder().setPrettyPrinting().create();
        canvas = buildTopologyFromFile(conf.default_json_conf_file);
        if (canvas.getEdgeDatacenter().isEmpty()) {
            System.err.println("ERROR: the json configuration file should contain at least one edge configuration");
            System.exit(1);
        }
        if (canvas.getEdgeDatacenter().get(0).getMELEntities().isEmpty()) {
            System.err.println("ERROR: the json configuration file should contain at least one edge MEL");
            System.exit(1);
        }
        aMel = canvas.getEdgeDatacenter().get(0).getMELEntities().get(0).getName();
        time_to_consumption = new TreeMap<>();
        hasRun = false;
    }

    public static void main(String[] args) throws Exception {
        var x = new RunFullSimulator();
        x.run();
    }

    private ConfiguationEntity buildTopologyFromFile(String filePath) throws Exception {
        try (FileReader jsonFileReader = new FileReader(filePath)){
            ConfiguationEntity conf = gson.fromJson(jsonFileReader, ConfiguationEntity.class);
            return conf;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

}
