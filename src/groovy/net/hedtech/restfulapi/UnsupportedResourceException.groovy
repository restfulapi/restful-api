/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi


class UnsupportedResourceException extends RuntimeException {

    String pluralizedResourceName

    UnsupportedResourceException( String pluralizedResourceName ) {
        this.pluralizedResourceName = pluralizedResourceName
    }

}