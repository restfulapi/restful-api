/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi

/**
 * An ApplicationException for use in testing.
 * This 'dummy' exception satisfies the requirements of an
 * ApplicationException. That is, it has:
 *   1) a public 'def getHttpStatusCode()' method
 *   2) a public 'returnMap' property that is a Closure
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
