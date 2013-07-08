/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

class RepresentationDelegate {

    def mediaTypes = []
    String marshallerFramework
    String contentType
    def marshallers = []
    def extractor

    private RestConfig restConfig

    RepresentationDelegate(RestConfig config) {
        this.restConfig = config
    }

    RepresentationDelegate extractor(Object obj) {
        this.extractor = obj
        return this
    }

    RepresentationDelegate jsonExtractor(Closure c) {
        JSONExtractorDelegate delegate = new JSONExtractorDelegate()
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        extractor = JSONExtractorFactory.instantiate(delegate.config, restConfig)
        this
    }

    RepresentationDelegate xmlExtractor(Closure c) {
        XMLExtractorDelegate delegate = new XMLExtractorDelegate()
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        extractor = XMLExtractorFactory.instantiate(delegate.config, restConfig)
        this
    }

    RepresentationDelegate marshallers(Closure c) {
        MarshallersDelegate delegate = new MarshallersDelegate(this)
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()

        this
    }

    protected MarshallerGroupConfig getMarshallerGroup(String name) {
        restConfig.getMarshallerGroup(name)
    }

}