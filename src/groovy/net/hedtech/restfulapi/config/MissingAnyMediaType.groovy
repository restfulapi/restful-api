/* ***************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

/**
 * Exception thrown when the type declared for anyMediaType on a resource
 * is not defined within the resource.
 **/
class MissingAnyMediaType extends RuntimeException {
    String resourceName
    String mediaType

    String getMessage() {
        "Resource $resourceName uses $mediaType for anyMediaType, but no representation exists for this type"
    }
}
