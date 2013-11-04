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

class DummyCollection<E> implements Collection {

    boolean add(E e) {
        false
    }

    boolean addAll(Collection<? extends E> c) {
        false
    }

    void clear() {
    }

    boolean contains(Object o) {
        false
    }

    boolean containsAll(Collection<?> c) {
        false
    }

    boolean isEmpty() {
        true
    }

    Iterator<E> iterator() {
        new DummyIterator()
    }

    boolean remove(Object o) {
        false
    }

    boolean removeAll(Collection<?> c) {
        false
    }

    boolean retainAll(Collection<?> c) {
        false
    }

    int size() {
        0
    }

    Object[] toArray() {
        []
    }

    public <T> T[] toArray(T[] a) {
        []
    }


}
