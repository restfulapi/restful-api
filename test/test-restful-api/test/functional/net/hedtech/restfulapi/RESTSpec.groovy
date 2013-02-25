/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import grails.test.mixin.*
import spock.lang.*
import grails.plugins.rest.client.*

import org.codehaus.groovy.runtime.InvokerHelper

import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
//import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate


abstract class RESTSpec extends Specification {

    def response

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


    protected doRequestInternal(String url, Closure customizer, HttpMethod method) {

        def requestCustomizer = new RequestCustomizer()
        if (customizer != null) {
            customizer.delegate = requestCustomizer
            customizer.call()
        }

        RestTemplate restTemplate = new RestTemplate()
        try {
            def entity = requestCustomizer.createEntity()
            RESTSpecUtils.dumpRequestInfo(url,method,entity)
            response = restTemplate.exchange(
                url, method, entity, String
            )

            response = new RestResponse( responseEntity:response )
        }
        catch (HttpStatusCodeException e) {
            response = new ErrorResponse(error:e)
        }
        RESTSpecUtils.dumpResponse( response )
    }
}