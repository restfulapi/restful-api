/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

class UnspecifiedMarshallerFrameworkException extends RuntimeException {
    String pluralizedResourceName
    String mediaType

    String getMessage() {
        "Cannot support media type ${mediaType} in resource ${pluralizedResourceName}.  Marshaller framework not specified, and cannot assign to json or xml based on media type."
    }
}