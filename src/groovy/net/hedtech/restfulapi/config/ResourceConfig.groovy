/* ***************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication
import net.hedtech.restfulapi.Methods

class ResourceConfig {

    String name
    String serviceName
    def methods = [ 'list', 'show', 'create', 'update', 'delete' ]
    def representations = [:]

    private RestConfig restConfig

    ResourceConfig setServiceName(String name) {
        this.serviceName = name
        return this
    }

    ResourceConfig setMethods( def methods ) {
        this.methods = methods
        return this
    }

    boolean allowsMethod( String method ) {
        this.methods.contains( method )
    }

    def getMethods() {
        return this.methods
    }

    ResourceConfig representation(Closure c) {
        RepresentationDelegate delegate = new RepresentationDelegate(restConfig)
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        delegate.mediaTypes.each { mediaType ->
            if (representations[mediaType] != null) {
               throw new AmbiguousRepresentationException( resourceName:name, mediaType:mediaType )
            }
            RepresentationConfig config = new RepresentationConfig(
                mediaType:mediaType, marshallerFramework:delegate.marshallerFramework,
                contentType:delegate.contentType,
                marshallers:delegate.marshallers, extractor:delegate.extractor )
            representations[mediaType] = config
        }
        return this
    }

    RepresentationConfig getRepresentation( String mediaType ) {
        representations[mediaType]
    }

    String getResourceName() {
        return name
    }

    void validate() {
        if (!(methods instanceof Collection)) {
            throw new MethodsNotCollectionException( resourceName: name )
        }
        if (methods != null) {
            methods.each {
                if (!(Methods.getAllMethods().contains( it ))) {
                    throw new UnknownMethodException( resourceName:name, methodName: it )
                }
            }
        }
    }
}
