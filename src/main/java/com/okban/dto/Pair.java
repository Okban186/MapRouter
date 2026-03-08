package com.okban.dto;

public class Pair<K, V> {
    public K key;
    public V value;

    public Pair(K key, V value) {
        this.key = key;

        this.value = value;
    }

    public V getValue() {
        return value;
    }
}
