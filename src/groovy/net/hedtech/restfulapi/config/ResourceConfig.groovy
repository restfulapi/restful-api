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
    //use a LinkedHashMap because we want to preserve
    //the order in which representations are added
    def representations = new LinkedHashMap()
    //if set, defines the media type to use for */*
    String anyMediaType

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
            if (representations.get(mediaType) != null) {
               throw new AmbiguousRepresentationException( resourceName:name, mediaType:mediaType )
            }
            RepresentationConfig config = new RepresentationConfig(
                mediaType:mediaType, marshallerFramework:delegate.marshallerFramework,
                contentType:delegate.contentType,
                marshallers:delegate.marshallers, extractor:delegate.extractor )
            representations.put(mediaType, config)
        }
        return this
    }

    RepresentationConfig getRepresentation( String mediaType ) {
        if ('*/*' == mediaType) {
            mediaType = null //clear type, we need to choose one
            if (null != anyMediaType) {
                mediaType = anyMediaType
            } else {
                //pick the first type
                if (representations.size() > 0) {
                    mediaType = representations.entrySet().iterator().next().key
                }
            }
        }
        if (mediaType != null) {
            return representations.get(mediaType)
        }
        return null
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
        if (anyMediaType != null && representations.get(anyMediaType) == null) {
            throw new MissingAnyMediaType(resourceName: name, mediaType:anyMediaType)
        }
    }
}
