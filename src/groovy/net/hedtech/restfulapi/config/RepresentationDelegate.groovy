/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication
import net.hedtech.restfulapi.marshallers.json.DeclarativeDomainClassMarshaller

class RepresentationDelegate {

    def mediaTypes = []
    boolean jsonAsXml = false
    def marshallers = []
    def extractor

    private RestConfig restConfig

    RepresentationDelegate(RestConfig config) {
        this.restConfig = config
    }

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

    RepresentationDelegate addJSONDomainMarshaller(Closure c) {
        DomainMarshallerConfig config = new DomainMarshallerConfig()
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()

        //Merge the include chain into a final config
        config = restConfig.jsonDomain.getMergedConfig( config )

        def marshaller = new DeclarativeDomainClassMarshaller(
            app:restConfig.grailsApplication
        )
        marshaller.substitutions.putAll             config.substitutions
        marshaller.includedFields.addAll            config.includedFields
        marshaller.excludedFields.addAll            config.excludedFields
        marshaller.additionalFieldClosures.addAll   config.additionalFieldClosures
        if (config.isSupportClassSet)      marshaller.supportClass   = config.supportClass
        if (config.includeId != null)      marshaller.includeId      = config.includeId
        if (config.includeVersion != null) marshaller.includeVersion = config.includeVersion

        MarshallerConfig marshallerConfig = new MarshallerConfig()
        marshallerConfig.marshaller = marshaller
        marshallerConfig.priority = config.priority
        marshallers.add marshallerConfig
        this
    }



}