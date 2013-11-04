/* ****************************************************************************
 * Copyright 2013 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
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
