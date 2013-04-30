/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

/**
 * Exception thrown when two representations for a resource
 * have the same media type.
 **/
class MethodsNotCollectionException extends RuntimeException {
    String resourceName

    String getMessage() {
        "Resource $resourceName methods does not define a collection"
    }
}