/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class RESTConfig {

    private GrailsApplication grailsApplication

    def resources = [:]
    def jsonAsXml

    RESTConfig( GrailsApplication grailsApplication ) {
        this.grailsApplication = grailsApplication
    }

    static RESTConfig parse(GrailsApplication app, def c) {
        RESTConfig config = new RESTConfig( app )
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        return config
    }

    RESTConfig resource(Closure c) {
        ResourceConfig rc = new ResourceConfig( grailsApplication )
        c.delegate = rc
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        resources.put( rc.name, rc )
        return this
    }

    RESTConfig jsonAsXml(Closure c) {
        JsonAsXmlConfig config = new JsonAsXmlConfig( grailsApplication )
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        jsonAsXml = config
        this

    }

    ResourceConfig getResource( String pluralizedName ) {
        return resources[pluralizedName]
    }

    RepresentationConfig getRepresentation(pluralizedResourceName, allowedTypes) {
        ResourceConfig resource = getResource( pluralizedResourceName )
        if (!resource) return null
        for (def type : allowedTypes) {
            def rep = resource.getRepresentation( type )
            if (rep != null) return rep
        }
        return null
    }

}