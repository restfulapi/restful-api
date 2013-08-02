/* ***************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class MarshallerGroupsDelegate {

    private RestConfig restConfig

    MarshallerGroupsDelegate(RestConfig restConfig) {
        this.restConfig = restConfig
    }

    def group(String name) {
        GroupDelegate delegate = new GroupDelegate()
        [marshallers:{Closure c ->
            c.delegate = delegate
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()
            MarshallerGroupConfig config = new MarshallerGroupConfig(name:name,marshallers:delegate.marshallers)
            restConfig.marshallerGroups[name] = config
        }]
    }

    class GroupDelegate {
        def marshallers = []

        GrailsApplication getGrailsApplication() {
            restConfig.grailsApplication
        }

        GroupDelegate marshaller( Closure c ) {
            MarshallerConfig config = new MarshallerConfig()
            c.delegate = config
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()
            marshallers.add config
            this
        }

        GroupDelegate jsonDomainMarshaller( Closure c ) {
            JSONDomainMarshallerDelegate delegate = new JSONDomainMarshallerDelegate()
            c.delegate = delegate
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()

            def marshaller = JSONDomainMarshallerFactory.instantiateMarshaller(delegate.config,restConfig)

            MarshallerConfig marshallerConfig = new MarshallerConfig()
            marshallerConfig.instance = marshaller
            marshallerConfig.priority = delegate.config.priority

            marshallers.add marshallerConfig

            this
        }

        GroupDelegate jsonGroovyBeanMarshaller( Closure c ) {
            JSONGroovyBeanMarshallerDelegate delegate = new JSONGroovyBeanMarshallerDelegate()
            c.delegate = delegate
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()

            def marshaller = JSONGroovyBeanMarshallerFactory.instantiateMarshaller(delegate.config,restConfig)

            MarshallerConfig marshallerConfig = new MarshallerConfig()
            marshallerConfig.instance = marshaller
            marshallerConfig.priority = delegate.config.priority

            marshallers.add marshallerConfig

            this
        }

        GroupDelegate xmlDomainMarshaller( Closure c ) {
            XMLDomainMarshallerDelegate delegate = new XMLDomainMarshallerDelegate()
            c.delegate = delegate
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()

            def marshaller = XMLDomainMarshallerFactory.instantiateMarshaller(delegate.config,restConfig)

            MarshallerConfig marshallerConfig = new MarshallerConfig()
            marshallerConfig.instance = marshaller
            marshallerConfig.priority = delegate.config.priority

            marshallers.add marshallerConfig

            this
        }

        GroupDelegate xmlGroovyBeanMarshaller( Closure c ) {
            XMLGroovyBeanMarshallerDelegate delegate = new XMLGroovyBeanMarshallerDelegate()
            c.delegate = delegate
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()

            def marshaller = XMLGroovyBeanMarshallerFactory.instantiateMarshaller(delegate.config,restConfig)

            MarshallerConfig marshallerConfig = new MarshallerConfig()
            marshallerConfig.instance = marshaller
            marshallerConfig.priority = delegate.config.priority

            marshallers.add marshallerConfig

            this
        }
    }

}
