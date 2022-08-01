package uk.ncl.giacomobergami.algorithmics;

import uk.ncl.giacomobergami.solver.ConcretePair;
import uk.ncl.giacomobergami.solver.RSU;
import uk.ncl.giacomobergami.solver.Vehicle;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClusterDifference<T> implements Predicate<T> {

    @Override
    public boolean test(T t) {
        switch (change) {
            case UNCHANGED -> {
                return true;
            }
            case CHANGED -> {
                var test = changes.get(t);
                return ((test != typeOfChange.REMOVAL_OF));
            }
        }
        return false;
    }

    enum type {
        UNCHANGED,
        CHANGED
    }

    enum typeOfChange {
        REMOVAL_OF,
        ADDITION_OF
    }

    public final ClusterDifference.type change;
    public final Map<T, typeOfChange> changes;

    public ClusterDifference() {
        change = type.UNCHANGED;
        changes = new HashMap<>();
    }

    @Override
    public String toString() {
        return "ClusterDifference{" +
                "change=" + change +
                ", changes=" + changes +
                '}';
    }

    public ClusterDifference(type change, Map<T, typeOfChange> changes) {
        this.change = change;
        this.changes = changes;
    }

    public List<T> reconstructFrom(List<T> ls, Comparator<T> cmp) {
        if (change == type.CHANGED) {
            if (ls == null) ls = Collections.emptyList();
            var tmp = ls.stream().filter(this).collect(Collectors.toList());
            for (var x : changes.entrySet()) {
                if (x.getValue() == typeOfChange.REMOVAL_OF) continue;
                tmp.add(x.getKey());
            }
            tmp.sort(cmp);
            return tmp;
        }
        return ls;
    }

    public static <T> ClusterDifference<T> listOfChanges(List<T> ls, List<T> rs, Comparator<T> no) {
        if (ls == null)
            ls = Collections.emptyList();
        else
            ls.sort(no);
        if (rs == null)
            rs = Collections.emptyList();
        else
            rs.sort(no);
        Map<T, typeOfChange> change = new HashMap<>();
        int i = 0, j = 0;
        int N = ls.size(), M = rs.size();
        while ((i<N) && (j<M)) {
            var cost =
                    no.compare(ls.get(i), rs.get(j));
            if (cost == 0) {
                i++; j++;
            } else if (cost < 0) {
                change.put(ls.get(i++), typeOfChange.REMOVAL_OF);
            } else {
                change.put(rs.get(j++), typeOfChange.ADDITION_OF);
            }
        }
        while (i<N) {
            change.put(ls.get(i++), typeOfChange.REMOVAL_OF);
        }
        while (j<M) {
            change.put(rs.get(j++), typeOfChange.ADDITION_OF);
        }
        return change.isEmpty() ? new ClusterDifference<T>() : new ClusterDifference<T>(type.CHANGED, change);
    }

    public static <H, K, T> ConcretePair<ConcretePair<H, List<T>>, List<ClusterDifference<T>>>
    diff(Map<H, HashMap<K, List<T>>> toDiff,
         K holder,
         Comparator<T>tmp) {
        boolean first = true;
        List<T> prevLs = null;
        ConcretePair<H, List<T>> cp = null;
        List<ClusterDifference<T>> lsDiff = new ArrayList<>();
        for (var x : toDiff.entrySet()) {
            if (first) {
                prevLs = x.getValue().get(holder);
                cp = new ConcretePair<>(x.getKey(), prevLs);
                first = false;
            } else {
                List<T> currLs = x.getValue().get(holder);
                lsDiff.add(listOfChanges(prevLs, currLs, tmp));
                prevLs = currLs;
            }
        }
        return new ConcretePair<>(cp, lsDiff);
    }

    public static <H, K, T> HashMap<K, ConcretePair<ConcretePair<H, List<T>>, List<ClusterDifference<T>>>>
    diff(Map<H, HashMap<K, List<T>>> toDiff,
         Collection<K> holder,
         Comparator<T>tmp) {
        HashMap<K, ConcretePair<ConcretePair<H, List<T>>, List<ClusterDifference<T>>>> res = new HashMap<>();
        for (var h : holder) {
            res.put(h, diff(toDiff, h, tmp));
        }return res
                ;
    }


    public static <H, T> List<List<T>> reconstruct(ConcretePair<ConcretePair<H, List<T>>,
                                                                         List<ClusterDifference<T>>> reconstruction,
                                                                         Comparator<T> cmp) {
        List<List<T>> result = new ArrayList<>(reconstruction.getRight().size()+1);
        result.add(reconstruction.getLeft().getValue());
        for (int i = 0, N = reconstruction.getRight().size(); i<N; i++) {
            result.add(reconstruction.getRight().get(i).reconstructFrom(result.get(result.size()-1), cmp));
        }
        return result;
    }

    public static <H, K, T> void test(Map<H, HashMap<K, List<T>>> toDiff,
                                                            List<K> holderList,
                                                            Comparator<T> cmp) {
        for (K k : holderList) {
            var res = diff(toDiff, k, cmp);
            var backup = reconstruct(res, cmp);
            int i = 0;
            for (var x : toDiff.entrySet()) {
                var obj = backup.get(i++);
                if (obj == null) obj = Collections.emptyList();
                var LS = x.getValue().getOrDefault(k, Collections.emptyList());
                if (!LS.containsAll(obj))
                    System.err.println(LS+" vs1" +obj);
                if (!obj.containsAll(LS))
                    System.err.println(LS+" vs2" +obj);
            }
        }
    }
}
