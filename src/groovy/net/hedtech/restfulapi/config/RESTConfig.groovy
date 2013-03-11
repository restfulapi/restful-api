/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class RESTConfig {

    GrailsApplication grailsApplication

    def resources = [:]
    def jsonAsXml
    def marshallerGroups = [:]

    RESTConfig( GrailsApplication grailsApplication ) {
        this.grailsApplication = grailsApplication
    }

    ResourceConfig getResource( String pluralizedName ) {
        return resources[pluralizedName]
    }

    RepresentationConfig getRepresentation(String pluralizedResourceName, String type ) {
        getRepresentation(pluralizedResourceName, [type])
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

//------------ These methods exist to support the closures used to provide configuration ------------------
//------------ They may throw exceptions to indicate errors when processing configuration -----------------

    static RESTConfig parse(GrailsApplication app, def c) {
        RESTConfig config = new RESTConfig( app )
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        return config
    }

    RESTConfig resource(Closure c) {
        ResourceConfig rc = new ResourceConfig()
        c.delegate = rc
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        resources.put( rc.name, rc )
        return this
    }

    RESTConfig jsonAsXml(Closure c) {
        JsonAsXmlConfig config = new JsonAsXmlConfig()
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        jsonAsXml = config
        this
    }

    RESTConfig marshallerGroup(Closure c) {
        MarshallerGroupConfig group = new MarshallerGroupConfig()
        c.delegate = group
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        marshallerGroups[group.name] = group
        this
    }

    MarshallerGroupConfig getMarshallerGroup( String name ) {
        def group = marshallerGroups[name]
        if (group == null) {
            throw new MissingMarshallerGroupException( name:name )
        }
        group
    }

}