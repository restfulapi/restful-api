/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class RestConfig {

    GrailsApplication grailsApplication

    def resources = [:]
    def jsonAsXml
    def marshallerGroups = [:]
    ConfigGroup jsonDomain = new ConfigGroup()

    RestConfig( GrailsApplication grailsApplication ) {
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

    void validate() {
        resources.values().each() { it.validate() }
        resources.values().each { resource ->
            resource.representations.values().each { representation ->
                if (representation.jsonAsXml) {
                    def json = getRepresentation(resource.name, getJsonEquivalentMediaType( representation.mediaType ))
                    if (json == null) {
                        throw new MissingJSONEquivalent( resourceName:resource.name, mediaType: representation.mediaType )
                    }
                }
            }
        }
    }

    String getJsonEquivalentMediaType( String xmlType ) {
        def s = xmlType.substring(0,xmlType.length()-3)
        return s + "json"
    }

//------------ These methods exist to support the closures used to provide configuration ------------------
//------------ They may throw exceptions to indicate errors when processing configuration -----------------

    static RestConfig parse(GrailsApplication app, def c) {
        RestConfig config = new RestConfig( app )
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        return config
    }

    RestConfig resource(Closure c) {
        ResourceConfig rc = new ResourceConfig(this)
        c.delegate = rc
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        resources.put( rc.name, rc )
        return this
    }

    RestConfig jsonAsXml(Closure c) {
        JsonAsXmlConfig config = new JsonAsXmlConfig()
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        jsonAsXml = config
        this
    }

    RestConfig marshallerGroup(Closure c) {
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

    def jsonDomainMarshaller(String name) {
        def closure = { Closure c ->
            DomainMarshallerConfig config = new DomainMarshallerConfig()
            c.delegate = config
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()
            jsonDomain.configs[name] = config
        }
        [params:closure]
    }



}