/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class ResourceConfig {

    private GrailsApplication grailsApplication

    String name
    def representations = [:]

    ResourceConfig( GrailsApplication grailsApplication ) {
        this.grailsApplication = grailsApplication
    }

    ResourceConfig name(String name) {
        this.name = name
        return this
    }


    ResourceConfig representation(Closure c) {
        RepresentationConfig config = new RepresentationConfig( grailsApplication )
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        representations[config.mediaType] = config
        this
    }

    RepresentationConfig getRepresentation( String mediaType ) {
        def config = representations[mediaType]
    }


}