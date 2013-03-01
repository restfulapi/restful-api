/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

class MediaType {

    static final XML = 'application/xml'

    MediaType(String n, Map params = [:]) {
        name = n
        parameters.putAll(params)
    }

    String name
    Map parameters = [q:"1.0"]

    boolean equals(Object o) { o instanceof MediaType && name.equals(o.name) }
    int hashCode() { name.hashCode() }

    String toString() {
        "MediaType { name=$name,parameters=$parameters }"
    }

}