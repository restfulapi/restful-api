/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi

class DummyIterator<E> implements Iterator {

    boolean hasNext() {
        false
    }

    public E next() {
        null
    }

    void remove() {
    }
}