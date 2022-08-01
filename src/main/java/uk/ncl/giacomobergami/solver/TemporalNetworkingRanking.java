package uk.ncl.giacomobergami.solver;

import uk.ncl.giacomobergami.algorithmics.CartesianProduct;
import uk.ncl.giacomobergami.algorithmics.ClusterDifference;
import uk.ncl.giacomobergami.utils.StringComparator;

import java.util.*;

public class TemporalNetworkingRanking {

    /**
     * This implementation assumes that the orchestrating agent is an oracle which can forecast in the future all of the
     * possible moves of the vehicles.
     *
     * Under this unrealistic assumption, we can always ensure that we are able to compute the best routing rules so far
     *
     * @param simulationSolutions
     * @param veh_s
     * @param bestResultScore
     * @param candidate
     * @param removal
     * @param addition
     */
    public static void oracularBestNetworking(TreeMap<Double, ArrayList<LocalTimeOptimizationProblem.Solution>> simulationSolutions,
                                              List<String> veh_s,
                                              Double bestResultScore,
                                              CandidateSolutionParameters candidate,
                                              double removal,
                                              double addition,
                                              Comparator<double[]> dominance) {
        // Geneating all of the possible combinations of network configurations
        var allThePossibleSolutions = CartesianProduct.mapCartesianProduct(simulationSolutions);
        simulationSolutions = null;

        System.out.println("Valuating oracular candidate solutions for ranking: ");
        int i = 0;
        for (Map<Double, LocalTimeOptimizationProblem.Solution> candidateSolution : allThePossibleSolutions) {
            if ((i % 1000) == 0) {
                System.out.print(i+"... ");
                System.out.flush();
            }
            bestResultScore = assessingCandidate(veh_s, bestResultScore, candidate, removal, addition, i, candidateSolution);
            i++;
        }
    }

    private static Double assessingCandidate(List<String> veh_s,
                                             Double bestResultScore,
                                             CandidateSolutionParameters candidate,
                                             double removal,
                                             double addition,
                                             int i,
                                             Map<Double, LocalTimeOptimizationProblem.Solution> candidateSolution) {
        HashMap<Double, Map<RSU, List<Vehicle>>> local_inCurrentTime = new HashMap<>();
        TreeMap<Double, Map<String, List<String>>> local_inStringTime = new TreeMap<>();
        TreeMap<Double, Map<String, List<String>>> vehClustAssoc = new TreeMap<>();

        for (var timeToSolution : candidateSolution.entrySet()) {
            HashMap<String, List<String>> map = new HashMap<>(), map2 = new HashMap<>();
            var currTime = timeToSolution.getKey();
            local_inCurrentTime.put(currTime, timeToSolution.getValue().rsuToCommunicatingVehiclesCluster);
            local_inStringTime.put(currTime, map);
            vehClustAssoc.put(currTime, map2);

            for (var y : timeToSolution.getValue().rsuToCommunicatingVehiclesCluster.entrySet()) {
                List<String> ls = new ArrayList<>();
                map.put(y.getKey().tl_id, ls);
                for (var z : y.getValue()) {
                    ls.add(z.id);
                    if (!map2.containsKey(z.id)) {
                        map2.put(z.id, new ArrayList<>());
                    }
                    map2.get(z.id).add(y.getKey().tl_id);
                }
            }
        }

        // getting the simulation program associated to each vehicle
        HashMap<String, ConcretePair<ConcretePair<Double, List<String>>, List<ClusterDifference<String>>>> local_delta_associations = ClusterDifference.diff(vehClustAssoc, veh_s, StringComparator.getInstance());

        double totalChangePerVehicle = 0.0;
        // Among these, we prefer a solution minimizing the size of the deta_associations, for all of the vehicles
        for (var eachDelta : local_delta_associations.entrySet()) {
            totalChangePerVehicle += ClusterDifference.computeCumulativeChange(eachDelta.getValue().getValue(), removal, addition);
        }

        if (totalChangePerVehicle < bestResultScore) {
            bestResultScore = totalChangePerVehicle;
            candidate.bestResult = candidateSolution;
            candidate.delta_associations = local_delta_associations;
            candidate.inStringTime = local_inStringTime;
            candidate.inCurrentTime = local_inCurrentTime;
            System.out.print("[New best candidate: "+ i +"]");
        }
        return bestResultScore;
    }

    public static LocalTimeOptimizationProblem.Solution returnCandidate(Collection<LocalTimeOptimizationProblem.Solution> ls,
                                                                 Comparator<double[]> dominance) {
        return ls.stream().min((o1, o2) -> dominance.compare(o1.obj, o2.obj)).orElseThrow();
    }

    public static void nonclairvoyantBestNetworking(TreeMap<Double, ArrayList<LocalTimeOptimizationProblem.Solution>> simulationSolutions,
                                              List<String> veh_s,
                                              Double bestResultScore,
                                              CandidateSolutionParameters candidate,
                                              double removal,
                                              double addition,
                                              Comparator<double[]> dominance) {
        Map<Double, LocalTimeOptimizationProblem.Solution> resultMap = new TreeMap<>();
        LocalTimeOptimizationProblem.Solution prev = null;
        Double prevSimTime = null;
        for (var x : simulationSolutions.entrySet()) {
            if (prev == null) {
                prev = returnCandidate(x.getValue(), dominance);
                prevSimTime = x.getKey();
                resultMap.put(x.getKey(), prev);
            } else {
                Double bestScore = Double.MAX_VALUE;
                LocalTimeOptimizationProblem.Solution nextPreferredSolution = null;
                for (var localCandidates : x.getValue()) {
                    TreeMap<Double, Map<String, List<String>>> vehClustAssoc = new TreeMap<>();
                    fillFromSimulationTime(vehClustAssoc, prev, prevSimTime);
                    fillFromSimulationTime(vehClustAssoc, localCandidates, x.getKey());

                    // getting the simulation program associated to each vehicle
                    HashMap<String, ConcretePair<ConcretePair<Double, List<String>>, List<ClusterDifference<String>>>> local_delta_associations = ClusterDifference.diff(vehClustAssoc, veh_s, StringComparator.getInstance());

                    double totalChangePerVehicle = 0.0;
                    // Among these, we prefer a solution minimizing the size of the deta_associations, for all of the vehicles
                    for (var eachDelta : local_delta_associations.entrySet()) {
                        totalChangePerVehicle += ClusterDifference.computeCumulativeChange(eachDelta.getValue().getValue(), removal, addition);
                    }

                    if (totalChangePerVehicle < bestScore) {
                        bestScore = totalChangePerVehicle;
                        nextPreferredSolution = localCandidates;
                    }
                }
                resultMap.put(x.getKey(), nextPreferredSolution);
            }
        }
        bestResultScore = assessingCandidate(veh_s, bestResultScore, candidate, removal, addition, 0, resultMap);
    }

    private static void fillFromSimulationTime(TreeMap<Double, Map<String, List<String>>> vehClustAssoc, LocalTimeOptimizationProblem.Solution curr, Double time) {
        HashMap<String, List<String>> map2 = new HashMap<>();
        vehClustAssoc.put(time, map2);
        for (var y : curr.rsuToCommunicatingVehiclesCluster.entrySet()) {
            for (var z : y.getValue()) {
                if (!map2.containsKey(z.id)) {
                    map2.put(z.id, new ArrayList<>());
                }
                map2.get(z.id).add(y.getKey().tl_id);
            }
        }
    }
}
