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
import org.cloudbus.cloudsim.edge.core.edge.ConfiguationEntity;
import org.jblas.DoubleMatrix;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import uk.ncl.giacomobergami.osmosis.CSVOsmosisRecord;
import uk.ncl.giacomobergami.sumo.TrafficLightInformation;
import uk.ncl.giacomobergami.sumo.VehicleRecord;
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
        HashMap<Double, HashMap<String, List<String>>> inCurrentTime = new HashMap<>();
//        ArrayList<CSVOsmosisAppFromTags.TransactionRecord> xyz = new ArrayList<>();
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
        HashMap<String, Integer> tlsMap = new HashMap<>();
        var traffic_lights = XPathUtil.evaluateNodeList(networkFile, "/net/junction[@type='traffic_light']");
        for (int i = 0, N = traffic_lights.getLength(); i<N; i++) {
            var curr = traffic_lights.item(i).getAttributes();
            TrafficLightInformation tlInfo = new TrafficLightInformation();
            tlInfo.id = curr.getNamedItem("id").getTextContent();
            tlInfo.x = Double.parseDouble(curr.getNamedItem("x").getTextContent());
            tlInfo.y = Double.parseDouble(curr.getNamedItem("y").getTextContent());
            tls.add(tlInfo);
            tlsMap.put(tlInfo.id, i);
        }
        DoubleMatrix sqDistanceMatrix = DoubleMatrix.zeros(traffic_lights.getLength(),traffic_lights.getLength());
        for (int i = 0, N = traffic_lights.getLength(); i<N; i++) {
            var semX = tls.get(i);
            for (int j = 0; j<i; j++) {
                var semY = tls.get(j);
                final double deltaX = semX.x - semY.x;
                final double deltaY = semX.y - semY.y;
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
                    inCurrentTime.get(currTime).put(x.id, lsx);
                    for (var veh : distanceQueryResult) {
                        lsx.add(veh.id);
                        allDevices.add(veh.asIoDevice(conf.bw,
                                conf.max_battery_capacity,
                                conf.battery_sensing_rate,
                                conf.battery_sending_rate,
                                conf.network_type,
                                conf.protocol));
//                        csvFile.add(new CSVOsmosisRecord(j, x, veh, conf, aMel));
                    }
                    allDestinations.add(x.asVMEntity(conf.VM_pes, conf.VM_mips, conf.VM_ram, conf.VM_storage, conf.VM_bw, conf.VM_cloudletPolicy));
                }
            }
            if (hasSomeResult) {
                if (conf.isDo_thresholding())
                    simulateTraffic(inCurrentTime.get(currTime),
                            tls,
                            tlsMap,
                            conf.getMax_threshold(),
                            conf.getBest_distance());
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

        Path intersection_file_python = Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_tracesMatch.json");
        Files.writeString(intersection_file_python, gson.toJson(inCurrentTime));


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

        {
            FileOutputStream tlsF = new FileOutputStream(Paths.get(new File(conf.OsmosisOutput).getAbsolutePath(), conf.experimentName+"_tls.csv").toFile());
            BufferedWriter flsF2 = new BufferedWriter(new OutputStreamWriter(tlsF));
            flsF2.write("Id,X,Y");
            var XMax = tls.stream().map(x -> x.x).max(Comparator.comparingDouble(y -> y)).get();
            var YMin = tls.stream().map(x -> x.y).min(Comparator.comparingDouble(y -> y)).get();
            tls.sort(Comparator.comparingDouble(sem -> {
                double x = sem.x - XMax;
                double y = sem.y - YMin;
                return (x*x)+(y*y);
            }));

            System.out.println(            tls.subList(0, 8).stream().map(x -> x.id
            ).collect(Collectors.joining("\",\"","LS <- list(\"", "\")")));

            flsF2.newLine();
            for (var x : tls) {
                flsF2.write(x.id+","+x.x+","+x.y);
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
            List<String> e = Collections.emptyList();
            for (var cp : inCurrentTime.entrySet()) {
                Double time = cp.getKey();
                for (var sem : tls) {
                    flsF2.write(time+","+sem.id+","+cp.getValue().getOrDefault(sem.id, e).size());
                    flsF2.newLine();
                }
            }
            flsF2.close();
            tlsF.close();
        }
    }

    private void simulateTraffic(HashMap<String, List<String>> stringListHashMap,
                                 ArrayList<TrafficLightInformation> semList,
                                 HashMap<String, Integer> semId,
                                 final int maxThreshold,
                                 int best_distance) {
        if (stringListHashMap == null) return;
        HashMap<String, List<String>> updated =  new HashMap<>();
        stringListHashMap.forEach((x,y)-> updated.put(x, new ArrayList<>(y)));
        int bestSqDistance = best_distance * best_distance;

        Comparator<TrafficLightInformation> IntCmp = Comparator.comparingInt(z -> {
            var x = updated.get(z.id);
            return  (x == null) ? 0 : x.size();
        });

        // Sorting the semaphores by the most critical ones
        List<TrafficLightInformation> sortedSemaphores = semList.stream()
                .sorted(IntCmp.reversed())
                .collect(Collectors.toList());

        HashMap<String, Set<String>> strayVehFrom = new HashMap<>();
        Set<String> strayVehs = new HashSet<>(); // Vehicles that might fall off from an association
        Set<String> allVehs = new HashSet<>(); // Vehicles already associated to a semaphore
        for (var sem : sortedSemaphores) {
            var semIdX = semId.get(sem.id);
            var vehs = updated.get(sem.id);
            if ((vehs == null) || vehs.size() < maxThreshold) break;

            var LS = vehs.subList(maxThreshold-1, vehs.size());
            strayVehs.addAll(LS);
            strayVehFrom.put(sem.id, new HashSet<>(LS));
            vehs.removeAll(LS);
            allVehs.addAll(vehs);
        }

        // Removing the stray vehicles that are already associated to a good semaphore
        strayVehs.removeAll(allVehs);
        strayVehFrom.forEach((k,v) -> v.removeAll(allVehs));
        strayVehFrom.entrySet().stream().filter(cp-> cp.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
                .forEach(strayVehFrom::remove);

        // Vehicles to get re-allocated
        HashMap<String, Set<String>> distributorOf = new HashMap<>();
        HashMap<String, List<TrafficLightInformation>> personalDistributors = new HashMap<>();
        if (!strayVehs.isEmpty()) {

            // Determining which semaphores will be affected by over-demands by busy semaphores
            for (var sem : strayVehFrom.keySet()) {
                // A valid distributor is a semaphore which is not currently full
                var distributor = semList.stream()
                        .filter(x -> {
                            var ls = updated.get(x.id);
                            return (!x.id.equals(sem)) && ((ls == null ? 0 : ls.size()) < maxThreshold);
                        })
                        .collect(Collectors.toList());
                for (var cp2 : distributor) {
                    if (!distributorOf.containsKey(cp2.id))
                        distributorOf.put(cp2.id, new HashSet<>());
                    distributorOf.get(cp2.id).add(sem);
                }
                personalDistributors.put(sem, distributor);
            }

            for (var x : personalDistributors.entrySet()) {
                // The more the semaphores are near to x, the more the space left, and the less
                // the requests that the semaphore might receive from the siblings, the better
                // Sorting the candidates accordingly.
                // Last, splitting the semaphores into free and overloaded.
                var map = x.getValue().stream()
                        .sorted(Comparator.comparingDouble(y -> {
                            var distrOf = distributorOf.get(y.id);
                            var demandForecast = 1.0 / (((double) (distrOf == null ? 0 : distrOf.size()))+1.0);
                            return demandForecast * availFormula(updated, maxThreshold, semList.get(semId.get(x.getKey())), y);
                        }))
                        .collect(Collectors.groupingBy(y -> {
                            var ls = updated.get(y.id);
                            var sem = semList.get(semId.get(x.getKey()));
                            double xDistX = (y.x - sem.x),
                                    xDistY = (y.y - sem.y);
                            double xDistSq = (xDistX*xDistX)+(xDistY*xDistY);
                            return ((ls == null ? 0 : ls.size()) < maxThreshold) && (xDistSq < bestSqDistance);
                        }));
                var locVehs = new ArrayList<>(strayVehFrom.get(x.getKey()));
                int i = 0, N = locVehs.size();

                Set<TrafficLightInformation> okTL = new HashSet<>(), stopIL = new HashSet<>();
                var mapOk =  map.get(true);
                if (mapOk != null) okTL.addAll(mapOk);
                mapOk = map.get(false);
                if (mapOk != null)
                    stopIL.addAll(mapOk);

                // Simulating an uniform distribution among the available elements.
                // Then, stopping as soon as they get full
                while (!okTL.isEmpty()) {
                    // Continuing to allocate vehicles until there is something left
                    if (i>=N) break;
                    for (var sem : okTL) {
                        if (i>=N) break;
                        updated.putIfAbsent(sem.id, new ArrayList<>());
                        if (updated.get(sem.id).size() > maxThreshold) {
                            stopIL.add(sem);
                        } else {
                            updated.get(sem.id).add(locVehs.get(i++));
                        }
                    }
                    okTL.removeAll(stopIL);
                }

                // Uniformly re-distribuiting the load among the other remaining semaphores
                while (i<N) {
                    for (var sem : stopIL) {
                        if (i>=N) break;
                        updated.putIfAbsent(sem.id, new ArrayList<>());
                        updated.get(sem.id).add(locVehs.get(i++));
                    }
                }
            }

            stringListHashMap.putAll(updated);
        }
    }

    private double availFormula(HashMap<String, List<String>> stringListHashMap, int maxThreshold, TrafficLightInformation sem, TrafficLightInformation x) {
        var ls = stringListHashMap.get(x.id);
        double xAvail = maxThreshold - (ls == null ? 0 : ls.size());
        if (Math.abs(xAvail)>=Double.MIN_NORMAL) {
            double absAvail = Math.abs(xAvail);
            double xDistX = (x.x - sem.x),
                    xDistY = (x.y - sem.y);
            double xDistSq = (xDistX*xDistX)+(xDistY*xDistY);
            xDistSq = xDistSq / (xDistSq+1); // normalization
            return Math.signum(xAvail) * (absAvail / (absAvail + 1)) * xDistSq;
        } else {
            return 0.0;
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

    public NoOsmosis() throws Exception {
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
        hasRun = false;
    }

    public static void main(String[] args) throws Exception {
        var x = new NoOsmosis();
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
