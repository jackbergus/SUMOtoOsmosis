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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.ortools.Loader;
import io.jenetics.ext.moea.Pareto;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudbus.cloudsim.edge.core.edge.ConfiguationEntity;
import org.jblas.DoubleMatrix;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import uk.ncl.giacomobergami.algorithmics.ClusterDifference;
import uk.ncl.giacomobergami.osmosis.CSVOsmosisRecord;
import uk.ncl.giacomobergami.osmosis.RSUProgram;
import uk.ncl.giacomobergami.osmosis.VehicularProgram;
import uk.ncl.giacomobergami.solver.*;
import uk.ncl.giacomobergami.sumo.VehicleRecord;
import uk.ncl.giacomobergami.utils.CartesianDistanceFunction;
import uk.ncl.giacomobergami.utils.SimulatorConf;
import uk.ncl.giacomobergami.utils.StringComparator;
import uk.ncl.giacomobergami.utils.XPathUtil;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class NoOsmosis {

    private boolean hasRun;
    private final SimulatorConf conf;
    private final DocumentBuilderFactory dbf;
    private final DocumentBuilder db;
//    private final String aMel;
//    private ConfiguationEntity canvas;
    private final Gson gson;
//    private final SortedMap<Double, HashMap<String, Double>> time_to_consumption;

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

        // With this, choosing the comparator given the specified parameters
        Comparator<double[]> comparator;
        if (conf.use_pareto_front) {
            comparator = Pareto::dominance;
        } else {
            comparator = Comparator.comparingDouble(o -> o[0] * conf.p1 + o[1] * conf.p2 + o[2] * (1 - conf.p1  - conf.p2));
        }

        HashMap<Double, Long> problemSolvingTime = new HashMap<>();
        TreeMap<Double, ArrayList<LocalTimeOptimizationProblem.Solution>> simulationSolutions = new TreeMap<>();
        HashSet<Vehicle> intersectingVehicles = new HashSet<>();
//        ArrayList<CSVOsmosisAppFromTags.TransactionRecord> xyz = new ArrayList<>();
        File file = new File(conf.sumo_configuration_file_path);
        File folderOut = new File(conf.OsmosisConfFiles);
        File folderOut2 = new File(conf.OsmosisOutput);
        if (! folderOut2.exists()) {
            folderOut2.mkdirs();
        } else if (folderOut2.isFile()) {
            System.err.println("ERROR: the current file exists, and it is a file: a folder was expected. " + folderOut2);
            System.exit(1);
        }
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
        ArrayList<RSU> tls = new ArrayList<>();
//        HashMap<String, Integer> tlsMap = new HashMap<>();
        var traffic_lights = XPathUtil.evaluateNodeList(networkFile, "/net/junction[@type='traffic_light']");
        for (int i = 0, N = traffic_lights.getLength(); i<N; i++) {
            var curr = traffic_lights.item(i).getAttributes();
            RSU tlInfo = new RSU(curr.getNamedItem("id").getTextContent(),
                    Double.parseDouble(curr.getNamedItem("x").getTextContent()),
                            Double.parseDouble(curr.getNamedItem("y").getTextContent()),
                                    conf.best_distance,
                    conf.max_threshold);
            tls.add(tlInfo);
//            tlsMap.put(tlInfo.id, i);
        }
        DoubleMatrix sqDistanceMatrix = DoubleMatrix.zeros(traffic_lights.getLength(),traffic_lights.getLength());
        for (int i = 0, N = traffic_lights.getLength(); i<N; i++) {
            var semX = tls.get(i);
            for (int j = 0; j<i; j++) {
                var semY = tls.get(j);
                final double deltaX = semX.tl_x - semY.tl_x;
                final double deltaY = semX.tl_y - semY.tl_y;
                sqDistanceMatrix.put(i, j, ((deltaX * deltaX) + (deltaY * deltaY)));
                sqDistanceMatrix.put(j, i, ((deltaX * deltaX) + (deltaY * deltaY)));
            }
        }

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
            ArrayList<Vehicle> vehs = new ArrayList<>(tag.getLength());
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
                    intersectingVehicles.add(rec.copy());
                }
            }
            if (vehs.isEmpty()) continue;

            LocalTimeOptimizationProblem solver = new LocalTimeOptimizationProblem(vehs, tls, conf);
            List<ConfiguationEntity.IotDeviceEntity> allDevices = new ArrayList<>();
            List<ConfiguationEntity.VMEntity> allDestinations = new ArrayList<>();

            if (solver.init(allDevices, allDestinations)) { // also, re-setting the time benchmark
                int expectedTotalVehs = allDevices.size();

                if (conf.isDo_thresholding()) {
                    if (conf.use_nearest_MEL_to_IoT) {
                        solver.setNearestMELForIoT();
                    } else {
                        solver.setAllPossibleMELForIoT();
                    }
                    if (conf.use_greedy_algorithm) {
                        solver.setGreedyPossibleTargetsForIoT(conf.use_local_demand_forecast);
                    } else if (conf.use_top_k_nearest_targets > 0) {
                        solver.setAllPossibleNearestKTargetsForCommunication(conf.use_top_k_nearest_targets, conf.use_top_k_nearest_targets_randomOne);
                    }  else {
                        solver.setAllPossibleTargetsForCommunication();
                    }
                } else {
                    solver.alwaysCommunicateWithTheNearestMel();
                }


                ArrayList<LocalTimeOptimizationProblem.Solution> sol =
                        solver.multi_objective_pareto(conf.k1, conf.k2, conf.ignore_cubic, comparator, conf.reduce_to_one, conf.update_after_flow);

                problemSolvingTime.put(currTime, solver.getRunTime());
                simulationSolutions.put(currTime, sol);

//                canvas.getCloudDatacenter().get(0).setVMs(allDestinations);
//                canvas.getEdgeDatacenter().get(0).setIoTDevices(allDevices);
//                String confCURR = conf.experimentName+"_t"+currTime;
//                File jsonFile =  Paths.get(folderOut.getAbsolutePath(), confCURR+".json").toFile();
//                var fw = new FileWriter(jsonFile);
//                gson.toJson(canvas, fw);
//                fw.flush();
//                fw.close();
//                File CSV_CONF_FILE = Paths.get(folderOut.getAbsolutePath(), confCURR+".csv").toFile();
//                CSVOsmosisRecord.WriteCsv(CSV_CONF_FILE, csvFile);
            }
        }

        List<String> tls_s = tls.stream().map(x -> x.tl_id).collect(Collectors.toList());
        List<String> veh_s = intersectingVehicles.stream().map(x -> x.id).collect(Collectors.toList());

        System.out.println("Computing all of the possible Pareto Routing scenarios...");

        if (simulationSolutions.values().stream().anyMatch(ArrayList::isEmpty)) {
            System.err.println("NO viable solution found!");
        } else {

            Double bestResultScore = Double.MAX_VALUE;

            CandidateSolutionParameters candidate = new CandidateSolutionParameters();

            if (conf.clairvoyance) {
                TemporalNetworkingRanking.oracularBestNetworking(simulationSolutions, veh_s, bestResultScore, candidate, conf.removal, conf.addition, comparator);
            } else {
                TemporalNetworkingRanking.nonclairvoyantBestNetworking(simulationSolutions, veh_s, bestResultScore, candidate, conf.removal, conf.addition, comparator);
            }

            // SETTING UP THE VEHICULAR PROGRAMS
            HashMap<String, VehicularProgram> vehProgramHashMap = new HashMap<>();
            for (var veh : intersectingVehicles) {
                var vehProgram = new VehicularProgram(candidate.delta_associations.get(veh.id));
                for (var entry : candidate.bestResult.entrySet()){
                    vehProgram.putDeltaRSUAssociation(entry.getKey(), entry.getValue().slowRetrievePath(veh));
                }
                vehProgram.finaliseProgram();
                vehProgramHashMap.put(veh.id, vehProgram);
            }

            TreeMap<Double, Map<String, List<String>>> networkTopology = new TreeMap<>(); // Actually, for RSU programs: saving one iteration cycle
            for (var entry : candidate.bestResult.entrySet()){
                var npMap = entry.getValue()
                     .RSUNetworkNeighbours
                     .entrySet()
                     .stream()
                     .map(x -> new ConcretePair<>(x.getKey().tl_id,
                             x.getValue().stream().map(y -> y.tl_id).collect(Collectors.toList())))
                        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
                networkTopology.put(entry.getKey(), npMap);
                for (var vehs : entry.getValue().getAlphaAssociation()) {
                    vehProgramHashMap.get(vehs.getKey().id).setLocalInformation(entry.getKey(), (VehicleRecord) vehs.getKey());
                }
            }

            // SETTING UP THE RSU PROGRAMS
            // This concept is relevant, so if we need to remove some nodes from the simulation,
            // and to add others. This also defines with which MELs and Vehicles should an element connect/disconnect
            // for its routing
            HashMap<String, RSUProgram> rsuProgramHashMap = new HashMap<>();
            var delta_clusters = ClusterDifference.diff(candidate.inStringTime, tls_s, StringComparator.getInstance());
            var delta_network_neighbours = ClusterDifference.diff(networkTopology, tls_s, StringComparator.getInstance());

            for (var r : tls) {
                var rsuProgram = new RSUProgram(candidate.bestResult.keySet());
                rsuProgram.finaliseProgram(delta_clusters.get(r.tl_id), delta_network_neighbours.get(r.tl_id));
                rsuProgramHashMap.put(r.tl_id, rsuProgram);
            }

            // TODO: simulation using programHashMap
            // delta_associations contains the associations of each IoT device with a MEL
            // So, we can give each agent this information as the "program"/trace they will execute
            // The other MELs, will just blatantly forward/send the messages to the destination via Routing (?)
            // At each simulation time t, get the nearest trace event T describing it, so t~T
            //
            // 1. If unchanged, SCENARIO 1: then do nothing at all
            //                  SCENARIO 2: then send another message to the same server
            // 2. If changed, remove any message that was coming as from a reponse from my previous send
            //                send new messages to my new last mile MEL
            // 3. If the current implementation has no routing information, then we can use the
            //    vehicular programs in programHashMap, which state at each time with which node
            //    it should be communicating with, and which is the path for establish such a communication

            Path intersection_file_python = Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_tracesMatch.json");
            Files.writeString(intersection_file_python, gson.toJson(candidate.inCurrentTime));


            Files.writeString(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_delta_clusters.json"), gson.toJson(delta_clusters));
            Files.writeString(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_delta_assocs.json"), gson.toJson(candidate.delta_associations));
            Files.writeString(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_iot_programs.json"), gson.toJson(vehProgramHashMap));
            Files.writeString(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_rsu_programs.json"), gson.toJson(rsuProgramHashMap));


            {
                FileOutputStream tlsF = new FileOutputStream(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_time_benchmark.csv").toFile());
                BufferedWriter flsF2 = new BufferedWriter(new OutputStreamWriter(tlsF));
                flsF2.write("sim_time,bench_time");
                flsF2.newLine();
                for (var x : problemSolvingTime.entrySet()) {
                    flsF2.write(x.getKey()+","+x.getValue());
                    flsF2.newLine();
                }
                flsF2.close();
                tlsF.close();
            }

            {
                FileOutputStream tlsF = new FileOutputStream(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_tls.csv").toFile());
                BufferedWriter flsF2 = new BufferedWriter(new OutputStreamWriter(tlsF));
                flsF2.write("Id,X,Y");
                var XMax = tls.stream().map(x -> x.tl_x).max(Comparator.comparingDouble(y -> y)).get();
                var YMin = tls.stream().map(x -> x.tl_y).min(Comparator.comparingDouble(y -> y)).get();
                tls.sort(Comparator.comparingDouble(sem -> {
                    double x = sem.tl_x - XMax;
                    double y = sem.tl_y - YMin;
                    return (x*x)+(y*y);
                }));

                System.out.println(            tls.subList(0, 8).stream().map(x -> x.tl_id
                ).collect(Collectors.joining("\",\"","LS <- list(\"", "\")")));

                flsF2.newLine();
                for (var x : tls) {
                    flsF2.write(x.tl_id +","+x.tl_x +","+x.tl_y);
                    flsF2.newLine();
                }
                flsF2.close();
                tlsF.close();
            }

            {
                FileOutputStream tlsF = new FileOutputStream(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_tracesMatch_toplot.csv").toFile());
                BufferedWriter flsF2 = new BufferedWriter(new OutputStreamWriter(tlsF));
                flsF2.write("SimTime,Sem,NVehs");
                flsF2.newLine();
                List<Vehicle> e = Collections.emptyList();
                for (var cp : candidate.inCurrentTime.entrySet()) {
                    Double time = cp.getKey();
                    for (var sem : tls) {
                        flsF2.write(time+","+sem.tl_id +","+cp.getValue().getOrDefault(sem, e).size());
                        flsF2.newLine();
                    }
                }
                flsF2.close();
                tlsF.close();
            }
        }






//        {
//            FileOutputStream fos = new FileOutputStream(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+".csv").toFile());
//            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
//            bw.write(String.join(",", CSVOsmosisAppFromTags.headerApp));
//            bw.newLine();
//            for (var x : xyz) {
//                bw.write(String.join(",", x.toString()));
//                bw.newLine();
//            }
//            bw.close();
//            fos.close();
//        }

//        {
//            FileOutputStream batt = new FileOutputStream(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_batt.csv").toFile());
//            BufferedWriter b2 = new BufferedWriter(new OutputStreamWriter(batt));
//            b2.write("Time,Sem,BatteryConsumption");
//            b2.newLine();
//            for (var x : time_to_consumption.entrySet()) {
//                var t = x.getKey();
//                for (var y : x.getValue().entrySet()) {
//                    b2.write(t+","+y.getKey()+","+y.getValue());
//                    b2.newLine();
//                }
//            }
//            b2.close();
//            batt.close();
//        }


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

    public NoOsmosis(String x) throws Exception {
        System.out.println(x);
        Thread.sleep(1000);
        File f = new File(x);
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
        hasRun = false;
    }

    public static void main(String[] args) throws Exception {
        Loader.loadNativeLibraries();
        var x = new NoOsmosis(args.length > 0 ? args[0] : "sumo_configuration_file_path.yaml");
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
