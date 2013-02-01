/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi

/**
 * A class meeting the requirements for an ApplicationException for
 * use in testing.
 **/
class DummyApplicationException extends RuntimeException {

    private int statusCode
    private def msgCode
    private def errorType

    DummyApplicationException( def statusCode, def msgCode, def errorType ) {
        this.statusCode = statusCode.toInteger()
        this.msgCode = msgCode
        this.errorType = errorType
    }

    public def getHttpStatusCode() {
        return statusCode
    }

    public returnMap = { localize ->
        def map = [:]
        if (msgCode) {
            map.message = localize( code: msgCode, args:['foo','foo','foo','foo'] )
        }
        if (errorType) {
            map.errors = [ "type": errorType ]
        }
        if (errorType == "validation") {
            map.headers = ['X-Status-Reason':'Validation failed']
        }
        if (errorType == "programming") {
            throw new RuntimeException( "simulating coding error in error handling" )
        }
        map
    }


}