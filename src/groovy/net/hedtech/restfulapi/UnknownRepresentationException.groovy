/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

class UnknownRepresentationException extends RuntimeException {

    String pluralizedResourceName
    String contentType

    UnknownRepresentationException( String pluralizedResourceName, String contentType ) {
        this.pluralizedResourceName = pluralizedResourceName
        this.contentType = contentType
    }

}