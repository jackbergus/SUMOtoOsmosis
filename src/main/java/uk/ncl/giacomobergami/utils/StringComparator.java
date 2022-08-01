package uk.ncl.giacomobergami.utils;

import java.util.Comparator;
import java.util.Objects;

public class StringComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        if (Objects.equals(o1, o2)) return 0;
        else if (o1 == null) return -1;
        else if (o2 == null) return 1;
        else return o1.compareTo(o2);
    }

    private StringComparator() {}
    private static StringComparator instance = null;
    public static StringComparator getInstance() {
        if (instance == null) {
            instance = new StringComparator();
        }
        return instance;
    }
}
