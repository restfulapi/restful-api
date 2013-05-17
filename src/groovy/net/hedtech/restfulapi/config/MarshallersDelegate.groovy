/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

class MarshallersDelegate {

    RepresentationDelegate parent

    MarshallersDelegate( RepresentationDelegate parent ) {
        this.parent = parent
    }

    MarshallersDelegate marshaller(Closure c) {
        MarshallerConfig config = new MarshallerConfig()
        c.delegate = config
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        parent.marshallers.add config
        this
    }

    MarshallersDelegate marshallerGroup(String name) {
        def group = parent.getMarshallerGroup( name )
        group.marshallers.each() {
            parent.marshallers.add it
        }
        this
    }

    MarshallersDelegate jsonDomainMarshaller(Closure c) {
        JSONDomainMarshallerDelegate delegate = new JSONDomainMarshallerDelegate()
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()

        def marshaller = JSONDomainMarshallerFactory.instantiateDomainClassMarshaller(delegate.config,parent.restConfig)

        MarshallerConfig marshallerConfig = new MarshallerConfig()
        marshallerConfig.instance = marshaller
        marshallerConfig.priority = delegate.config.priority
        parent.marshallers.add marshallerConfig
        this
    }
}