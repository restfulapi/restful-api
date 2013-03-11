/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

/**
 * Exception thrown when two representations for a resource
 * have the same media type.
 **/
class AmbiguousRepresentationException extends RuntimeException {
    String resourceName
    String mediaType

    String getMessage() {
        "Resource $resourceName has more than one representation for $mediaType configured"
    }
}