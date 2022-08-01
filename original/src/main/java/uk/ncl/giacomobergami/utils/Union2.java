package uk.ncl.giacomobergami.utils;

public class Union2<K,V> {
    K val1;
    V val2;
    boolean isFirst;

    public boolean isFirst() {
        return isFirst;
    }
    public K getVal1() {
        return val1;
    }
    public V getVal2() {
        return val2;
    }

    public static <K, V> Union2<K, V> left(K left) {
        Union2<K, V> var = new Union2<>();
        var.val1 = left;
        var.val2 = null;
        var.isFirst = true;
        return var;
    }

    public static <K, V> Union2<K, V> right(V right) {
        Union2<K, V> var = new Union2<>();
        var.val1 = null;
        var.val2 = right;
        var.isFirst = false;
        return var;
    }

    private Union2() {}
}
