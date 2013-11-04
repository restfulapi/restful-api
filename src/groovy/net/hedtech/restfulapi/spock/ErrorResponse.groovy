/* ***************************************************************************
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

package net.hedtech.restfulapi.spock

import grails.converters.JSON
import grails.converters.XML
import groovy.util.slurpersupport.GPathResult

import org.codehaus.groovy.grails.web.json.JSONElement
import org.springframework.web.client.HttpStatusCodeException

class ErrorResponse {

    @Delegate HttpStatusCodeException error

    @Lazy String text = {
        error.responseBodyAsString
    }()

    @Lazy String contentType = {
        error?.responseHeaders?.getContentType().toString().split(';')[0]
    }()

    String header(String name) {
        responseEntity?.getResponseHeaders?.getFirst(name)
    }

    def getHeaders() {
        error.getResponseHeaders()
    }

    int getStatus() {
        error.statusCode?.value() ?: 200
    }

    def getBody() {
         error.responseBodyAsString
    }
}
