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

import grails.test.mixin.*
import spock.lang.*

import org.codehaus.groovy.runtime.InvokerHelper

import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate


/*
  Attribution:
  Based on the rest-client-builder plugin by Graeme Rocher.
  See <a href="http://grails.org/plugin/rest-client-builder"/>
  Based on the functional-test plugin by Marc Palmer.
  See <a href="http://grails.org/plugin/functional-test"/>
*/
abstract class RestSpecification extends Specification {

    def response

    private RestSpecUtils utils

    /**
     * Issues a GET request and returns the response in the most appropriate type
     * @param url The URL
     * @param url The closure customizer used to customize request attributes
     */
    def get(url, Closure customizer = null) {
        doRequestInternal(url,customizer, HttpMethod.GET)
    }

    /**
     * Issues a PUT request and returns the response in the most appropriate type
     *
     * @param url The URL
     * @param customizer The clouser customizer
     */
    def put(url, Closure customizer = null) {
        doRequestInternal(url,customizer, HttpMethod.PUT)
    }

    /**
     * Issues a POST request and returns the response
     * @param url The URL
     * @param customizer (optional) The closure customizer
     */
    def post(url, Closure customizer = null) {
        doRequestInternal(url,customizer, HttpMethod.POST)
    }

    /**
     * Issues DELETE a request and returns the response
     * @param url The URL
     * @param customizer (optional) The closure customizer
     */
    def delete(url, Closure customizer = null) {
        doRequestInternal(url,customizer, HttpMethod.DELETE)
    }

    String responseHeader(String name) {
        List l = response?.headers[name]
        if (l == null) return null
        if (l.size() == 0) {
            return null
        } else if (l.size() == 1) {
            return l.get(0).toString()
        } else {
            throw new RuntimeException("more than one value for header found")
        }
    }

    def responseHeaders(String name) {
        return response?.headers[name]
    }


    protected doRequestInternal(String url, Closure customizer, HttpMethod method) {

        def requestCustomizer = new RequestCustomizer()
        if (customizer != null) {
            customizer.delegate = requestCustomizer
            customizer.call()
        }

        RestTemplate restTemplate = new RestTemplate()
        if (requestCustomizer.getRequestFactory() != null) {
            restTemplate.setRequestFactory( requestCustomizer.getRequestFactory() )
        }

        Class responseType = requestCustomizer.responseType != null ? requestCustomizer.responseType : String

        try {
            def entity = requestCustomizer.createEntity()
            if (isDisplay()) {
                getUtils().dumpRequestInfo(url,method,entity)
            }
            response = restTemplate.exchange(
                url, method, entity, responseType
            )

            response = new RestResponse( responseEntity:response )
        }
        catch (HttpStatusCodeException e) {
            response = new ErrorResponse(error:e)
        }
        if (isDisplay()) {
            getUtils().dumpResponse( response )
        }
    }

    protected boolean isDisplay() {
        return System.getProperties().getProperty("RestSpecification.display")
    }

    protected PrintStream getDisplayStream() {
        System.out
    }

    protected getUtils() {
        if (null == utils) {
            utils = new RestSpecUtils(getDisplayStream())
        }
        utils
    }
}
