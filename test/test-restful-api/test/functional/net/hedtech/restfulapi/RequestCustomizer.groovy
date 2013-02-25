/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import grails.converters.JSON
import grails.converters.XML
import grails.web.JSONBuilder
import groovy.xml.StreamingMarkupBuilder

import org.codehaus.groovy.grails.plugins.codecs.Base64Codec
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

class RequestCustomizer {

    HttpHeaders headers = new HttpHeaders()

    def body

    MultiValueMap<String, Object> mvm = new LinkedMultiValueMap<String, Object>()

    // configures basic author
    RequestCustomizer auth(String username, String password) {
        String encoded = Base64Codec.encode("$username:$password")
        headers.Authorization = "Basic $encoded".toString()
        return this
    }

    RequestCustomizer contentType(String contentType) {
        headers.setContentType(MediaType.valueOf(contentType))
        return this
    }

    RequestCustomizer accept(String... contentTypes) {
        def list = contentTypes.collect { MediaType.valueOf(it) }
        headers.setAccept(list)
        return this
    }

    RequestCustomizer headers(Closure c) {
        c.delegate = this.headers
        c.call()
        return this
    }

    RequestCustomizer body(Closure c) {
        body = c()?.toString()
        return this
    }

    HttpEntity createEntity() {
        return mvm ? new HttpEntity(mvm, headers) : new HttpEntity(body, headers)
    }

    void setProperty(String name, value) {
        if (value instanceof File) {
            value = new FileSystemResource(value)
        }
        else if (value instanceof URL) {
            value = new UrlResource(value)
        }
        else if (value instanceof InputStream) {
            value = new InputStreamResource(value)
        }
        mvm[name] = value
    }
}