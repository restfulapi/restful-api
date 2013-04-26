/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class ResourceConfig {

    String name
    String serviceName
    def allowedMethods = [ 'list', 'show', 'save', 'update', 'delete' ]
    def representations = [:]

    ResourceConfig name(String name) {
        this.name = name
        return this
    }

    ResourceConfig serviceName(String name) {
        this.serviceName = name
        return this
    }

    ResourceConfig allowedMethods(def allowed) {
        this.allowedMethods = allowed
        return this
    }

    ResourceConfig representation(Closure c) {
        RepresentationDelegate delegate = new RepresentationDelegate()
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        delegate.mediaTypes.each { mediaType ->
            if (representations[mediaType] != null) {
               throw new AmbiguousRepresentationException( resourceName:name, mediaType:mediaType )
            }
            RepresentationConfig config = new RepresentationConfig( mediaType:mediaType, jsonAsXml:delegate.jsonAsXml, marshallers:delegate.marshallers, extractor:delegate.extractor )
            representations[mediaType] = config
        }
        this
    }

    RepresentationConfig getRepresentation( String mediaType ) {
        def config = representations[mediaType]
    }

    String getResourceName() {
        return name
    }


}