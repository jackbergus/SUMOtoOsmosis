package uk.ncl.giacomobergami.solver;

import com.eatthepath.jvptree.VPTree;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.jenetics.ext.moea.Pareto;
import io.jenetics.ext.moea.ParetoFront;
import org.cloudbus.cloudsim.edge.core.edge.ConfiguationEntity;
import uk.ncl.giacomobergami.algorithmics.CartesianProduct;
import uk.ncl.giacomobergami.utils.CartesianDistanceFunction;
import uk.ncl.giacomobergami.utils.SimulatorConf;
import uk.ncl.giacomobergami.utils.Union2;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LocalTimeOptimizationProblem {
    public List<Vehicle> vehicles;
    public List<RSU>     rsus;
    private final SimulatorConf conf;
    CartesianDistanceFunction f;
    List<Map<Vehicle, RSU>> firstMileCommunication;
    List<Map<Vehicle, RSU>> targetCommunication;
    MinCostMaxFlow flow;
    Map<Vehicle, ArrayList<RSU>> vehicles_communicating_with_nearest_RSUs;
    Random rd;
    long run_time;

    public LocalTimeOptimizationProblem(List<Vehicle> vehicles,
                                        List<RSU> rsus,
                                        final SimulatorConf conf) {
        this.vehicles = vehicles;
        this.rsus = rsus;
        this.conf = conf;
        f = new CartesianDistanceFunction();
        firstMileCommunication = Collections.emptyList();
        targetCommunication = Collections.emptyList();
        flow = new MinCostMaxFlow();
        rd = new Random();
    }

    public class Solution {
        double[] obj;
        Map<Vehicle, RSU> firstMileCommunication;
        Map<Vehicle, RSU> alphaAssociation;
        private final Map<Vehicle, List<Union2<Vehicle, RSU>>> vehicularPaths;
        public final Map<RSU, List<RSU>> RSUNetworkNeighbours;
        public HashMap<RSU, List<Vehicle>> rsuToCommunicatingVehiclesCluster;

        public Solution(IntermediateSolution v, ConcretePair<Map<Vehicle, RSU>, Map<Vehicle, RSU>> cp) {
            this(v.objectives, cp.getKey(), cp.getValue(), v.communicationPaths, v.RSUNetworkNeighbours);
        }

        public Set<Map.Entry<Vehicle, RSU>> getAlphaAssociation() {
            return alphaAssociation.entrySet();
        }

        public Solution(double[] obj,
                        Map<Vehicle, RSU> firstMileCommunication,
                        Map<Vehicle, RSU> alphaAssociation,
                        Map<Vehicle, List<Union2<Vehicle, RSU>>> vehicularPaths,
                        Map<RSU, List<RSU>> RSUNetworkNeighbours) {
            this.obj = obj;
            this.firstMileCommunication = firstMileCommunication;
            this.alphaAssociation = alphaAssociation;
            this.vehicularPaths = vehicularPaths;
            this.RSUNetworkNeighbours = RSUNetworkNeighbours;
            this.rsuToCommunicatingVehiclesCluster = new HashMap<>();
        }

        public List<Union2<Vehicle, RSU>> slowRetrievePath(Vehicle x) {
            for (var tempt : vehicularPaths.entrySet()) {
                if (tempt.getKey().id.equals(x.id))
                    return tempt.getValue();
            }
            return null;
        }
        public RSU firstMileAssociationWith(Vehicle x) {
            return firstMileCommunication.get(x);
        }
        public RSU getAssociationWith(Vehicle x) {
            return alphaAssociation.get(x);
        }
        public double objectiveRanking(double pi1, double pi2) {
            return obj[0] * pi1 + obj[1] * pi2 + obj[2] * (1.0-pi1-pi2);
        }
        public double[] objectives() {
            return obj;
        }
        public double obj_IoT() {
            return obj[0];
        }
        public double obj_mel() {
            return obj[1];
        }
        public double obj_network() {
            return obj[2];
        }
    }

    /**
     * Returning all of the candidate belonging to the pareto solution
     * @return
     */
    public ArrayList<Solution> multi_objective_pareto(double k1,
                                                       double k2,
                                                       boolean ignoreCubic,
                                                       Comparator<double[]> dominance,
                                                       boolean reduceToOne,
                                                       boolean updateAfterFlow) {
        long startTime = System.currentTimeMillis();
        final ArrayList<Solution> solutionList = new ArrayList<>();
        final ArrayList<IntermediateSolution> all = new ArrayList<>();
        final ArrayList<ConcretePair<Map<Vehicle, RSU>, Map<Vehicle, RSU>>> allPossiblePairs = new ArrayList<>();

        for (Map<Vehicle, RSU> firstCommunication : this.firstMileCommunication) {
            if (firstCommunication.isEmpty()) continue;
            for (Map<Vehicle, RSU> alpha : this.targetCommunication) {
                if (alpha.isEmpty()) continue;
                allPossiblePairs.add(new ConcretePair<>(firstCommunication, alpha));
            }
        }

        for (int i = 0; i < allPossiblePairs.size(); i++) {
            if (i % 1000 == 0) System.out.print(i+"... ");
            System.out.flush();
            all.add(computeRanking(k1, k2, ignoreCubic, allPossiblePairs.get(i), updateAfterFlow));
        }
        final ParetoFront<double[]> front = new ParetoFront<>(dominance);
        System.out.println("\nParetoing...\n");
        all.forEach(x -> front.add(x.objectives));
        double[] prev = new double[]{Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE};
        for (int i = 0, N = all.size(); i<N; i++) {
            var v = all.get(i);
            if (front.contains(v.objectives)) {
                var cp = allPossiblePairs.get(i);
                if (solutionList.isEmpty() || (!reduceToOne))
                    solutionList.add(new Solution(v, cp));
                else if (dominance.compare(prev, v.objectives) >= 0) {
                    solutionList.set(0, new Solution(v, cp));
                    prev = v.objectives;
                }
            }
        }

        for (var potentialSolution : solutionList) {
            for (var vehicleToRSU : potentialSolution.getAlphaAssociation()) {
                if (!potentialSolution.rsuToCommunicatingVehiclesCluster.containsKey(vehicleToRSU.getValue()))
                    potentialSolution.rsuToCommunicatingVehiclesCluster.put(vehicleToRSU.getValue(), new ArrayList<>());
                potentialSolution.rsuToCommunicatingVehiclesCluster.get(vehicleToRSU.getValue()).add(vehicleToRSU.getKey());
            }
        }

        run_time += (System.currentTimeMillis() - startTime);
        System.out.println("Solution found: " + solutionList.size() + " over " + all.size());
        return solutionList;
    }

    private class IntermediateSolution {
        private final double[] objectives;
        private final Map<Vehicle, List<Union2<Vehicle, RSU>>> communicationPaths;
        private final Map<RSU, List<RSU>> RSUNetworkNeighbours;

        public IntermediateSolution(double[] objectives,
                                    Map<Vehicle, List<Union2<Vehicle, RSU>>> communicationPaths,
                                    Map<RSU, List<RSU>> RSUNetworkNeighbours) {
            this.objectives = objectives;
            this.communicationPaths = communicationPaths;
            this.RSUNetworkNeighbours = RSUNetworkNeighbours;
        }
    }

    private IntermediateSolution computeRanking(double k1,
                                                                                    double k2,
                                                                                    boolean ignoreCubic,
                                                                                    ConcretePair<Map<Vehicle, RSU>, Map<Vehicle, RSU>> pair,
                                                                                    boolean updateAfterRunning) {
        var firstCommunication = pair.getLeft();
        var alpha = pair.getRight();

        double obj_IoT = 0.0;
        double obj_mel = 0.0;
        double obj_network = 0.0;

        // Counting the total number of vertices
        AtomicInteger counter = new AtomicInteger(0);
        Integer initialSource = counter.getAndIncrement();
        Integer finalTarget = counter.getAndIncrement();

        // Calculating for each IoT device the minimization of their distances to the RSUs
        Map<RSU, Integer> auto = new HashMap<>();
        Map<Vehicle, Integer> vehs = new HashMap<>();
        Map<RSU, Integer> rsus = new HashMap<>();
        Map<Integer, Union2<Vehicle, RSU>> vehOrRSUPath = new HashMap<>();
        Map<String, Vehicle> vehNameToVeh = new HashMap<>();
        Map<String, RSU> rsuNameToRSU = new HashMap<>();
        Map<Vehicle, List<Union2<Vehicle, RSU>>> paths = new HashMap<>();
        Map<RSU, List<RSU>> RSUNetworkNeighbours = new HashMap<>();

        // Making all of the rsus as nodes of the graph, as we can distribute the load
        // within the network
        for (var rsu : this.rsus) {
            var id = counter.getAndIncrement();
            rsus.put(rsu, id);
            vehOrRSUPath.put(id, Union2.right(rsu));
            rsuNameToRSU.put(rsu.tl_id, rsu);
        }

        for (var assoc : firstCommunication.entrySet()) {
            // Adding the communicating IoT nodes to the graph if required
            var id = counter.get();
            vehs.computeIfAbsent(assoc.getKey(), vehicle -> id);
            vehOrRSUPath.put(id, Union2.left(assoc.getKey()));
            counter.getAndIncrement();
            vehNameToVeh.put(assoc.getKey().id, assoc.getKey());
        }

        int vertexSize = counter.get();
        int[][] capacity = new int[vertexSize][vertexSize];
        int[][] cost = new int[vertexSize][vertexSize];
//        HashMap<ConcretePair<Integer, Integer>, IntVar> edges = new HashMap<>();
        for (var rsu1 : this.rsus) {
            var sq1 = rsu1.communication_radius * rsu1.communication_radius;
            var r1 = rsus.get(rsu1);
            for (var rsu2 : this.rsus) {
                var r2 = rsus.get(rsu2);
                if (!Objects.equals(r1, r2)) {
                    var sq2 = rsu2.communication_radius * rsu2.communication_radius;
                    var d = f.getDistance(rsu1, rsu2);

                    // we can establish a link if and only if they are respectively within their communication radius
                    if (d <= Math.min(sq1, sq2)) {
                        if (!RSUNetworkNeighbours.containsKey(rsu1)) {
                            RSUNetworkNeighbours.put(rsu1, new ArrayList<>());
                        }
                        if (!RSUNetworkNeighbours.containsKey(rsu2)) {
                            RSUNetworkNeighbours.put(rsu2, new ArrayList<>());
                        }
                        RSUNetworkNeighbours.get(rsu1).add(rsu2);
                        RSUNetworkNeighbours.get(rsu2).add(rsu1);
                        // The communication capacity is capped at the minimum communicative threshold being shared
                        capacity[r1][r2] = capacity[r2][r1] = (int) Math.min(rsu1.max_vehicle_communication, rsu2.max_vehicle_communication);
                        // The communication cost is directly proportional to the nodes' distance
                        cost[r1][r2] = cost[r2][r1] = (int) Math.round(k1 * d + k2);
//                        IntVar r1r2 = model.newIntVar(0, capacity[r1][r2], "f"+r1+"t"+r2);
//                        edges.put(new ConcretePair<>(r1, r2), r1r2);
//                        edges.put(new ConcretePair<>(r2, r1), r1r2);
//                        model.addLessOrEqual(r1r2, cost[r1][r2]); // C1
                    } else {
                        capacity[r1][r2] = capacity[r2][r1] = 0;
                        cost[r1][r2] = cost[r2][r1] = 0;
                    }
                }
            }
        }

        for (var assoc : alpha.entrySet()) {
            // Computing the objective function, as minimizing the distance from the target node
            obj_IoT += f.getDistance(assoc.getKey(), assoc.getValue());
            // Counting the total number of devices that are communicating with the target node
            auto.compute(assoc.getValue(), (vehicle, integer) -> {
                if (integer == null) return 1;
                else return integer + 1;
            });
        }

        for (var assoc : firstCommunication.entrySet()) {
            // Adding the nodes to the graph if required
            var vehId = vehs.get(assoc.getKey());
            var rsuId = rsus.get(assoc.getValue());

            // The capacity from vehicle and rsu is just unitary
            capacity[vehId][rsuId] = 1;
            // The capacity from bogus source and vehicle id is also unitary
            capacity[initialSource][vehId] = 1;

            // The communication cost is proportional to the distance of the two nodes
            cost[vehId][rsuId] = (int) Math.round(k1 * f.getDistance(assoc.getKey(), assoc.getValue()) + k2);
            // No cost for starting from the bogus node
            cost[initialSource][vehId] = 0;
        }

        // Calculating for each RSU device the minimization of the occupancy
        for (var inv_assoc : auto.entrySet()) {
            // The capacity from the actual destination and the bogus one is the number of
            // target vehicles that want to communicate with it
            int ainvSize = inv_assoc.getValue();
            obj_mel += ignoreCubic ? (ainvSize) : (Math.pow(inv_assoc.getKey().max_vehicle_communication, -3.0) * Math.pow(ainvSize - inv_assoc.getKey().max_vehicle_communication, 3.0) + 1.0);

            // The capacity associated for reaching the final target shall be equal to how many nodes want to communicate with it
            var id = rsus.get(inv_assoc.getKey());
            capacity[id][finalTarget] = inv_assoc.getValue();
            // No cost for reaching the target bogus node
            cost[id][finalTarget] = 0;
        }

        var result = flow.getMaxFlow(capacity, cost, initialSource, finalTarget);
        for (var p : result.minedPaths) {
            var pp = p.stream().map(vehOrRSUPath::get).collect(Collectors.toList());
//            System.out.println(pp);
            var v = pp.get(0).getVal1();
            var expected = pair.value.get(v);
            var returned = pp.get(pp.size()-1).getVal2();
            if (!Objects.equals(expected, returned)) {
                if (updateAfterRunning) {
                    pair.value.put(v, returned);
                } else {
                    // Forcibly running shortest path, so to reconstruct the expected path.
                    flow.bellman_ford_moore(vehs.get(v));
                    var cp = new ConcretePair<>(vehs.get(v), rsus.get(returned));
                    p = flow.map.get(cp);
                    if (p == null) {
                        p = updatePathWithFeasibleOne(vehs, vehOrRSUPath, v, returned, p);
                    }
                    pp = p.stream().map(vehOrRSUPath::get).collect(Collectors.toList());
                }
            }
            paths.put(v, pp);
        }
        if (result.minedPaths.size() != pair.key.size()) {
            if (result.minedPaths.size() > pair.key.size()) {
                throw new RuntimeException("We are expecting the opposite, that the mined paths are less than the expected ones");
            }
            for (var v : pair.key.entrySet()) {
                if (paths.containsKey(v.getKey())) continue; // I am not re-computing the paths that were computed before
                flow.bellman_ford_moore(vehs.get(v.getKey()));
                var p = flow.map.get(new ConcretePair<>(vehs.get(v.getKey()), rsus.get(v.getValue())));
                if (p == null) {
                    p = updatePathWithFeasibleOne(vehs, vehOrRSUPath, v.getKey(), v.getValue(), p);
                }
                var pp = p.stream().map(vehOrRSUPath::get).collect(Collectors.toList());
                paths.put(v.getKey(), pp);
            }
            if ((paths.size() != pair.key.size())) {
                throw new RuntimeException("That should have fixed the problem! " + paths.size()+ " vs "+ pair.key.size());
            }
        }

        var it = RSUNetworkNeighbours.entrySet().iterator();
        while (it.hasNext()) {
            var x = it.next();
            x.setValue(Lists.newArrayList(Sets.newHashSet(x.getValue())));
        }
        obj_network = result.total_cost;
        return new IntermediateSolution(new double[]{obj_IoT, obj_mel, obj_network},
                                        paths,
                                        RSUNetworkNeighbours);
    }

    private List<Integer> updatePathWithFeasibleOne(Map<Vehicle, Integer> vehs,
                                                    Map<Integer, Union2<Vehicle, RSU>> vehOrRSUPath,
                                                    Vehicle currentVehicle,
                                                    RSU expectedRSU,
                                                    List<Integer> p) {
        // If the path is null, it means that despite the algorithm determined one node to be
        // the best candidate solution, this is unreachable to the current car! Therefore, I need
        // to get, among all of the possible minimum paths that there exist, the one which the target
        // is nearer to the expected node
        double candidateSize = Double.MAX_VALUE;
        double countCandidates = 0;
        for (var candidatePath : flow.map.entrySet()) {
            if (!candidatePath.getKey().key.equals(vehs.get(currentVehicle))) continue;
            double d = f.getDistance(vehOrRSUPath.get(candidatePath.getKey().value).getVal2(), expectedRSU);
            if (d < candidateSize) {
                candidateSize = d;
                p = candidatePath.getValue();
            }
            countCandidates ++;
        }
        if (countCandidates == 0) {
            throw new RuntimeException("ERROR: expected at least one feasable path from a vehicle to a RSU!");
        }
        return p;
    }

    public long getRunTime() {
        return run_time;
    }

    public boolean init(List<ConfiguationEntity.IotDeviceEntity> sumoIoTDevices,
                        List<ConfiguationEntity.VMEntity> allDestinations) {
        run_time = 0;
        long startTime = System.currentTimeMillis();
        if (sumoIoTDevices != null) sumoIoTDevices.clear();
        if (allDestinations != null) allDestinations.clear();
        vehicles_communicating_with_nearest_RSUs = new HashMap<>();
        for (Vehicle veh: vehicles ) {
            vehicles_communicating_with_nearest_RSUs.put(veh, new ArrayList<>());
        }
        if (vehicles.isEmpty()) return false;
        boolean hasSomeResult = false;
        var tree = new VPTree<>(f, vehicles);
        var visitedVehicles = new HashSet<>();
        for (RSU x : rsus) {
            var distanceQueryResult = tree.getAllWithinDistance(x, x.communication_radius * x.communication_radius);
            if (!distanceQueryResult.isEmpty()) {
                hasSomeResult = true;
                boolean hasInsertion = false;
                for (var veh : distanceQueryResult) {
                    vehicles_communicating_with_nearest_RSUs.get(veh).add(x);
                    if (visitedVehicles.add(veh.id)) {
                        hasInsertion = true;
                        if (sumoIoTDevices != null)
                            sumoIoTDevices.add(veh.asIoDevice(conf.bw,
                                conf.max_battery_capacity,
                                conf.battery_sensing_rate,
                                conf.battery_sending_rate,
                                conf.network_type,
                                conf.protocol));
                    }
                }
                if (hasInsertion && ((allDestinations != null)))
                    allDestinations.add(x.asVMEntity(conf.VM_pes, conf.VM_mips, conf.VM_ram, conf.VM_storage, conf.VM_bw, conf.VM_cloudletPolicy));
            }
        }
        long endTime = System.currentTimeMillis();
        run_time += (endTime-startTime);
        return hasSomeResult;
    }

    /**
     * Given all of the possible MELs near to the vehicle, it considers only the one nearest to him for starting the communication
     */
    public void setNearestMELForIoT() {
         long startTime = System.currentTimeMillis();
         firstMileCommunication = CartesianProduct.mapCartesianProduct(vehicles_communicating_with_nearest_RSUs.entrySet().stream().filter(e ->!e.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().stream().min(Comparator.comparingDouble(o -> f.getDistance(x.getKey(), o))).stream().toList()))).stream().toList();
         run_time += (System.currentTimeMillis() - startTime);
    }

    public void alwaysCommunicateWithTheNearestMel() {
        long startTime = System.currentTimeMillis();
        firstMileCommunication = CartesianProduct.mapCartesianProduct(vehicles_communicating_with_nearest_RSUs.entrySet().stream().filter(e ->!e.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().stream().min(Comparator.comparingDouble(o -> f.getDistance(x.getKey(), o))).stream().toList()))).stream().toList();
        targetCommunication = new ArrayList<>(firstMileCommunication);
        run_time += (System.currentTimeMillis() - startTime);
    }

    public void nearestFurthestRandomMELForIoT() {
        long startTime = System.currentTimeMillis();
        firstMileCommunication = CartesianProduct.mapCartesianProduct(vehicles_communicating_with_nearest_RSUs.entrySet().stream().filter(e ->!e.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, x -> rd.nextBoolean() ?
                x.getValue().stream().min(Comparator.comparingDouble(o -> f.getDistance(x.getKey(), o))).stream().toList():
                x.getValue().stream().max(Comparator.comparingDouble(o -> f.getDistance(x.getKey(), o))).stream().toList()))).stream().toList();
        run_time += (System.currentTimeMillis() - startTime);
    }

    /**
     * Given all of the possible MELs near to the vehicle, it considers all of them
     */
    public void setAllPossibleMELForIoT() {
        long startTime = System.currentTimeMillis();
        firstMileCommunication = CartesianProduct.mapCartesianProduct(vehicles_communicating_with_nearest_RSUs.entrySet().stream()
                .filter(e ->!e.getValue().isEmpty())
                .collect(Collectors.<Map.Entry<Vehicle, ArrayList<RSU>>,Vehicle,List<RSU>>toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())))).stream().toList();
        run_time += (System.currentTimeMillis() - startTime);
    }

    /**
     * Exploits a greedy algorithm for associating each IoT device a single MEL with which
     * establish a communication
     */
    public void setGreedyPossibleTargetsForIoT(boolean useLocalDemandForecast) {
        long startTime = System.currentTimeMillis();
        Map<Vehicle, List<RSU>> result = new HashMap<>();
        for (var map : firstMileCommunication) {
            HashMap<RSU, List<Vehicle>> stringListHashMap = new HashMap<>();
            for (var cp : map.entrySet()) {
                if (!stringListHashMap.containsKey(cp.getValue())) {
                    stringListHashMap.put(cp.getValue(), new ArrayList<>());
                }
                stringListHashMap.get(cp.getValue()).add(cp.getKey());
            }
            trafficGreedyHeuristic(stringListHashMap, rsus, result, useLocalDemandForecast);
        }
        targetCommunication = CartesianProduct.mapCartesianProduct(result).stream().toList();
        run_time += (System.currentTimeMillis() - startTime);
    }


    private void trafficGreedyHeuristic(HashMap<RSU, List<Vehicle>> stringListHashMap,
                                        List<RSU> semList,
                                        Map<Vehicle, List<RSU>> result,
                                        boolean useLocalDemandForecast
    ) {
        if (stringListHashMap == null) return;
        HashMap<RSU, List<Vehicle>> updated =  new HashMap<>();
        stringListHashMap.forEach((x,y)-> updated.put(x, new ArrayList<>(y)));

        Comparator<RSU> IntCmp = Comparator.comparingInt(z -> {
            var x = updated.get(z);
            return  (x == null) ? 0 : x.size();
        });

        // Sorting the semaphores by the most busy ones
        List<RSU> sortedRSUs = semList.stream()
                .sorted(IntCmp.reversed())
                .collect(Collectors.toList());

        // Collecting all of the RSUs that have an excess of total number of vehicles
        HashMap<RSU, Set<Vehicle>> busyRSUs = new HashMap<>();
        Set<Vehicle> vehsWithBusyRSUs = new HashSet<>(); // Vehicles that might fall off from an association
        Set<Vehicle> allVehs = new HashSet<>(); // Vehicles already associated to a semaphore
        int minForSem = Integer.MAX_VALUE, maxForSem = 0;
        for (var sem : sortedRSUs) {
            var vehs = updated.get(sem);
            int vehSize = vehs == null ? 0 : vehs.size();
            if (minForSem > vehSize) minForSem = vehSize;
            if (maxForSem < vehSize) maxForSem = vehSize;
            if ((vehs == null) || vehSize < sem.max_vehicle_communication) break;
            var LS = vehs.subList((int)sem.max_vehicle_communication-1, vehSize);
            vehsWithBusyRSUs.addAll(LS);
            busyRSUs.put(sem, new HashSet<>(LS));
            vehs.removeAll(LS);
            allVehs.addAll(vehs);
        }
        int averageOcccupacy = (int)Math.round(((double) minForSem)+((double) maxForSem)/2.0);

        // Removing the stray vehicles that are already associated to a good semaphore
        vehsWithBusyRSUs.removeAll(allVehs);
        busyRSUs.forEach((k,v) -> v.removeAll(allVehs));
        busyRSUs.entrySet().stream().filter(cp-> cp.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
                .forEach(busyRSUs::remove);

        // Vehicles to get re-allocated
        HashMap<RSU, Set<RSU>> distributorOf = new HashMap<>();
        HashMap<RSU, List<RSU>> candidatesForLoadBalancing = new HashMap<>();

        if (!vehsWithBusyRSUs.isEmpty()) {
            // Determining which RSUs will be affected by over-demands by busy semaphores
            for (RSU busyRSU : busyRSUs.keySet()) {
                // A valid distributor for a busyRSU is a semaphore which is not currently full
                List<RSU> distributor = semList.stream()
                        .filter(x -> {
                            var ls = updated.get(x);
                            return (!x.equals(busyRSU)) && ((ls == null ? 0 : ls.size()) < (int)busyRSU.max_vehicle_communication);
                        })
                        .collect(Collectors.toList());
                for (RSU cp2 : distributor) {
                    if (!distributorOf.containsKey(cp2))
                        distributorOf.put(cp2, new HashSet<>());
                    distributorOf.get(cp2).add(busyRSU);
                }
                candidatesForLoadBalancing.put(busyRSU, distributor);
            }

            for (var busyRSUToCandidates : candidatesForLoadBalancing.entrySet()) {
                var busyRSU = busyRSUToCandidates.getKey();
                var map = busyRSUToCandidates.getValue().stream()
                        // The more the candidate RSU are near to the busy one, the more the space left,
                        // and the less the requests that the semaphore might receive from the siblings,
                        // the better. Sorting the candidates accordingly.
                        .sorted(Comparator.comparingDouble((RSU candidateRSU) -> {
                            double demandForecast = 1.0;
                            if (useLocalDemandForecast) {
                                var distrOf = distributorOf.get(candidateRSU);
//                                demandForecast = Math.exp(1.0 / (((double) (distrOf == null ? 0 : distrOf.size()))/candidateRSU.max_vehicle_communication+1.0));
                                demandForecast = Math.pow(0.5, (distrOf == null ? 0 : distrOf.size()));
                            }
                            return  desirabilityScoreComparison(updated, busyRSU, candidateRSU) * demandForecast;
                        }))

                        // Last, splitting the semaphores into free and overloaded and within the desired communication radius
                        .collect(Collectors.groupingBy(candidateRSU -> {
                            boolean isFree = true;
                            if (useLocalDemandForecast) {
                                var distrOf = updated.get(candidateRSU);
                                isFree = (distrOf == null) || (distrOf.size() <= candidateRSU.max_vehicle_communication);
                            }
                            var ls = updated.get(candidateRSU);
                            var sem = busyRSUToCandidates.getKey();
                            double xDistX = (candidateRSU.tl_x - sem.tl_x),
                                    xDistY = (candidateRSU.tl_y - sem.tl_y);
                            double xDistSq = (xDistX*xDistX)+(xDistY*xDistY);
                            return ((ls == null ? 0 : ls.size()) < (useLocalDemandForecast ? candidateRSU.max_vehicle_communication : averageOcccupacy)) &&
                                    (xDistSq < (candidateRSU.communication_radius * candidateRSU.communication_radius)) &&
                                    isFree;
                        }));
                var locVehs = new ArrayList<>(busyRSUs.get(busyRSUToCandidates.getKey()));
                int i = 0, N = locVehs.size();

                Set<RSU> okTL = new HashSet<>(), stopIL = new HashSet<>();
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
                        updated.putIfAbsent(sem, new ArrayList<>());
                        if (updated.get(sem).size() > sem.max_vehicle_communication) {
                            stopIL.add(sem);
                        } else {
                            updated.get(sem).add(locVehs.get(i++));
                        }
                    }
                    okTL.removeAll(stopIL);
                }

                // Uniformly re-distribuiting the load among the other remaining semaphores
                while (i<N) {
                    for (var sem : stopIL) {
                        if (i>=N) break;
                        updated.putIfAbsent(sem, new ArrayList<>());
                        updated.get(sem).add(locVehs.get(i++));
                    }
                }
            }

            for (var ref : updated.entrySet()) {
                for (var veh : ref.getValue()) {
                    if (!result.containsKey(veh))
                        result.put(veh, new ArrayList<>());
                    result.get(veh).add(ref.getKey());
                }
            }
        } else {
            for (var ref : stringListHashMap.entrySet()) {
                for (var veh : ref.getValue()) {
                    if (!result.containsKey(veh))
                        result.put(veh, new ArrayList<>());
                    result.get(veh).add(ref.getKey());
                }
            }
        }
    }

    /**
     * Stating that desirability is directly proportional to the availability of novel IoT devices jointly with the
     * distance from the busy RSU, this ranks the alternative RSUs for communication accordingly
     *
     * @param MELToIoT
     * @param busyRSU
     * @param candidateRankingAlternativeRSU
     * @return
     */
    private double desirabilityScoreComparison(HashMap<RSU, List<Vehicle>> MELToIoT,
                                               RSU busyRSU,
                                               RSU candidateRankingAlternativeRSU) {
        var ls = MELToIoT.get(candidateRankingAlternativeRSU);
        double xAvail = candidateRankingAlternativeRSU.max_vehicle_communication - (ls == null ? 0 : ls.size());
        if (Math.abs(xAvail)>=Double.MIN_NORMAL) {
            double absAvail = Math.abs(xAvail);
            double xDistX = (candidateRankingAlternativeRSU.tl_x - busyRSU.tl_x),
                    xDistY = (candidateRankingAlternativeRSU.tl_y - busyRSU.tl_y);
            double xDistSq = (xDistX*xDistX)+(xDistY*xDistY);
            xDistSq = xDistSq / (xDistSq+1); // normalization
            return Math.signum(xAvail) * (absAvail / (absAvail + 1)) * xDistSq;
        } else {
            return 0.0;
        }
    }

    /**
     * Considers all of the possible MELs in the communication network as possible targets
     */
    public void setAllPossibleTargetsForCommunication() {
        long start = System.currentTimeMillis();
        targetCommunication = CartesianProduct.mapCartesianProduct(vehicles_communicating_with_nearest_RSUs.keySet().stream().filter(e -> !vehicles_communicating_with_nearest_RSUs.get(e).isEmpty()).collect(Collectors.<Vehicle, Vehicle, List<RSU>>toMap(e->e, e->List.copyOf(rsus)))).stream().toList();
        run_time += (System.currentTimeMillis() - start);
    }

    public void setAllPossibleNearestKTargetsForCommunication(int k, boolean randomOne) {
        long start = System.currentTimeMillis();
        targetCommunication = CartesianProduct.mapCartesianProduct(vehicles_communicating_with_nearest_RSUs.keySet().stream().filter(e -> !vehicles_communicating_with_nearest_RSUs.get(e).isEmpty()).collect(Collectors.<Vehicle, Vehicle, List<RSU>>toMap(e->e, e-> {
            Function<RSU, Double> fun = o -> f.getDistance(o, e);
            Comparator<RSU> comparator = Comparator.comparingDouble(fun::apply);
            PriorityQueue<RSU> pq = new PriorityQueue<>(k, comparator);
            int added = 0;
            for (int i = 0; i < rsus.size(); i++) {
                if (fun.apply(rsus.get(i)) > rsus.get(i).communication_radius * rsus.get(i).communication_radius) continue;
                if (added < k) // add until heap is filled with k elements.
                { pq.add(rsus.get(i)); added++; }
                else if (comparator.compare(pq.peek(), rsus.get(i)) < 0) { // check if it's bigger than the
                    // smallest element in the heap.
                    pq.poll();
                    pq.add(rsus.get(i));
                }
            }
            if (randomOne) {
                ArrayList<RSU> ls = new ArrayList<>();
                for (int i = 0; i<k && pq.size() > 1; i++)
                    pq.poll();
                if (!pq.isEmpty()) {
                    ls.add(pq.peek());
                }
                if (ls.size() == 0) {
                    throw new RuntimeException(vehicles_communicating_with_nearest_RSUs.get(e).size()+"");
                }
                return ls;
            } else {
                if (pq.isEmpty()) {
                    if (!vehicles_communicating_with_nearest_RSUs.get(e).isEmpty()) {
                        throw new RuntimeException("ERROR!");
                    }
                }
                return new ArrayList<>(pq);
            }
        }))).stream().toList();
        run_time += (System.currentTimeMillis() - start);
    }

}
