/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class MarshallerGroupConfig {

    String name
    def marshallers = []

    MarshallerGroupConfig setName( String name ) {
        this.name = name
        this
    }

    MarshallerGroupConfig addMarshaller( Closure c ) {
        MarshallerConfig config = new MarshallerConfig()
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        marshallers.add config
        this
    }

}