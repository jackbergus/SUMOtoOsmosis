package uk.ncl.giacomobergami.solver;

import org.apache.commons.lang3.tuple.Pair;

public class ConcretePair<K, V> extends Pair<K, V> {
    final K key; final V value;

    public ConcretePair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getLeft() {
        return key;
    }

    @Override
    public V getRight() {
        return value;
    }

    @Override
    public V setValue(V value) {
        return null;
    }
}
