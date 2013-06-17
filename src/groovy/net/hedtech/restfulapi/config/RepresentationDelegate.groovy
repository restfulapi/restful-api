/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

class RepresentationDelegate {

    def mediaTypes = []
    boolean jsonAsXml = false
    def marshallers = []
    def extractor

    private RestConfig restConfig

    RepresentationDelegate(RestConfig config) {
        this.restConfig = config
    }

    RepresentationDelegate jsonAsXml(boolean b) {
        this.jsonAsXml = b
        this
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