/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi

class DummyMap<K,V> implements Map {

    void clear() {
    }

    boolean containsKey(Object key) {
        false
    }

    boolean containsValue(Object value) {
        false
    }

    Set<Map.Entry<K,V>> entrySet() {
        new HashSet()
    }

    public V get(Object key) {
        null
    }

    boolean isEmpty() {
        true
    }

    public Set<K> keySet() {
        new HashSet()
    }

    public V put (K key, V value) {
        value
    }

    void putAll(Map<? extends K, ? extends V> m) {
    }

    public V remove(Object key) {
        null
    }

    int size() {
        0
    }

    Collection<V> values() {
        new ArrayList()
    }
}