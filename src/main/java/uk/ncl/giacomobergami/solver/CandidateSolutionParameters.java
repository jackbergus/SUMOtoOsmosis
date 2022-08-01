package uk.ncl.giacomobergami.solver;

import uk.ncl.giacomobergami.algorithmics.ClusterDifference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CandidateSolutionParameters {
    public Map<Double, LocalTimeOptimizationProblem.Solution> bestResult = null;
    public TreeMap<Double, Map<String, List<String>>> inStringTime = null;
    public HashMap<Double, Map<RSU, List<Vehicle>>> inCurrentTime = null;
    public HashMap<String, ConcretePair<ConcretePair<Double, List<String>>, List<ClusterDifference<String>>>> delta_associations = null;
}
