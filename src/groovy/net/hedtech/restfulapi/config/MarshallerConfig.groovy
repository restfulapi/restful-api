/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class MarshallerConfig {

    def instance
    int priority = 0

    MarshallerConfig instance(Object instance) {
        this.instance = instance
        this
    }

    MarshallerConfig priority(int priority) {
        this.priority = priority
        this
    }
}
