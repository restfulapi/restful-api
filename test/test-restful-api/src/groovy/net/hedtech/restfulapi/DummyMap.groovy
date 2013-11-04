/* ****************************************************************************
 * Copyright 2013 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
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
