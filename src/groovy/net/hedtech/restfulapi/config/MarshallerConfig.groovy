/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class MarshallerConfig {

    def marshaller
    int priority

    MarshallerConfig marshaller(Object marshaller) {
        this.marshaller = marshaller
        this
    }

    MarshallerConfig priority(int priority) {
        this.priority = priority
        this
    }
}