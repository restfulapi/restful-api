/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class RestConfig {

    GrailsApplication grailsApplication

    def resources = [:]
    def jsonAsXml
    //map of group name to MarshallerGroupConfig instance
    def marshallerGroups = [:]
    ConfigGroup jsonDomain = new ConfigGroup()
    ConfigGroup groovyBean = new ConfigGroup()
    ConfigGroup jsonExtractor = new ConfigGroup()

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

    def resource(String name) {
        ResourceConfig rc = new ResourceConfig(restConfig:this,name:name)
        Closure closure = { Closure c->
            c.delegate = rc
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()
            if (rc.name != name) throw new RuntimeException("Name of resource illegally changed")
            resources.put( name, rc )
        }
        [config:closure]
    }

    RestConfig marshallerGroups(Closure c) {
        MarshallerGroupsDelegate delegate = new MarshallerGroupsDelegate( this )
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        this
    }

    MarshallerGroupConfig getMarshallerGroup( String name ) {
        def group = marshallerGroups[name]
        if (group == null) {
            throw new MissingMarshallerGroupException( name:name )
        }
        group
    }

    def jsonDomainMarshallerTemplates(Closure c) {
        JSONDomainTemplates delegate = new JSONDomainTemplates(this)
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        this
    }

    def jsonGroovyBeanMarshallerTemplates(Closure c) {
        JSONGroovyBeanTemplates delegate = new JSONGroovyBeanTemplates(this)
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        this
    }

    def jsonExtractorTemplates(Closure c) {
        JSONExtractorTemplates delegate = new JSONExtractorTemplates(this)
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        this
    }

    class JSONDomainTemplates {
        RestConfig parent
        JSONDomainTemplates(RestConfig parent) {
            this.parent = parent
        }

        def template(String name) {
            def closure = { Closure c ->
                JSONDomainMarshallerDelegate delegate = new JSONDomainMarshallerDelegate()
                c.delegate = delegate
                c.resolveStrategy = Closure.DELEGATE_FIRST
                c.call()
                parent.jsonDomain.configs[name] = delegate.config
            }
            [config:closure]
        }
    }

    class JSONGroovyBeanTemplates {
        RestConfig parent
        JSONGroovyBeanTemplates(RestConfig parent) {
            this.parent = parent
        }

        def template(String name) {
            def closure = { Closure c ->
                JSONGroovyBeanMarshallerDelegate delegate = new JSONGroovyBeanMarshallerDelegate()
                c.delegate = delegate
                c.resolveStrategy = Closure.DELEGATE_FIRST
                c.call()
                parent.groovyBean.configs[name] = delegate.config
            }
            [config:closure]
        }
    }

    class JSONExtractorTemplates {
        RestConfig parent
        JSONExtractorTemplates(RestConfig parent) {
            this.parent = parent
        }

        def template(String name) {
            def closure = { Closure c ->
                JSONExtractorDelegate delegate = new JSONExtractorDelegate()
                c.delegate = delegate
                c.resolveStrategy = Closure.DELEGATE_FIRST
                c.call()
                parent.jsonExtractor.configs[name] = delegate.config
            }
            [config:closure]
        }
    }

}