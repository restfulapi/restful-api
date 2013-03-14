/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

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