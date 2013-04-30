/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.query

class BadFilterException extends RuntimeException {

    String pluralizedResourceName
    List badFilters


    public BadFilterException( String pluralizedResourceName, badFilters ) {

        this.pluralizedResourceName = pluralizedResourceName
        this.badFilters = badFilters
    }
}