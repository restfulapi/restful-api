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
import net.hedtech.restfulapi.Inflector

class ValidationExceptionHandler implements ExceptionHandler {

    boolean supports(Throwable t) {
        (t instanceof grails.validation.ValidationException)
    }

    ErrorResponse handle(Throwable e, ExceptionHandlerContext context) {
        new ErrorResponse(
            httpStatusCode: 400,
            headers: ['X-Status-Reason':'Validation failed'],
            message: context.localizer.message(
                code: "default.rest.validation.errors.message",
                args: [ Inflector.singularize(context.pluralizedResourceName)]),
            content: [
                errors: [
                    [
                        type: "validation",
                        errorMessage: e.message
                    ]
                ]
            ]
        )
    }
}
