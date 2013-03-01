/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class MarshallerConfig {

    private GrailsApplication grailsApplication

    def marshaller
    int priority

    MarshallerConfig( GrailsApplication grailsApplication ) {
        this.grailsApplication = grailsApplication
    }

    MarshallerConfig marshaller(Object marshaller) {
        this.marshaller = marshaller
        this
    }

    MarshallerConfig priority(int priority) {
        this.priority = priority
        this
    }
}