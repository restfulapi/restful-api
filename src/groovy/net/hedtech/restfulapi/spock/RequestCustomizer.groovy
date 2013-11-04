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

/*
  Attribution:
  Based on the rest-client-builder plugin by Graeme Rocher.
  See <a href="http://grails.org/plugin/rest-client-builder"/>
*/
class RequestCustomizer {

    HttpHeaders headers = new HttpHeaders()

    def body
    def requestFactory
    Class responseType

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

    RequestCustomizer requestFactory(Object factory) {
        this.requestFactory = factory
        return this
    }

    RequestCustomizer responseType(Class clazz) {
        this.responseType = clazz
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
