/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class JsonAsXmlConfig {

    private GrailsApplication grailsApplication

    boolean enableDefault
    def marshallers = []
    def extractor

    JsonAsXmlConfig( GrailsApplication grailsApplication ) {
        this.grailsApplication = grailsApplication
    }

    JsonAsXmlConfig enableDefault(boolean b) {
        this.enableDefault = b
        return this
    }


    JsonAsXmlConfig representation(Closure c) {
        RepresentationConfig config = new RepresentationConfig( grailsApplication )
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        representations[config.mediaType] = config
        this
    }

    JsonAsXmlConfig marshaller(Closure c) {
        MarshallerConfig config = new MarshallerConfig( grailsApplication )
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        marshallers.add config
        this
    }

    JsonAsXmlConfig extractor(Object obj) {
        this.extractor = obj
        this
    }


}