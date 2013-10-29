/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.query

class BadDateFilterException extends BadFilterException {

    public BadDateFilterException( String pluralizedResourceName, badFilter ) {
        super(pluralizedResourceName, [badFilter])
    }
}