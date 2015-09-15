/* ***************************************************************************
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
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

package net.hedtech.restfulapi.exceptionhandlers

import net.hedtech.restfulapi.ErrorResponse
import net.hedtech.restfulapi.ExceptionHandler
import net.hedtech.restfulapi.ExceptionHandlerContext

class ApplicationExceptionHandler implements ExceptionHandler {

    boolean supports(Throwable e) {
        e.metaClass.respondsTo( e, "getHttpStatusCode") &&
        e.hasProperty( "returnMap" ) &&
        e.returnMap &&
        e.returnMap instanceof Closure
        //treat as an 'ApplicationException'.  That is, assume the exception is taking
        //responsibility for specifying the correct status code and
        //response message elements
    }

    ErrorResponse handle(Throwable e, ExceptionHandlerContext context) {
        // wrap the 'message' invocation within a closure, so it can be
        // passed into an ApplicationException to localize error messages
        def localizer = { mapToLocalize ->
            context.localizer.message(mapToLocalize)
        }

        def response = new ErrorResponse()
        response.httpStatusCode = e.getHttpStatusCode()

        def appMap = e.returnMap(localizer)
        if (appMap.headers) {
            response.headers = appMap.headers
        }
        if (appMap.message) {
            response.message = appMap.message
        }

        def returnMap = [:]
        if (appMap.errors) {
            returnMap.errors = appMap.errors
        }
        response.content = returnMap

        return response
    }
}
