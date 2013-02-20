/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

class UnsupportedResponseRepresentationException extends RuntimeException {

    String pluralizedResourceName
    String contentType

    UnsupportedResponseRepresentationException( String pluralizedResourceName, String contentType ) {
        this.pluralizedResourceName = pluralizedResourceName
        this.contentType = contentType
    }

}