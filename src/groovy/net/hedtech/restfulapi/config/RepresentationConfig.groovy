/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class RepresentationConfig {

    private GrailsApplication grailsApplication

    String mediaType
    boolean jsonAsXml = false
    def marshallers = []
    def extractor

    RepresentationConfig( GrailsApplication grailsApplication ) {
        this.grailsApplication = grailsApplication
    }

    RepresentationConfig setMediaType(String mediaType) {
        this.mediaType = mediaType
        return this
    }

    RepresentationConfig jsonAsXml(boolean b) {
        this.jsonAsXml = b
        this
    }


    RepresentationConfig marshaller(Closure c) {
        MarshallerConfig config = new MarshallerConfig( grailsApplication )
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        marshallers.add config
        this
    }

    RepresentationConfig extractor(Object obj) {
        this.extractor = obj
        return this
    }

}