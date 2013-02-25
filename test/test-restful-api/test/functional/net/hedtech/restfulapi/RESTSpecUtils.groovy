/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import org.springframework.http.*

class RESTSpecUtils {
    static maxW = 80

    static final void dumpHeading(String title) {
        def padL = '== '
        def padR = '=' * Math.max( (int)2, (int)(maxW - (padL.length() + 1 + title.length())) )

        System.out.println(padL + title + ' ' + padR)

    }

    static final void dumpSeparator() {
        System.out.println('='*maxW)
    }

    static final void dumpRequestInfo(url, method, HttpEntity entity) {
        System.out.println('')
        dumpHeading("Making request ${method} ${url}")
        dumpHeading("Request headers:")
        dumpHeaders( entity?.getHeaders() )
        dumpHeading("Content")
        if (entity.hasBody()) System.out.println(entity.getBody())
        dumpSeparator()
    }

    static final void dumpResponse(response) {
        dumpHeading("Response was ${response.getStatusCode().value()} headers:")
        dumpHeaders( response?.getHeaders() )
        dumpSeparator()
        dumpHeading("Content")
        System.out.println(response.getBody())
        dumpSeparator()
    }

    static final void dumpHeaders(def headers) {
        headers?.entrySet()?.each {
            def key = it.key
            it.value.each() {
                System.out.println("${key}: ${it.value}")
            }
        }

    }

}