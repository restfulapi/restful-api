/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

class UnsupportedRequestRepresentationException extends RuntimeException {
    String pluralizedResourceName
    String contentType

    UnsupportedRequestRepresentationException( String pluralizedResourceName, String contentType ) {
        this.pluralizedResourceName = pluralizedResourceName
        this.contentType = contentType
    }
}