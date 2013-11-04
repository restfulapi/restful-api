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

import org.springframework.http.*

/*
  Attribution:
  Based on formatting request/response output in
  the functional-test plugin by Marc Palmer.
  See <a href="http://grails.org/plugin/functional-test"/>
*/
class RestSpecUtils {
    static maxW = 80

    protected PrintStream out

    RestSpecUtils(PrintStream out) {
        this.out = out
    }

    final void dumpHeading(String title) {
        def padL = '== '
        def padR = '=' * Math.max( (int)2, (int)(maxW - (padL.length() + 1 + title.length())) )

        out.println(padL + title + ' ' + padR)
    }

    final void dumpSeparator() {
        out.println('='*maxW)
    }

    final void dumpRequestInfo(url, method, HttpEntity entity) {
        out.println('')
        dumpHeading("Making request ${method} ${url}")
        dumpHeading("Request headers:")
        dumpHeaders( entity?.getHeaders() )
        dumpHeading("Content")
        if (entity.hasBody()) out.println(entity.getBody())
        dumpSeparator()
    }

    final void dumpResponse(response) {
        dumpHeading("Response was ${response.getStatusCode().value()} headers:")
        dumpHeaders( response?.getHeaders() )
        dumpSeparator()
        dumpHeading("Content")
        out.println(response.getBody())
        dumpSeparator()
    }

    final void dumpHeaders(def headers) {
        headers?.entrySet()?.each {
            def key = it.key
            it.value.each() {
                out.println("${key}: ${it.value}")
            }
        }
    }
}
