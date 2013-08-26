/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
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