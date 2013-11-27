/* ****************************************************************************
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

package net.hedtech.restfulapi

import grails.test.mixin.*
import grails.plugins.rest.client.*

import grails.converters.JSON
import grails.converters.XML

import java.util.zip.*

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.spock.*

import spock.lang.*


class RestfulApiControllerFunctionalSpec extends RestSpecification {

    static final String localBase = "http://127.0.0.1:8080/test-restful-api"

    def setup() {
        deleteThings()
    }

    def cleanup() {
        deleteThings()
    }

    def "Test list with json response"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:"list with  application/json accept"
        get("$localBase/api/things") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        2 == json.size()
        "AA" == json[0].code
        "An AA thing" == json[0].description

        // assert localization of the message
        "List of thing resources" == responseHeader('X-hedtech-message')

        //check pagination headers
        "2" == responseHeader('X-hedtech-totalCount')
        null != responseHeader('X-hedtech-pageOffset')
        null != responseHeader('X-hedtech-pageMaxSize')
        json[0]._href?.contains('things')
        null == json[0].numParts
        null == json[0].sha1
    }

    def "Test list filtering with json response"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:"list with application/json accept"
        get("$localBase/api/things?filter[0][field]=code&filter[0][operator]=eq&filter[0][value]=AA&max=1") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        1 == json.size()
        "AA" == json[0].code
        "An AA thing" == json[0].description

        // assert localization of the message
        "List of thing resources" == responseHeader('X-hedtech-message')

        //check pagination headers
        "1" == responseHeader('X-hedtech-totalCount')
        "0" == responseHeader('X-hedtech-pageOffset')
        "1" == responseHeader('X-hedtech-pageMaxSize')
    }

    def "Test list filtering using POST with json"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:"list with application/json accept"
        post("$localBase/qapi/things") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
                body {
                // akin to ?filter[0][field]=code&filter[0][operator]=eq&filter[0][value]=BB&max=1
                """
                {
                    "filter[0][field]": "code",
                    "filter[0][operator]": "eq",
                    "filter[0][value]": "BB",
                    "max":"1"
                }
                """
            }
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        1 == json.size()
        "BB" == json[0].code
        "An BB thing" == json[0].description

        // assert localization of the message
        "List of thing resources" == responseHeader('X-hedtech-message')

        //check pagination headers
        "1" == responseHeader('X-hedtech-totalCount')
        "0" == responseHeader('X-hedtech-pageOffset')
        "1" == responseHeader('X-hedtech-pageMaxSize')
    }

    def "Test filtering with date"() {
        setup:
        createThing('AA')

        when:
        get("$localBase/api/things?filter[0][field]=dateManufactured&filter[0][operator]=gt&filter[0][value]=2005-01-01T12:00:00Z&filter[0][type]=date&max=1") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        1 == json.size()
        "AA" == json[0].code
        "An AA thing" == json[0].description

        // assert localization of the message
        "List of thing resources" == responseHeader('X-hedtech-message')

        //check pagination headers
        "1" == responseHeader('X-hedtech-totalCount')
        "0" == responseHeader('X-hedtech-pageOffset')
        "1" == responseHeader('X-hedtech-pageMaxSize')
    }

    def "Test list filtering with bad date returns 400"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:"list with application/json accept"
        get("$localBase/api/things?filter[0][field]=dateManufactured&filter[0][operator]=lt&filter[0][value]=not_a_date&filter[0][type]=date&max=1") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }

        then:
        400 == response.status
        'application/json' == response.contentType

        'Validation failed' == responseHeader("X-Status-Reason")
    }

    def "Test list filtering using POST with xml"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:"list with application/json accept"
        post("$localBase/qapi/things") {
            headers['Content-Type'] = 'application/xml'
            headers['Accept']       = 'application/json'
                body {
                // akin to ?filter[0][field]=code&filter[0][operator]=eq&filter[0][value]=BB&max=1
                """<filters map="true">
                    <entry key="filter[0][field]">code</entry>
                    <entry key="filter[0][operator]">eq</entry>
                    <entry key="filter[0][value]">BB</entry>
                    <entry key="max">1</entry>
                </filters>
                """
            }
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        1 == json.size()
        "BB" == json[0].code
        "An BB thing" == json[0].description

        // assert localization of the message
        "List of thing resources" == responseHeader('X-hedtech-message')

        //check pagination headers
        "1" == responseHeader('X-hedtech-totalCount')
        "0" == responseHeader('X-hedtech-pageOffset')
        "1" == responseHeader('X-hedtech-pageMaxSize')
    }

    def "Test list with xml response"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:"list with accept application/xml"
        get( "$localBase/api/things?sort=code" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/xml'
        }
        def list = XML.parse response.text

        then:
        200 == response.status
        'application/xml' == response.contentType

        //check pagination headers
        "2"  == responseHeader('X-hedtech-totalCount')
        null != responseHeader('X-hedtech-pageOffset')
        null != responseHeader('X-hedtech-pageMaxSize')

        2                         == list.thing.size()
        "AA"                      == list.thing[0].code.text()
        "An AA thing"             == list.thing[0].description.text()
        "List of thing resources" == responseHeader('X-hedtech-message')

        list.thing[0]._href[0].text().contains('things')
        0 == list.thing[0].numParts.size()
        0 == list.thing[0].sha1.size()
    }

    def "Test list with version 0 json response"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:
        get( "$localBase/api/things?sort=code" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/vnd.hedtech.v0+json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        'application/vnd.hedtech.v0+json' ==  responseHeader("X-hedtech-Media-Type")

        def json = JSON.parse response.text

        // Assert the 'numParts' property is present proving the
        // resource-specific marshaller registered for the 'application/vnd.hedtech.v0+json'
        // configuration was used.
        //
        2 == json[0].numParts
        null != json[0].sha1

        "AA" == json[0].code
        "An AA thing" == json[0].description
    }

    def "Test list when resource is not recognized"() {

        when:"list with application/json accept"
        get("$localBase/api/unknown-things") {
            headers['Accept']       = 'application/json'
        }

        then:
        404 == response.status
        "Unsupported resource 'unknown-things'" == responseHeader('X-hedtech-message')
    }

    def "Test list when Content-Type is not recognized"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:"list with  application/json accept"
        get("$localBase/api/things") {
            headers['Content-Type'] = 'application/something_unknown'
            headers['Accept']       = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        2 == json.size()
    }

    def "Test list with version 0 xml response"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:
        get( "$localBase/api/things?sort=code" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/vnd.hedtech.v0+xml'
        }

        then:
        200 == response.status
        'application/xml' == response.contentType
        'application/vnd.hedtech.v0+xml' == responseHeader("X-hedtech-Media-Type")

        def xml = XML.parse response.text
        2             == xml.children().size()
        "AA"          == xml.thing[0].code.text()
        "An AA thing" == xml.thing[0].description.text()

        // assert localization of the message
        "List of thing resources" == responseHeader('X-hedtech-message')

        '2'  == xml.thing[0].numParts.text()
        null != xml.thing[0].sha1.text()
    }

    def "Test list of part-of-thing with json response when not nested"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:"list with application/json accept"
        get("$localBase/api/part-of-things?sort=code") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        4 == json.size()

        "aa" == json[0].code
        "aa part" == json[0].description

        // assert localization of the message
        "List of part-of-thing resources" == responseHeader('X-hedtech-message')

        //check pagination headers
        "4" == responseHeader('X-hedtech-totalCount')
        null != responseHeader('X-hedtech-pageOffset')
        null != responseHeader('X-hedtech-pageMaxSize')
        json[0]._href?.contains('part-of-things')
    }

    def "Test list of nested resource with json response"() {
        setup:
        def parentId = createThing('AA')
        createThing('BB')

        when:"list of nested resource with application/json accept"
        get("$localBase/api/things/$parentId/part-of-things?sort=code") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        2 == json.size()

        "aa" == json[0].code
        "aa part" == json[0].description

        // assert localization of the message
        "List of part-of-thing resources" == responseHeader('X-hedtech-message')

        //check pagination headers
        "2" == responseHeader('X-hedtech-totalCount')
        null != responseHeader('X-hedtech-pageOffset')
        null != responseHeader('X-hedtech-pageMaxSize')
        json[0]._href?.contains('part-of-things')
    }

    @Unroll
    def "Test list paging with json response"( int max, int offset, int numReturned ) {
        setup:
        def totalNumber = 95
        createManyThings(totalNumber)

        when:"list with application/json accept"
        get("$localBase/api/things?max=$max&offset=$offset") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        numReturned == json.size()

        // assert localization of the message
        "List of thing resources" == responseHeader('X-hedtech-message')

        //check pagination headers
        totalNumber == responseHeader('X-hedtech-totalCount').toInteger()
        offset      == responseHeader('X-hedtech-pageOffset').toInteger()
        max         == responseHeader('X-hedtech-pageMaxSize').toInteger()

        where:
        max | offset | numReturned
        10  | 0      | 10
        10  | 10     | 10
        10  | 90     | 5
    }

    def "Test list with matching Etag results in 304"() {
        setup:
        createThing('AA')
        createThing('BB')

        get("$localBase/api/things") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }

        def etagHeader = responseHeader('Etag')

        when:"list with  application/json accept"
        get("$localBase/api/things") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            headers['If-None-Match'] = etagHeader
        }

        then:
        304 == response.status
    }

    def "Test validation error"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:
        get( "$localBase/api/things/?forceValidationError=y" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }

        then:
        400 == response.status
        'application/json' == response.contentType

        'Validation failed' == responseHeader("X-Status-Reason")

        // assert localization of the message
        "thing resource had validation errors" == responseHeader('X-hedtech-message')

        def json = JSON.parse response.text
        1 == json.errors.size()
        "validation" == json.errors[0].type
        null != json.errors[0].errorMessage
    }

    def "Test show when resource is not recognized"() {

        when:"show with application/json accept"
        get("$localBase/api/unknown-things/123") {
            headers['Accept']       = 'application/json'
        }

        then:
        404 == response.status
        "Unsupported resource 'unknown-things'" == responseHeader('X-hedtech-message')
    }

    def "Test show thing with json response"() {
        setup:
        def id = createThing('AA')
        createThing('BB')

        when:
        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType

        def json = JSON.parse response.text
        "AA" == json.code
        "An AA thing" == json.description
        json._href?.contains('things')
        null == json.numParts
        null == json.sha1
        null != json.version

        // assert localization of the message
         "Details for the thing resource" == responseHeader('X-hedtech-message')
    }

    def "Test ability to use a service-specific service adapter"() {
        setup:
        def id = createThing('AA')
        createThing('BB')

        when:
        get( "$localBase/api/nothings/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType

        def json = JSON.parse response.text
        "AA" == json.code
        "Modified by the NothingServiceAdapter" == json.description
         "Details for the nothing resource" == responseHeader('X-hedtech-message')
    }


    def "Test show thing with xml response"() {
        setup:
        def id = createThing('AA')
        createThing('BB')

        when:
        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/xml'
        }

        then:
        200 == response.status
        'application/xml' == response.contentType

        def xml = XML.parse response.text
        "AA" == xml.code[0].text()
        "An AA thing" == xml.description[0].text()
        xml._href[0]?.text().contains('things')
        xml.numParts.isEmpty()
        xml.sha1.isEmpty()
        null != xml.version[0].text()

        // assert localization of the message
         "Details for the thing resource" == responseHeader('X-hedtech-message')
    }

    def "Test show thing with version 0 json response"() {
        setup:
        def id = createThing('AA')
        createThing('BB')

        when:
        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/vnd.hedtech.v0+json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
         'application/vnd.hedtech.v0+json' == responseHeader("X-hedtech-Media-Type")

        def json = JSON.parse response.text
        "AA" == json.code
        "An AA thing" == json.description
        2 == json.numParts
        null != json.version
    }

    def "Test show thing with xml version 0"() {
        setup:
        def id = createThing('AA')
        createThing('BB')

        when:
        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/vnd.hedtech.v0+xml'
        }

        then:
        200 == response.status
        'application/xml' == response.contentType
        'application/vnd.hedtech.v0+xml' == responseHeader("X-hedtech-Media-Type")

        def xml = XML.parse response.text
        "AA" == xml.code[0].text()
        "An AA thing" == xml.description[0].text()
        '2' == xml.numParts[0].text()
        null != xml.version[0].text()
    }

    def "Test show when Content-Type is not recognized"() {
        setup:
        def id = createThing('AA')
        createThing('BB')

        when:
        get( "$localBase/api/things/$id" ) {
            headers['Accept']        = 'application/json'
            headers['Content-Type']  = 'application/something_unknown'
        }

        then:
        200 == response.status
        'application/json' == response.contentType

        def json = JSON.parse response.text
        "AA" == json.code
    }

    def "Test show with same etag results in 304"() {
        setup:
        def id = createThing('AA')

        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']   = 'application/json'
            headers['Accept']         = 'application/json'
        }

        def etagHeader = responseHeader('Etag')

        when:
        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
            headers['If-None-Match'] = etagHeader
        }

        then:
        304 == response.status
    }

    def "Test show with model-created etag that matches results in 304"() {
        setup:
        def thingId = createThing('AA')
        def id = Thing.get( thingId ).parts.toArray()[0].id
        get( "$localBase/api/part-of-things/$id" ) {
            headers['Content-Type']   = 'application/json'
            headers['Accept']         = 'application/json'
        }
        def etagHeader = responseHeader('Etag')

        when:
        get( "$localBase/api/part-of-things/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
            headers['If-None-Match'] = etagHeader
        }

        then:
        null != etagHeader
        304 == response.status
    }

    def "Test show with UUID-based etag results in 200"() {
        setup:
        get( "$localBase/api/thing-wrappers/1" ) {
            headers['Content-Type']   = 'application/json'
            headers['Accept']         = 'application/json'
        }
        def etagHeader = responseHeader('Etag')

        when:
        get( "$localBase/api/thing-wrappers/1" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
            headers['If-None-Match'] = etagHeader
        }

        then:
        null != etagHeader
        200 == response.status
    }

    def "Test show with different etag results in 200"() {
        setup:
        def id = createThing('AA')

        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']   = 'application/json'
            headers['Accept']         = 'application/json'
        }

        when:
        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
            headers['If-None-Match'] = '5f167d1ef1ccNO_MATCH2b29a610d3cef0fc793e8'
        }

        then:
        200 == response.status
    }

    def "Test show of unchanged resource with If-Modified-Since header results in 304"() {
        setup:
        def id = createThing('AA')

        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']   = 'application/json'
            headers['Accept']         = 'application/json'
        }

        def lastModifiedHeader = responseHeader('Last-Modified')

        when:
        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']      = 'application/json'
            headers['Accept']            = 'application/json'
            headers['If-Modified-Since'] = lastModifiedHeader
        }

        then:
        304 == response.status
    }

    def "Test show of modified resource with 'old' If-Modified-Since header results in 200"() {
        setup:
        def id = createThing('AA')

        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']   = 'application/json'
            headers['Accept']         = 'application/json'
        }

        def lastModifiedHeader = responseHeader('Last-Modified')
        sleep 1000

        put( "$localBase/api/things/$id" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "description": "updated description",
                    "version": "0",
                }
                """
            }
        }

        when:
        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']      = 'application/json'
            headers['Accept']            = 'application/json'
            headers['If-Modified-Since'] = lastModifiedHeader
        }

        then:
        200 == response.status
        def json = JSON.parse response.text
        1 == json.version
    }

    def "Test create when resource is not recognized"() {

        when:"create with application/json accept"
        post("$localBase/api/unknown-things") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "code": "NO",
                    "description": "You cannot find me!"
                }
                """
            }
        }

        then:
        404 == response.status
        "Unsupported resource 'unknown-things'" == responseHeader('X-hedtech-message')
    }

    def "Test save as json"() {
        setup:
        def someDate = new Date().format("yyyy-MM-dd'T'HH:mm:ssZ")

        when:
        post( "$localBase/api/things") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "code": "AC",
                    "description": "An AC thingy",
                    "dateManufactured": "$someDate"
                }
                """
            }
        }

        then:
        201 == response.status

        'application/json' == response.contentType

        // assert localization of the message
        "thing resource created" == responseHeader('X-hedtech-message')
        def json = JSON.parse response.text
        null != json.id
        "AC" == json.code
        "An AC thingy" == json.description
        0 == json.parts.size()
        null != json.version
    }

    def "Test create nested resource as json"() {
        setup:
        def parentId = createThing('AA')
        def someDate = new Date().format("yyyy-MM-dd'T'HH:mm:ssZ")

        when:
        post( "$localBase/api/things/$parentId/part-of-things" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "code": "pp",
                    "description": "Part of AA",
                    "thing": { "_link": "/things/$parentId" }
                }
                """
            }
        }

        then:
        201 == response.status

        'application/json' == response.contentType

        // assert localization of the message
        "part-of-thing resource created" == responseHeader('X-hedtech-message')
        def json = JSON.parse response.text
        null != json.id
        "pp" == json.code
        "Part of AA" == json.description
        "[_link:/things/$parentId]" as String == json.thing as String
        null != json.version
    }

    def "Test saving json and getting version 0 response"() {

        when:
        post( "$localBase/api/things") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/vnd.hedtech.v0+json'
            body {
                """
                {
                    "code": "AD",
                    "description": "An AD thingy",
                }
                """
            }
        }

        then:
        201 == response.status
        'application/json' == response.contentType

        'application/vnd.hedtech.v0+json' == responseHeader("X-Hedtech-Media-Type")

        def json = JSON.parse response.text
        null != json.id
        "AD" == json.code
        "An AD thingy" == json.description
        0 == json.numParts
        null != json.version
    }

    def "Test saving xml"() {
        when:
        post( "$localBase/api/things") {
            headers['Content-Type'] = 'application/xml'
            headers['Accept']       = 'application/xml'
            body {
                """<?xml version="1.0" encoding="UTF-8"?>
                <json>
                    <code>AC</code>
                    <description>An AC thingy</description>
                </json>
                """
            }
        }

        then:
        201 == response.status
        'application/xml' == response.contentType

        def xml = XML.parse response.text

        null != xml.id[0].text()
        "AC" == xml.code[0].text()
        "An AC thingy" == xml.description[0].text()
        null != xml.version[0].text()
        xml.version[0].text().length() > 0
    }

    def "Test appropriate validation error when saving an existing thing"() {
        setup:
        createThing('AA')

        when:
        post( "$localBase/api/things" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "code": "AA",
                    "description": "An AA thingy",
                }
                """
            }
        }

        then:
        400 == response.status
        'Validation failed' == responseHeader("X-Status-Reason")

        def json = JSON.parse response.text
        1 == json.errors.size()
        "validation" == json.errors[0].type
        null != json.errors[0].errorMessage
    }

    def "Test that an unknown, non-application exception results in a 500 status"() {
        when:
        post( "$localBase/api/things?forceGenericError=y") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "code": "AA",
                    "description": "An AA thingy",
                }
                """
            }
        }

        then:
        500 == response.status
        def json = JSON.parse response.text
        1 == json.errors.size()
        "general" == json.errors[0].type
        null != json.errors[0].errorMessage
    }

    def "Test updating when the resource is not recognized"() {

        when:"create with application/json accept"
        put("$localBase/api/unknown-things/123") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "description": "You cannot find me!"
                }
                """
            }
        }

        then:
        404 == response.status
        "Unsupported resource 'unknown-things'" == responseHeader('X-hedtech-message')
    }

    def "Test updating a thing with json"() {
        setup:
        def id = createThing('AA')

        when:
        put( "$localBase/api/things/$id" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "description": "updated description",
                    "version": "0"
                }
                """
            }
        }

        then:
        200 == response.status

        // assert localization of the message
        "thing resource updated" == responseHeader('X-hedtech-message')

        def json = JSON.parse response.text
        null != json.id
        null != json.version
        json.version > 0
        "updated description" == json.description
        null == json.numParts
        2 == json.parts.size()
    }

    def "Test updating thing with json and getting version 0 json back"() {
        setup:
        def id = createThing('AA')

        when:
        put( "$localBase/api/things/$id" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/vnd.hedtech.v0+json'
            body {
                """
                {
                    "description": "updated description",
                    "version": "0"
                }
                """
            }
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        'application/vnd.hedtech.v0+json' == responseHeader("X-hedtech-Media-Type")
        def json = JSON.parse response.text
        null != json.id
        null != json.version
        json.version > 0
        "updated description" == json.description
        2 == json.numParts
        2 == json.parts.size()
    }

    void "Test that optimistic lock returns 409 status"() {
        setup:
        def id = createThing('AA')
        updateThing( id, [description:'changed'] )

        when:
        put( "$localBase/api/things/$id" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/vnd.hedtech.v0+json'
            body {
                """
                {
                    "description": "updated description",
                    "version": "0"
                }
                """
            }
        }

        then:
        409 == response.status
        // assert localization of the message
        "Another user has updated this thing while you were editing" == responseHeader('X-hedtech-message')
        "" == response.text
    }

    void "Test that optimistic lock returns 409 status, even with a conditional request"() {
        // Currently, conditional PUT requests are not supported. Consequently, instead of
        // returning a '412 Precondition Failure' a 'normal' '409 Conflict' error will be returned.
        // This test simply shows that including the 'If-Match' header has no affect.
        setup:
        def id = createThing('AA')
        updateThing( id, [description:'changed'] )

        when:
        put( "$localBase/api/things/$id" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/vnd.hedtech.v0+json'
            headers['If-Match']     = '5f167d1ef1ccNO_MATCH2b29a610d3cef0fc793e8'
            body {
                """
                {
                    "description": "updated description",
                    "version": "0"
                }
                """
            }
        }

        then:
        409 == response.status
    }

    def "Test deleting a thing"() {
        setup:
        def id = createThing('AA')

        when:
        delete( "$localBase/api/things/$id" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }
        def count
        Thing.withNewSession{
            count = Thing.findAll().size()
        }

        then:
        200 == response.status
        // assert localization of the message
        "thing resource deleted" == responseHeader('X-hedtech-message')
        0 == count
    }

    def "Test that an OptimisticLockingFailureException returns a 409 status"() {
        when:
        put( "$localBase/api/things/1?throwOptimisticLock=y" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/vnd.hedtech.v0+json'
            body {
                """
                {
                    "description": "updated description",
                    "version": "0"
                }
                """
            }
        }

        then:
        409 == response.status
        // assert localization of the message
        "Another user has updated this thing while you were editing" == responseHeader('X-hedtech-message')
        "" == response.text
    }

    def "Test optimistic lock when asking for xml response"() {
        setup:
        def id = createThing('AA')
        updateThing( id, [description:'changed'] )

        when:
        put( "$localBase/api/things/$id" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/xml'
            body {
                """
                {
                    "description": "updated description",
                    "version": "0"
                }
                """
            }
        }

        then:
        409 == response.status
        "Another user has updated this thing while you were editing" == responseHeader('X-hedtech-message')
        "" == response.text
    }

    def "Test delegating to ApplicationException for validation errors"() {
        when:
        put( "$localBase/api/things/1?throwApplicationException=y&appStatusCode=400&appMsgCode=testapp.application.exception.message&appErrorType=validation" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "description": "updated description",
                    "version": "0"
                }
                """
            }
        }

        then:
        400 == response.status
        'Validation failed' == responseHeader("X-Status-Reason")
        // assert localization of the message
        "foo resource had errors" == responseHeader('X-hedtech-message')
        def json = JSON.parse response.text
        null != json.errors
        1 == json.errors.size()
    }

    def "Test delegating to ApplicationException for validation errors with xml response"() {
        when:
        put( "$localBase/api/things/1?throwApplicationException=y&appStatusCode=400&appMsgCode=testapp.application.exception.message&appErrorType=validation" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/xml'
            body {
                """
                {
                    "description": "updated description",
                    "version": "0"
                }
                """
            }
        }
        def xml = XML.parse response.text

        then:
        400                       == response.status
        'Validation failed'       == responseHeader("X-Status-Reason")
        // assert localization of the message
        "foo resource had errors" == responseHeader('X-hedtech-message')
        1                         == xml.entry.size()
        "errors"                  == xml.entry.'@key'.text()
    }

    def "Test delegating to an application exception that generates no message or error block"() {
        when:
        put( "$localBase/api/things/1?throwApplicationException=y&appStatusCode=409" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "description": "updated description",
                    "version": "0"
                }
                """
            }
        }

        then:
        409 == response.status

        //should be no message header in this case
        null == responseHeader('X-hedtech-message')
        def json = JSON.parse response.text
        null == json.errors
    }

    def "Test programming error while handling an application exception"() {
        when:
        put( "$localBase/api/things/1?throwApplicationException=y&appStatusCode=409&appErrorType=programming" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "description": "updated description",
                    "version": "0"
                }
                """
            }
        }

        then:
        500 == response.status
        // assert localization of the message
        "Encountered unexpected error generating a response" == responseHeader('X-hedtech-message')
        "" == response.text
    }

    def "Test programming error while handling an application exception when xml is requested"() {
        when:
        put( "$localBase/api/things/1?throwApplicationException=y&appStatusCode=409&appErrorType=programming" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/xml'
            body {
                """
                {
                    "description": "updated description",
                    "version": "0"
                }
                """
            }
        }

        then:
        500 == response.status
        // assert localization of the message
        "Encountered unexpected error generating a response" == responseHeader('X-hedtech-message')
        "" == response.text
    }


    def "Test saving a thing with version 1 json representation"() {
        when:
        post( "$localBase/api/things") {
            headers['Content-Type'] = 'application/vnd.hedtech.v1+json'
            headers['Accept']       = 'application/vnd.hedtech.v1+json'
            body {
                """
                {
                    "code": "AC",
                }
                """
            }
        }

        then:
        201 == response.status
        'application/json' == response.contentType

        def json = JSON.parse response.text
        null != json.id
        "AC" == json.code
        "Default description" == json.description
        0 == json.parts.size()
        // null != json.version
    }

    def "Test saving a thing with version 2 json representation"() {
        when:
        post( "$localBase/api/things") {
            headers['Content-Type'] = 'application/vnd.hedtech.v2+json'
            headers['Accept']       = 'application/vnd.hedtech.v2+json'
            body {
                """
                {
                    "code": "AC",
                }
                """
            }
        }

        then:
        201 == response.status
        'application/json' == response.contentType

        def json = JSON.parse response.text
        null != json.id
        "AC" == json.code
        "Default description" == json.description
        0 == json.parts.size()
        // null != json.version
    }

    def "Test saving a thing with version 1 json representation"() {
        when:
        post( "$localBase/api/things") {
            headers['Content-Type'] = 'application/vnd.hedtech.v1+xml'
            headers['Accept']       = 'application/json'
            body {
                """<?xml version="1.0" encoding="UTF-8"?>
                <thing>
                    <code>AC</code>
                    <description>a thing</description>
                </thing>
                """
            }
        }

        then:
        201 == response.status

        'application/json' == response.contentType

        def json = JSON.parse response.text
        null != json.id
        "AC" == json.code
        "a thing" == json.description
        0 == json.parts.size()
        null != json.version
    }

    def "Test that a 415 status is returned if the content type of a json request is not supported"() {
        when:
        post( "$localBase/api/things" ) {
            headers['Content-Type'] = 'application/vnd.hedtech.no_extractor+json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    code:"AA",
                    description:"thing"
                }
                """
            }
        }

        then:
        415 == response.status

        // assert localization of the message
         "Unsupported media type 'application/vnd.hedtech.no_extractor+json' for resource 'things'" == responseHeader('X-hedtech-message')

        "" == response.text
    }

    def "Test that a 415 status is returned if the content type of a xml request is not supported"() {
        when:
        post( "$localBase/api/things" ) {
            headers['Content-Type'] = 'application/vnd.hedtech.no_extractor+xml'
            headers['Accept']       = 'application/xml'
            body {
                """<?xml version="1.0" encoding="UTF-8"?>
                <json>
                    <code>AC</code>
                    <description>An AC thingy</description>
                </json>
                """
            }
        }

        then:
        415 == response.status
        // assert localization of the message
        "Unsupported media type 'application/vnd.hedtech.no_extractor+xml' for resource 'things'" == responseHeader('X-hedtech-message')
        "" == response.text
    }

    def "Test that a 406 status is returned if the accept type is an unsupported xml type"() {
        when:
        post( "$localBase/api/things" ) {
            headers['Content-Type'] = 'application/xml'
            headers['Accept']       = 'application/vnd.hedtech.no_such_type+xml'
            body {
                """<?xml version="1.0" encoding="UTF-8"?>
                <json>
                    <code>AC</code>
                    <description>An AC thingy</description>
                </json>
                """
            }
        }

        then:
        406 == response.status
        'application/xml' == response.contentType

        // assert localization of the message
        "Unsupported media type 'application/vnd.hedtech.no_such_type+xml' for resource 'things'" == responseHeader('X-hedtech-message')
        "" == response.text
    }

    def "Test that a 406 status is returned if the accept type is an unsupported json type"() {
        when:
        post( "$localBase/api/things" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/vnd.hedtech.no_such_type+json'
            body {
                """
                {
                    code:"AA",
                    description:"thing"
                }
                """
            }
        }

        then:
        406 == response.status
        'application/json' == response.contentType

        // assert localization of the message
        "Unsupported media type 'application/vnd.hedtech.no_such_type+json' for resource 'things'" == responseHeader('X-hedtech-message')
        "" == response.text
    }

    def "Test custom xml marshalling version 0"() {
        setup:
        def id = createThing('AA')

        when:
        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/xml'
            headers['Accept']        = 'application/vnd.hedtech.thing.v0+xml'
        }

        then:
        200 == response.status
        'application/xml' == response.contentType
        'application/vnd.hedtech.thing.v0+xml' == responseHeader("X-hedtech-Media-Type")
        "Details for the thing resource" == responseHeader('X-hedtech-message')
        def xml = XML.parse response.text
        id.toString() == xml.@id.text()
        0 < xml.@version.text().length()
        "AA" == xml.code.text()
        "An AA thing" == xml.description.text()
        0 == xml.parts.size()
    }

    def "Test custom xml marshalling version 1"() {
        setup:
        def id = createThing('AA')

        when:
        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/xml'
            headers['Accept']        = 'application/vnd.hedtech.thing.v1+xml'
        }

        then:
        200 == response.status
        'application/xml' == response.contentType
        'application/vnd.hedtech.thing.v1+xml' == responseHeader("X-hedtech-Media-Type")
        "Details for the thing resource" == responseHeader('X-hedtech-message')
        def xml = XML.parse response.text
        id.toString() == xml.@id.text()
        0 < xml.@version.text().length()
        "AA" == xml.code.text()
        "An AA thing" == xml.description.text()
        2 == xml.parts.part.size()
    }

    def "Test version 0 custom xml extractor"() {
        when:
        post( "$localBase/api/things" ) {
            body {
                headers['Content-Type']  = 'application/vnd.hedtech.thing.v0+xml'
                headers['Accept']        = 'application/vnd.hedtech.thing.v0+xml'
                """<?xml version="1.0" encoding="UTF-8"?>
                   <Thing><code>AA</code><description>An AA thing</description></Thing>
                """
            }
        }

        then:
        201 == response.status
        'application/xml' == response.contentType
        'application/vnd.hedtech.thing.v0+xml' == responseHeader("X-hedtech-Media-Type")
        "thing resource created" == responseHeader('X-hedtech-message')
        def xml = XML.parse response.text
        null != xml.@id.text()
        0 < xml.@version.text().length()
        "AA" == xml.code.text()
        "An AA thing" == xml.description.text()
        0 == xml.parts.part.size()
    }

    def "Test version 1 custom xml extractor"() {

        when:
        post( "$localBase/api/things" ) {
            body {
                headers['Content-Type']  = 'application/vnd.hedtech.thing.v1+xml'
                headers['Accept']        = 'application/vnd.hedtech.thing.v1+xml'
                """|<?xml version="1.0" encoding="UTF-8"?>
                   |<Thing><code>AA</code><description>An AA thing</description>
                   |<parts>
                   |<part><code>xx</code><description>xx part</description></part>
                   |<part><code>yy</code><description>yy part</description></part>
                   |</parts>
                   |</Thing>
                """.stripMargin()
            }
        }

        then:
        201 == response.status
        'application/xml' == response.contentType
        'application/vnd.hedtech.thing.v1+xml' == responseHeader("X-hedtech-Media-Type")
        "thing resource created" == responseHeader('X-hedtech-message')
        def xml = XML.parse response.text
        null != xml.@id.text()
        0 < xml.@version.text().length()
        "AA" == xml.code.text()
        "An AA thing" == xml.description.text()
        2 == xml.parts.part.size()
    }

    def "Test that mismatch between id in url and resource representation for update returns 400"() {
        when:
        put( "$localBase/api/things/1") {
            body {
                headers['Content-Type'] = 'application/json'
                headers['Accept']       = 'application/json'
                """{id:2}"""
            }
        }

        then:
        400 == response.status
         '' == response.text
        'Id mismatch' == responseHeader( 'X-Status-Reason' )
        "Id in representation for resource 'things' did not match id of resource" == responseHeader( 'X-hedtech-message' )
    }

    @Unroll
    def "Test 405 response when a method is not supported on a resource"() {
        setup:
        def aaID = createThing('AA')
        createThing('BB')
        def url = id ? "$localBase/api/limitedthings/$aaID" : "$localBase/api/limitedthings"


        when:
        "$method"("$url") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            if(data) {
                body {
                    "$data"
                }
            }
        }

        then:
        status == response.status
        'application/json' == response.contentType
        // assert localization of the message
        null != responseHeader('X-hedtech-message')
        allowHeader == responseHeaders('Allow')

        where:
        method     | id    | status | allowHeader   | data
        'get'      | false | 405    | ['POST']      | null
        'get'      | true  | 200    | null          | null
        'post'     | false | 201    | null          | "{code:'ZZ',description:'ZZ thing'}"
        'put'      | true  | 200    | null          | "{description:'changed',version:'0'}"
        'delete'   | true  | 405    | ['GET','PUT'] | null

    }

    @Unroll
    def "Test overriding service spec for all operations"(def method, boolean id, def status, def data) {
        setup:
        def aaID = createThing('AA')
        createThing('BB')
        def url = id ? "$localBase/api/thingamabobs/$aaID" : "$localBase/api/thingamabobs"
        def expectedType = method == 'delete' ? 'text/plain' : 'application/json'


        when:
        "$method"("$url") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            if(data) {
                body {
                    "$data"
                }
            }
        }

        then:
        status       == response.status
        expectedType == response.contentType
        // assert localization of the message
        null != responseHeader('X-hedtech-message')
        responseHeader('X-hedtech-message').contains( 'thingamabob' )

        where:
        method     | id    | status | data
        'get'      | false | 200    | null
        'get'      | true  | 200    | null
        'post'     | false | 201    | "{code:'ZZ',description:'ZZ thing'}"
        'put'      | true  | 200    | "{description:'changed',version:'0'}"
        'delete'   | true  | 200    | null
    }

    def "Test supplemental data from declarative marshalling"() {
        setup:
        def id = createThing('AA')

        when:
        get("$localBase/api/things/$id") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/vnd.hedtech.additional.field.closure+json'
        }
        def json = JSON.parse response.text

        then:
        200                       == response.status
        'application/json'        == response.contentType
        "AA"                      == json.code
        "An AA thing"             == json.description
        null                      != json.numParts
        null                      != json.sha1
    }

    def "Test that affordances can be correctly generated when the domain name does not match the resource name"() {
        setup:
        def id = createThing('AA')

        when:
        get("$localBase/api/special-things/$id") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }
        def json = JSON.parse response.text

        then:
        "/special-things/$id" == json._href
    }

    def "Test that resource name can be overridden for short objects"() {
        setup:
        def id = createThing('AA')

        when:
        get("$localBase/api/special-things/$id") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }
        def json = JSON.parse response.text

        then:
        2 == json.parts.size()
        json.parts.each {
            it._link.startsWith('/thing-parts')
        }
    }

    def "Test using closures to controll marshalling of dates"() {
        setup:
        def id = createThing('AA')

        when:
        get("$localBase/api/closure-things/$id") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }
        def json = JSON.parse response.text

        then:
        json.dateManufactured.startsWith('customized-date:')
    }

    def "Test groovy bean marshalling with domain fields"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:
        get("$localBase/api/groovy-thing-wrappers") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }
        def json = JSON.parse response.text

        then:
        2 == json.size()
        3 == json[0].keySet().size()
        json[0].containsKey('things')
        json[0].containsKey('complexCode')
        json[0].containsKey('xlarge')
    }

    def "Test marshalling a list with a custom marshaller service (non-grails framework)"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:
        get("$localBase/api/things") {
            headers['Content-Type'] = 'application/vnd.hedtech.custom-framework+xml'
            headers['Accept']       = 'application/vnd.hedtech.custom-framework+xml'
        }
        def xml = XML.parse response.text

        then:
        'list' == xml.name()
        2 == xml.children().size()
        'AA' == xml.code[0].text()
        'BB' == xml.code[1].text()
    }

    def "Test marshalling a list with a custom marshaller service that returns byte[]"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:
        get("$localBase/api/things") {
            responseType byte[]
            headers['Content-Type'] = 'application/vnd.hedtech.custom-framework+xml+zip'
            headers['Accept']       = 'application/vnd.hedtech.custom-framework+xml+zip'
        }
        def zipStream = new ZipInputStream(new ByteArrayInputStream(response.responseEntity.getBody()))
        def entry = zipStream.getNextEntry()
        byte[] buffer = new byte[1000]
        int length = zipStream.read(buffer, 0, 1000)
        String s = new String(buffer, 0, length, 'UTF-8')
        def xml = XML.parse(s)

        then:
        null   != responseHeader('Content-Length')
        'list' == xml.name()
        2      == xml.children().size()
        'AA'   == xml.code[0].text()
        'BB'   == xml.code[1].text()
    }

    def "Test marshalling a list with a custom marshaller service that returns InputStream"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:
        get("$localBase/api/things") {
            responseType byte[]
            headers['Content-Type'] = 'application/vnd.hedtech.custom-framework+xml+stream'
            headers['Accept']       = 'application/vnd.hedtech.custom-framework+xml+stream'
        }
        def zipStream = new ZipInputStream(new ByteArrayInputStream(response.responseEntity.getBody()))
        def entry = zipStream.getNextEntry()
        byte[] buffer = new byte[1000]
        int length = zipStream.read(buffer, 0, 1000)
        String s = new String(buffer, 0, length, 'UTF-8')
        def xml = XML.parse(s)

        then:
        null   == responseHeader('Content-Length')
        'list' == xml.name()
        2      == xml.children().size()
        'AA'   == xml.code[0].text()
        'BB'   == xml.code[1].text()
    }

    def "Test marshalling a list with a custom marshaller service that returns InputStream"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:
        get("$localBase/api/things") {
            responseType byte[]
            headers['Content-Type'] = 'application/vnd.hedtech.custom-framework+xml+streamsize'
            headers['Accept']       = 'application/vnd.hedtech.custom-framework+xml+streamsize'
        }
        def zipStream = new ZipInputStream(new ByteArrayInputStream(response.responseEntity.getBody()))
        def entry = zipStream.getNextEntry()
        byte[] buffer = new byte[1000]
        int length = zipStream.read(buffer, 0, 1000)
        String s = new String(buffer, 0, length, 'UTF-8')
        def xml = XML.parse(s)

        then:
        null   != responseHeader('Content-Length')
        'list' == xml.name()
        2      == xml.children().size()
        'AA'   == xml.code[0].text()
        'BB'   == xml.code[1].text()
    }


    def "Test marshalling an object with a custom marshaller service (non-grails framework)"() {
        setup:
        def id = createThing('AA')

        when:
        get("$localBase/api/things/$id") {
            headers['Content-Type'] = 'application/vnd.hedtech.custom-framework+xml'
            headers['Accept']       = 'application/vnd.hedtech.custom-framework+xml'
        }
        def xml = XML.parse response.text

        then:
        'thing' == xml.name()
        'AA' == xml.code.text()
    }


    def "Test that a URL part may be used for tenant identification"() {
        // Note: The plugin does not deal with tenants - this simply shows the
        //       plugin does not preclude use of URL parts for identifying tenants.
        setup:
        def id = createThing('AA')

        when:
        get("$localBase/test/api/things/$id") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/vnd.hedtech.additional.field.closure+json'
        }
        def json = JSON.parse response.text

        then:
        200                       == response.status
        'application/json'        == response.contentType
        "AA"                      == json.code
        "An AA thing"             == json.description
        null                      != json.numParts
        null                      != json.sha1
        "test"                    == json.tenant
    }

    def "Test that a query parameter may be used for tenant identification"() {
        // Note: The plugin does not deal with tenants - this simply shows the
        //       plugin does not preclude use of URL parts for identifying tenants.
        setup:
        def id = createThing('AA')

        when:
        get("$localBase/api/things/$id?tenant=dev") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/vnd.hedtech.additional.field.closure+json'
        }
        def json = JSON.parse response.text

        then:
        200                       == response.status
        'application/json'        == response.contentType
        "AA"                      == json.code
        "An AA thing"             == json.description
        null                      != json.numParts
        null                      != json.sha1
        "dev"                     == json.tenant
    }

    @Ignore // This test requires configuration on the test machine.
            // Specifically, edit the 'hosts' file to add 'test.local'
            // as an additional host mapped to 127.0.0.1 before re-enabling this test.
    def "Test tenant identification as a subdomain"() {
        // Note: The plugin does not deal with tenants - this simply shows the
        //       plugin does not preclude use of subdomains for identifying tenants.
        setup:
        def id = createThing('AA')

        when:
        get("http://test.local:8080/test-restful-api/api/things/$id") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/vnd.hedtech.additional.field.closure+json'
        }
        def json = JSON.parse response.text

        then:
        200                       == response.status
        'application/json'        == response.contentType
        "AA"                      == json.code
        "An AA thing"             == json.description
        null                      != json.numParts
        null                      != json.sha1
        "test"                    == json.tenant
    }


// ------------------------------- Helper Methods ------------------------------


    private void createThings() {
        createThing('AA')
        createThing('BB')

    }

    private void createManyThings(int num = 99) {
        assert num < 100 // index is used as 2 char code
        int index = 0
        num.times {
            String code = String.format("%02d", index++)
            createThing(code)
        }
    }

    private long createThing(String code) {
        Thing.withTransaction {
            Thing thing = new Thing(code: code, description: "An $code thing",
                      dateManufactured: new Date(), isGood: 'Y', isLarge: true)
                .addToParts(new PartOfThing(code: 'aa', description: 'aa part'))
                .addToParts(new PartOfThing(code: 'bb', description: 'bb part'))
                .save(failOnError:true, flush:true)
            thing.getId()
        }
    }

    private void updateThing( def id, def props ) {
        Thing.withNewSession {
            Thing.withTransaction {
                def thing = Thing.get( id )
                thing.properties = props
                thing.save(failOnError:true,flush:true)
            }
        }
    }

    private void deleteThings() {
        Thing.withNewSession{
            def things = Thing.findAll()
            things.each() { aThing ->
                aThing.delete(failOnError:true,flush:true)
            }
        }
    }

}
