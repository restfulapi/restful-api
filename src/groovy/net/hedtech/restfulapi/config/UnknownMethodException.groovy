/* ***************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

/**
 * Exception thrown when two representations for a resource
 * have the same media type.
 **/
class UnknownMethodException extends RuntimeException {
    String resourceName
    String methodName

    String getMessage() {
        "Resource $resourceName references unknown method $methodName"
    }
}
