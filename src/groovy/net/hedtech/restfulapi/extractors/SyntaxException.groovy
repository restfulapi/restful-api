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
package net.hedtech.restfulapi.extractors

/**
 * An application style exception for use when declarative extractors
 * encounter an error with input, for example, unparsable dates.
 * This exception satisfies the requirements of an
 * ApplicationException. That is, it has:
 *   1) a public 'def getHttpStatusCode()' method
 *   2) a public 'returnMap' property that is a Closure
 * It will always result in a 400 response status, with a 'Validation failed'
 * X-Status-Reason header.
 **/
class SyntaxException extends Exception {

    def msgCode
    def params

    SyntaxException( String msgCode, def params ) {
        this.msgCode = msgCode
        this.params = params
    }

    public def getHttpStatusCode() {
        return 400
    }

    public returnMap = { localize ->
        def map = [:]
        if (msgCode) {
            map.message = localize( code: msgCode, args:params )
        }
        map.headers = ['X-Status-Reason':'Validation failed']
        map
    }


}
