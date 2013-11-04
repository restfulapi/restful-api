/* ****************************************************************************
 * Copyright 2013 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class RestConfig {

    GrailsApplication grailsApplication

    //map of resource name to its configuration
    //A resource with an empty name "" is treated as
    //a default resource block matching any resource
    //not explicitly named
    def resources = [:]

    //map of group name to MarshallerGroupConfig instance
    def marshallerGroups = [:]
    ConfigGroup jsonDomain    = new ConfigGroup()
    ConfigGroup jsonBean      = new ConfigGroup()
    ConfigGroup jsonExtractor = new ConfigGroup()

    ConfigGroup xmlDomain     = new ConfigGroup()
    ConfigGroup xmlBean       = new ConfigGroup()
    ConfigGroup xmlExtractor  = new ConfigGroup()

    RestConfig( GrailsApplication grailsApplication ) {
        this.grailsApplication = grailsApplication
    }

    ResourceConfig getResource( String pluralizedName ) {
        def resource = resources[pluralizedName]
        if (resource == null) {
            resource = resources[""]
        }
        resource
    }

    RepresentationConfig getRepresentation(String pluralizedResourceName, String type ) {
        getRepresentation(pluralizedResourceName, [type])
    }

    RepresentationConfig getRepresentation(String pluralizedResourceName, allowedTypes) {
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

    RestConfig anyResource(Closure c) {
        ResourceConfig rc = new ResourceConfig(restConfig:this,name:"")
        c.delegate = rc
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        if (rc.name != "") throw new RuntimeException("Name of resource illegally changed")
        resources.put( "", rc )
        this
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

    boolean hasMarshallerGroup(String name) {
        return marshallerGroups[name] != null
    }

    def jsonDomainMarshallerTemplates(Closure c) {
        JSONDomainTemplates delegate = new JSONDomainTemplates(this)
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        this
    }

    def jsonBeanMarshallerTemplates(Closure c) {
        JSONBeanTemplates delegate = new JSONBeanTemplates(this)
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

    def xmlDomainMarshallerTemplates(Closure c) {
        XMLDomainTemplates delegate = new XMLDomainTemplates(this)
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        this
    }

    def xmlBeanMarshallerTemplates(Closure c) {
        XMLBeanTemplates delegate = new XMLBeanTemplates(this)
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        this
    }

    def xmlExtractorTemplates(Closure c) {
        XMLExtractorTemplates delegate = new XMLExtractorTemplates(this)
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

    class XMLDomainTemplates {
        RestConfig parent
        XMLDomainTemplates(RestConfig parent) {
            this.parent = parent
        }

        def template(String name) {
            def closure = { Closure c ->
                XMLDomainMarshallerDelegate delegate = new XMLDomainMarshallerDelegate()
                c.delegate = delegate
                c.resolveStrategy = Closure.DELEGATE_FIRST
                c.call()
                parent.xmlDomain.configs[name] = delegate.config
            }
            [config:closure]
        }
    }

    class JSONBeanTemplates {
        RestConfig parent
        JSONBeanTemplates(RestConfig parent) {
            this.parent = parent
        }

        def template(String name) {
            def closure = { Closure c ->
                JSONBeanMarshallerDelegate delegate = new JSONBeanMarshallerDelegate()
                c.delegate = delegate
                c.resolveStrategy = Closure.DELEGATE_FIRST
                c.call()
                parent.jsonBean.configs[name] = delegate.config
            }
            [config:closure]
        }
    }

    class XMLBeanTemplates {
        RestConfig parent
        XMLBeanTemplates(RestConfig parent) {
            this.parent = parent
        }

        def template(String name) {
            def closure = { Closure c ->
                XMLBeanMarshallerDelegate delegate = new XMLBeanMarshallerDelegate()
                c.delegate = delegate
                c.resolveStrategy = Closure.DELEGATE_FIRST
                c.call()
                parent.xmlBean.configs[name] = delegate.config
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

    class XMLExtractorTemplates {
        RestConfig parent
        XMLExtractorTemplates(RestConfig parent) {
            this.parent = parent
        }

        def template(String name) {
            def closure = { Closure c ->
                XMLExtractorDelegate delegate = new XMLExtractorDelegate()
                c.delegate = delegate
                c.resolveStrategy = Closure.DELEGATE_FIRST
                c.call()
                parent.xmlExtractor.configs[name] = delegate.config
            }
            [config:closure]
        }
    }

}
