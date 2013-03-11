/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class RepresentationDelegate {

    def mediaTypes = []
    boolean jsonAsXml = false
    def marshallers = []
    def extractor

    RepresentationDelegate setMediaType(String mediaType) {
        this.mediaTypes.add mediaType
        return this
    }

    RepresentationDelegate jsonAsXml(boolean b) {
        this.jsonAsXml = b
        this
    }


    RepresentationDelegate addMarshaller(Closure c) {
        MarshallerConfig config = new MarshallerConfig()
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        marshallers.add config
        this
    }

    RepresentationDelegate addMarshaller(MarshallerGroupConfig group) {
        group.marshallers.each() {
            marshallers.add it
        }
        this
    }

    RepresentationDelegate extractor(Object obj) {
        this.extractor = obj
        return this
    }

}