/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

class IdMismatchException extends RuntimeException {
    String pluralizedResourceName

    IdMismatchException( String pluralizedResourceName ) {
        this.pluralizedResourceName = pluralizedResourceName
    }
}