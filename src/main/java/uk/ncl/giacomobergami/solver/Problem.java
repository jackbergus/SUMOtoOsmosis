package uk.ncl.giacomobergami.solver;

import com.eatthepath.jvptree.VPTree;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.ortools.sat.*;
import io.jenetics.ext.moea.Pareto;
import io.jenetics.ext.moea.ParetoFront;
import it.unimi.dsi.fastutil.doubles.DoubleComparator;
import it.unimi.dsi.fastutil.doubles.DoubleComparators;
import org.cloudbus.cloudsim.edge.core.edge.ConfiguationEntity;
import uk.ncl.giacomobergami.utils.CartesianDistanceFunction;
import uk.ncl.giacomobergami.utils.SimulatorConf;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Problem {
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
//    CpModel model;

    public Problem(List<Vehicle> vehicles,
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
//        model = new CpModel();
    }

    protected Set<Map<Vehicle, RSU>> allThePossibleAssociations(Map<Vehicle, List<RSU>> lists) {
        Set<Map<Vehicle, RSU>> resultLists = new HashSet<>();
        if (lists.size() == 0) {
            resultLists.add(Collections.emptyMap());
            return resultLists;
        } else {
            Vehicle current = lists.keySet().iterator().next();
            var firstList = lists.remove(current);
            if (firstList.isEmpty()) {
                return allThePossibleAssociations(lists);
            } else {
                Set<Map<Vehicle, RSU>> remainingLists = allThePossibleAssociations(lists);
                for (RSU condition : firstList) {
                    for (Map<Vehicle, RSU> remainingList : remainingLists) {
                        var resultList = new HashMap<>(remainingList);
                        resultList.put(current, condition);
                        resultLists.add(resultList);
                    }
                }
            }
        }
        return resultLists;
    }

    public void solveGoogle() {

    }

    public ArrayList<Solution> multi_objective_pareto(double k1, double k2, boolean reduceToOne) {
        return multi_objective_pareto(k1, k2, true, Pareto::dominance, reduceToOne);
    }

    public ArrayList<Solution> multi_objective_pareto(double k1, double k2, double pi1, double pi2, boolean reduceToOne) {
        return multi_objective_pareto(k1, k2, true, Comparator.comparingDouble(o -> o[0] * pi1 + o[1] * pi2 + o[2] * (1 - pi1 - pi2)), reduceToOne);
    }

    public ArrayList<Solution> multi_objective_pareto(double k1, double k2, boolean ignoreCubic, boolean reduceToOne) {
        return multi_objective_pareto(k1, k2, ignoreCubic, Pareto::dominance, reduceToOne);
    }

    public ArrayList<Solution> multi_objective_pareto(double k1, double k2, boolean ignoreCubic, double pi1, double pi2, boolean reduceToOne) {
        return multi_objective_pareto(k1, k2, ignoreCubic, Comparator.comparingDouble(o -> o[0] * pi1 + o[1] * pi2 + o[2] * (1 - pi1 - pi2)), reduceToOne);
    }

    public void googleORSolver() {
        int numVehicles = 1;
        int numFirstMile = 1;
        int numLastMile = 1;

        CpModel model = new CpModel();
        Literal[][][] shifts = new Literal[numVehicles][numFirstMile][numLastMile];
        for (int veh = 0; veh<numVehicles; veh++) {
            Multimap<Integer, Literal> multimap = HashMultimap.create();
            for (int first = 0; first<numFirstMile; first++) {
                // At most one last mile association per first mile association
                List<Literal> c1 = new ArrayList<>();
                for (int last = 0; last<numLastMile; last++) {
                    shifts[veh][first][last] = model.newBoolVar("veh" + veh + "from" + first + "to" + last);
                    multimap.put(last, shifts[veh][first][last]);
                }
                model.addAtMostOne(c1);
            }
            // At most one first mile association per last mile association
            multimap.asMap().forEach((key, value) -> model.addAtMostOne(value));
        }


    }

    public class Solution {
        double[] obj;
        Map<Vehicle, RSU> firstMileCommunication;
        Map<Vehicle, RSU> alphaAssociation;

        public Set<Map.Entry<Vehicle, RSU>> getAlphaAssociation() {
            return alphaAssociation.entrySet();
        }

        public Solution(double[] obj,
                        Map<Vehicle, RSU> firstMileCommunication,
                        Map<Vehicle, RSU> alphaAssociation) {
            this.obj = obj;
            this.firstMileCommunication = firstMileCommunication;
            this.alphaAssociation = alphaAssociation;
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
        public double[] updateWithComputeRanking(double k1,
                                                 double k2,
                                                 boolean ignoreCubic,
                                                 ConcretePair<Map<Vehicle, RSU>, Map<Vehicle, RSU>> pair) {
            obj = computeRanking(k1, k2, ignoreCubic, new ConcretePair<>(firstMileCommunication, alphaAssociation));
            return obj;
        }
    }

    /**
     * Returning all of the candidate belonging to the pareto solution
     * @return
     */
    private ArrayList<Solution> multi_objective_pareto(double k1,
                                                       double k2,
                                                       boolean ignoreCubic,
                                                       Comparator<double[]> dominance,
                                                       boolean reduceToOne) {
        long startTime = System.currentTimeMillis();
        final ArrayList<Solution> solution = new ArrayList<>();
        final ArrayList<double[]> all = new ArrayList<>();
        final ArrayList<ConcretePair<Map<Vehicle, RSU>, Map<Vehicle, RSU>>> allPossiblePairs = new ArrayList<>();

        for (Map<Vehicle, RSU> firstCommunication : this.firstMileCommunication) {
            if (firstCommunication.isEmpty()) continue;
            for (Map<Vehicle, RSU> alpha : this.targetCommunication) {
                if (alpha.isEmpty()) continue;
                allPossiblePairs.add(new ConcretePair<>(firstCommunication, alpha));
            }
        }

        // If there is only one solution to test, that is locally optimal by definition
        if (allPossiblePairs.size() == 1) {
            all.add(new double[]{0,0,0});
            solution.add(new Solution(new double[]{0,0,0}, allPossiblePairs.get(0).getKey(), allPossiblePairs.get(0).getValue()));
        } else {
            for (int i = 0; i < allPossiblePairs.size(); i++) {
                if (i % 1000 == 0) System.out.print(i+"... ");
                System.out.flush();
                all.add(computeRanking(k1, k2, ignoreCubic, allPossiblePairs.get(i)));
            }
            final ParetoFront<double[]> front = new ParetoFront<>(dominance);
            System.out.println("\nParetoing...\n");
            front.addAll(all);
            double[] prev = new double[]{Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE};
            for (int i = 0, N = all.size(); i<N; i++) {
                var v = all.get(i);
                if (front.contains(v)) {
                    var cp = allPossiblePairs.get(i);
                    if (solution.isEmpty() || (!reduceToOne))
                        solution.add(new Solution(v, cp.getKey(), cp.getValue()));
                    else if (prev[0] >= v[0] && prev[1] >= v[1] && prev[2] >= v[2]) {
                        solution.set(0, new Solution(v, cp.getKey(), cp.getValue()));
                        prev = v;
                    }
                }
            }
        }
        System.out.println("Solution found: " + solution.size() + " over " + all.size());
        run_time += (System.currentTimeMillis() - startTime);
        return solution;
    }

    private double[] computeRanking(double k1,
                                    double k2,
                                    boolean ignoreCubic,
                                    ConcretePair<Map<Vehicle, RSU>, Map<Vehicle, RSU>> pair) {
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

        // Making all of the rsus as nodes of the graph, as we can distribute the load
        // within the network
        for (var rsu : this.rsus) {
            rsus.put(rsu, counter.getAndIncrement());
        }

        for (var assoc : firstCommunication.entrySet()) {
            // Adding the communicating IoT nodes to the graph if required
            vehs.computeIfAbsent(assoc.getKey(), vehicle -> counter.getAndIncrement());
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

        obj_network = flow.getMaxFlow(capacity, cost, initialSource, finalTarget).total_cost;
        return new double[]{obj_IoT, obj_mel, obj_network};
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
         firstMileCommunication = allThePossibleAssociations(vehicles_communicating_with_nearest_RSUs.entrySet().stream().filter(e ->!e.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().stream().min(Comparator.comparingDouble(o -> f.getDistance(x.getKey(), o))).stream().toList()))).stream().toList();
         run_time += (System.currentTimeMillis() - startTime);
    }

    public void alwaysCommunicateWithTheNearestMel() {
        long startTime = System.currentTimeMillis();
        firstMileCommunication = allThePossibleAssociations(vehicles_communicating_with_nearest_RSUs.entrySet().stream().filter(e ->!e.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().stream().min(Comparator.comparingDouble(o -> f.getDistance(x.getKey(), o))).stream().toList()))).stream().toList();
        targetCommunication = new ArrayList<>(firstMileCommunication);
        run_time += (System.currentTimeMillis() - startTime);
    }

    public void nearestFurthestRandomMELForIoT() {
        long startTime = System.currentTimeMillis();
        firstMileCommunication = allThePossibleAssociations(vehicles_communicating_with_nearest_RSUs.entrySet().stream().filter(e ->!e.getValue().isEmpty()).collect(Collectors.toMap(Map.Entry::getKey, x -> rd.nextBoolean() ?
                x.getValue().stream().min(Comparator.comparingDouble(o -> f.getDistance(x.getKey(), o))).stream().toList():
                x.getValue().stream().max(Comparator.comparingDouble(o -> f.getDistance(x.getKey(), o))).stream().toList()))).stream().toList();
        run_time += (System.currentTimeMillis() - startTime);
    }

    /**
     * Given all of the possible MELs near to the vehicle, it considers all of them
     */
    public void setAllPossibleMELForIoT() {
        long startTime = System.currentTimeMillis();
        firstMileCommunication = allThePossibleAssociations(vehicles_communicating_with_nearest_RSUs.entrySet().stream()
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
        targetCommunication = allThePossibleAssociations(result).stream().toList();
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
                            double xDistX = (candidateRSU.x - sem.x),
                                    xDistY = (candidateRSU.y - sem.y);
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
            double xDistX = (candidateRankingAlternativeRSU.x - busyRSU.x),
                    xDistY = (candidateRankingAlternativeRSU.y - busyRSU.y);
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
        targetCommunication = allThePossibleAssociations(vehicles_communicating_with_nearest_RSUs.keySet().stream().filter(e -> !vehicles_communicating_with_nearest_RSUs.get(e).isEmpty()).collect(Collectors.<Vehicle, Vehicle, List<RSU>>toMap(e->e, e->List.copyOf(rsus)))).stream().toList();
        run_time += (System.currentTimeMillis() - start);
    }

    public void setAllPossibleNearestKTargetsForCommunication(int k, boolean randomOne) {
        long start = System.currentTimeMillis();
        targetCommunication = allThePossibleAssociations(vehicles_communicating_with_nearest_RSUs.keySet().stream().filter(e -> !vehicles_communicating_with_nearest_RSUs.get(e).isEmpty()).collect(Collectors.<Vehicle, Vehicle, List<RSU>>toMap(e->e, e-> {
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
                        for (int i = 0; i < rsus.size(); i++) {
                        if (vehicles_communicating_with_nearest_RSUs.get(e).contains(rsus.get(i))) {
                            var val = fun.apply(rsus.get(i));
                            var cmp = rsus.get(i).max_vehicle_communication * rsus.get(i).max_vehicle_communication;
                            throw new RuntimeException(rsus.get(i)+"");
                        }
                        if (fun.apply(rsus.get(i)) > rsus.get(i).max_vehicle_communication) continue;
                        if (i < k) // add until heap is filled with k elements.
                            pq.add(rsus.get(i));
                        else if (comparator.compare(pq.peek(), rsus.get(i)) < 0) { // check if it's bigger than the
                            // smallest element in the heap.
                            pq.poll();
                            pq.add(rsus.get(i));
                        }
                    }
                        throw new RuntimeException("ERROR!");
                    }
                }
                return new ArrayList<>(pq);
            }
        }))).stream().toList();
        run_time += (System.currentTimeMillis() - start);
    }

}
