/* ***************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

/**
 * Exception thrown when the methods defined for resource are not
 * a collection.
 **/
class MethodsNotCollectionException extends RuntimeException {
    String resourceName

    String getMessage() {
        "Resource $resourceName methods does not define a collection"
    }
}
