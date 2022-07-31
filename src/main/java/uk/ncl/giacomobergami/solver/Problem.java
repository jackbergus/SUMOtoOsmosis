package uk.ncl.giacomobergami.solver;

import com.eatthepath.jvptree.VPTree;
import io.jenetics.ext.moea.Pareto;
import io.jenetics.ext.moea.ParetoFront;
import org.apache.commons.lang3.tuple.Pair;
import org.cloudbus.cloudsim.edge.core.edge.ConfiguationEntity;
import uk.ncl.giacomobergami.utils.CartesianDistanceFunction;
import uk.ncl.giacomobergami.utils.SimulatorConf;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Problem {
    public List<Vehicle> vehicles;
    public List<RSU>     rsus;
    private final double currentSimulationTime;
    private final SimulatorConf conf;
    CartesianDistanceFunction f;
    List<Map<Vehicle, RSU>> firstMileCommunication;
    List<Map<Vehicle, RSU>> targetCommunication;
    MinCostMaxFlow flow;

    public Problem(List<Vehicle> vehicles,
                   List<RSU> rsus,
                   double currentSimulationTime,
                   final SimulatorConf conf) {
        this.vehicles = vehicles;
        this.rsus = rsus;
        this.currentSimulationTime = currentSimulationTime;
        this.conf = conf;
        f = new CartesianDistanceFunction();
        firstMileCommunication = Collections.emptyList();
        targetCommunication = Collections.emptyList();
        flow = new MinCostMaxFlow();
    }

    protected List<Map<Vehicle, RSU>> allThePossibleAssociations(Map<Vehicle, List<RSU>> lists) {
        List<Map<Vehicle, RSU>> resultLists = new ArrayList<>();
        if (lists.size() == 0) {
            resultLists.add(Collections.emptyMap());
            return resultLists;
        } else {
            Vehicle current = lists.keySet().iterator().next();
            var firstList = lists.remove(current);
            List<Map<Vehicle, RSU>> remainingLists = allThePossibleAssociations(lists);
            for (RSU condition : firstList) {
                for (Map<Vehicle, RSU> remainingList : remainingLists) {
                    var resultList = new HashMap<>(remainingList);
                    resultList.put(current, condition);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }

    public ArrayList<Pair<Map<Vehicle, RSU>, Map<Vehicle, RSU>>> multi_objective(double k1, double k2) {
        return multi_objective(k1, k2, true, Pareto::dominance);
    }

    public ArrayList<Pair<Map<Vehicle, RSU>, Map<Vehicle, RSU>>> multi_objective(double k1, double k2, double pi1, double pi2) {
        return multi_objective(k1, k2, true, Comparator.comparingDouble(o -> o[0] * pi1 + o[1] * pi2 + o[2] * (1 - pi1 - pi2)));
    }

    public ArrayList<Pair<Map<Vehicle, RSU>, Map<Vehicle, RSU>>> multi_objective(double k1, double k2, boolean ignoreCubic) {
        return multi_objective(k1, k2, ignoreCubic, Pareto::dominance);
    }

    public ArrayList<Pair<Map<Vehicle, RSU>, Map<Vehicle, RSU>>> multi_objective(double k1, double k2, boolean ignoreCubic, double pi1, double pi2) {
        return multi_objective(k1, k2, ignoreCubic, Comparator.comparingDouble(o -> o[0] * pi1 + o[1] * pi2 + o[2] * (1 - pi1 - pi2)));
    }

    /**
     * Returning all of the candidate belonging to the pareto solution
     * @return
     */
    private ArrayList<Pair<Map<Vehicle, RSU>, Map<Vehicle, RSU>>> multi_objective(double k1, double k2, boolean ignoreCubic, Comparator<double[]> dominance) {
        final ArrayList<Pair<Map<Vehicle, RSU>, Map<Vehicle, RSU>>> solution = new ArrayList<>();
        final ArrayList<double[]> all = new ArrayList<>();
        final ArrayList<Pair<Map<Vehicle, RSU>, Map<Vehicle, RSU>>> allPossiblePairs = new ArrayList<>();


        for (Map<Vehicle, RSU> firstCommunication : this.firstMileCommunication) {
            if (firstCommunication.isEmpty()) continue;
            for (Map<Vehicle, RSU> alpha : this.targetCommunication) {
                if (alpha.isEmpty()) continue;
                allPossiblePairs.add(new Pair<>() {
                    @Override
                    public Map<Vehicle, RSU> getLeft() {
                        return firstCommunication;
                    }
                    @Override
                    public Map<Vehicle, RSU> getRight() {
                        return alpha;
                    }
                    @Override
                    public Map<Vehicle, RSU> setValue(Map<Vehicle, RSU> value) {
                        return null;
                    }
                });
            }
        }

        for (var pair : allPossiblePairs) {
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

            for (var assoc : firstCommunication.entrySet()){
                // Adding the communicating IoT nodes to the graph if required
                vehs.computeIfAbsent(assoc.getKey(), vehicle -> counter.getAndIncrement());
            }

            Integer vertexSize = counter.get();
            int[][] capacity   = new int[vertexSize][vertexSize];
            int[][] cost       = new int[vertexSize][vertexSize];
            for (var rsu1 : this.rsus) {
                var sq1 = rsu1.communication_radius * rsu1.communication_radius;
                var r1 = rsus.get(rsu1);
                for (var rsu2 : this.rsus) {
                    var r2 = rsus.get(rsu2);
                    if (r1 != r2) {
                        var sq2 = rsu2.communication_radius * rsu2.communication_radius;
                        var d = f.getDistance(rsu1, rsu2);

                        // we can establish a link if and only if they are respectively within their communication radius
                        if (d <= Math.min(sq1, sq2)) {
                            // The communication capacity is capped at the minimum communicative threshold being shared
                            capacity[r1][r2] = capacity[r2][r1] = (int)Math.min(rsu1.max_vehicle_communication, rsu2.max_vehicle_communication);
                            // The communication cost is directly proportional to the nodes' distance
                            cost[r1][r2] = cost[r2][r1] = (int)Math.round(k1 * d + k2);
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
                    else return integer+1;
                });
            }

            for (var assoc : firstCommunication.entrySet()){
                // Adding the nodes to the graph if required
                var vehId = vehs.get(assoc.getKey());
                var rsuId = rsus.get(assoc.getValue());

                // The capacity from vehicle and rsu is just unitary
                capacity[vehId][rsuId] = 1;
                // The capacity from bogus source and vehicle id is also unitary
                capacity[initialSource][vehId] = 1;

                // The communication cost is proportional to the distance of the two nodes
                cost[vehId][rsuId] = (int)Math.round(k1 * f.getDistance(assoc.getKey(), assoc.getValue()) + k2);
                // No cost for starting from the bogus node
                cost[initialSource][vehId] = 0;
            }

            // Calculating for each RSU device the minimization of the occupancy
            for (var inv_assoc : auto.entrySet()) {
                // The capacity from the actual destination and the bogus one is the number of
                // target vehicles that want to communicate with it
                int ainvSize = inv_assoc.getValue();
                obj_mel += ignoreCubic ? (ainvSize) : (Math.pow(inv_assoc.getKey().max_vehicle_communication, -3.0) * Math.pow(ainvSize - inv_assoc.getKey().max_vehicle_communication, 3.0)+1.0);

                // The capacity associated for reaching the final target shall be equal to how many nodes want to communicate with it
                var id = rsus.get(inv_assoc.getKey());
                capacity[id][finalTarget] = inv_assoc.getValue();
                // No cost for reaching the target bogus node
                cost[id][finalTarget] = 0;
            }

            obj_network = flow.getMaxFlow(capacity, cost, initialSource, finalTarget).total_cost;

            all.add(new double[]{obj_IoT, obj_mel, obj_network});
        }
//        Comparator<double[]> dominance = Pareto::dominance;

        final ParetoFront<double[]> front = new ParetoFront<>(dominance);
        front.addAll(all);
        for (int i = 0, N = all.size(); i<N; i++) {
            if (front.contains(all.get(i))) {
                solution.add(allPossiblePairs.get(i));
            }
        }
        return solution;
    }

    public void init(List<ConfiguationEntity.IotDeviceEntity> sumoIoTDevices,
                    List<ConfiguationEntity.VMEntity> allDestinations) {
        if (sumoIoTDevices != null) sumoIoTDevices.clear();
        if (allDestinations != null) allDestinations.clear();
        Map<Vehicle, ArrayList<RSU>> vehicles_communicating_with_nearest_RSUs;
        vehicles_communicating_with_nearest_RSUs = new HashMap<>();
        for (Vehicle veh: vehicles ) {
            vehicles_communicating_with_nearest_RSUs.put(veh, new ArrayList<>());
        }
        if (vehicles.isEmpty()) return; // TODO
        var tree = new VPTree<>(f, vehicles);
        var visitedVehicles = new HashSet<>();
        for (RSU x : rsus) {
            var distanceQueryResult = tree.getAllWithinDistance(x, x.communication_radius * x.communication_radius);
            if (!distanceQueryResult.isEmpty()) {
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
        firstMileCommunication = allThePossibleAssociations(vehicles_communicating_with_nearest_RSUs.entrySet().stream()
                .collect(Collectors.<Map.Entry<Vehicle, ArrayList<RSU>>,Vehicle,List<RSU>>toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue()))));
        targetCommunication = allThePossibleAssociations(vehicles_communicating_with_nearest_RSUs.keySet().stream().collect(Collectors.<Vehicle, Vehicle, List<RSU>>toMap(e->e, e->List.copyOf(rsus))));
    }
}
