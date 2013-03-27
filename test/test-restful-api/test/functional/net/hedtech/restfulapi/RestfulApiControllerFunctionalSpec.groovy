/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import net.hedtech.restfulapi.spock.*

import grails.test.mixin.*
import spock.lang.*
import grails.plugins.rest.client.*

import grails.converters.JSON
import grails.converters.XML

import net.hedtech.restfulapi.extractors.configuration.*

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

    def "Test list with json as xml response"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:"list with accept application/xml"
        get( "$localBase/api/things" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/xml'
        }


        then:
        200 == response.status
        'application/xml' == response.contentType

        //check pagination headers
        "2"  == responseHeader('X-hedtech-totalCount')
        null != responseHeader('X-hedtech-pageOffset')
        null != responseHeader('X-hedtech-pageMaxSize')

        def xml = XML.parse response.text
        2                         == xml.'net-hedtech-object'.size()
        "AA"                      == xml.'net-hedtech-object'[0].code[0].text()
        "An AA thing"             == xml.'net-hedtech-object'[0].description[0].text()
        "List of thing resources" == responseHeader('X-hedtech-message')

        xml.'net-hedtech-object'[0]._href[0].text().contains('things')
        0 == xml.'net-hedtech-object'[0].numParts.size()
        0 == xml.'net-hedtech-object'[0].sha1.size()
    }

    def "Test list with version 0 json response"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:
        get( "$localBase/api/things" ) {
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

    def "Test list with version 0 json-as-xml response"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:
        get( "$localBase/api/things" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/vnd.hedtech.v0+xml'
        }

        then:
        200 == response.status
        'application/xml' == response.contentType
        'application/vnd.hedtech.v0+xml' == responseHeader("X-hedtech-Media-Type")

        def xml = XML.parse response.text
        2 == xml.'net-hedtech-object'.size()
        "AA" == xml.'net-hedtech-object'[0].code[0].text()
        "An AA thing" == xml.'net-hedtech-object'[0].description[0].text()

        // assert localization of the message
        "List of thing resources" == responseHeader('X-hedtech-message')

        // Assert that an '_href' property is present but not a 'numParts'
        // property, which proves the plugin-registered marshaller is
        // being used versus the built-in grails marshaller or the
        // resource-specific marshaller registered by this test app.
        //
        xml.'net-hedtech-object'[0]._href[0].text().contains('things')
        '2' == xml.'net-hedtech-object'[0].numParts[0].text()
        null != xml.'net-hedtech-object'[0].sha1[0].text()
    }

    def "Test list of part-of-thing with json response when not nested"() {
        setup:
        createThing('AA')
        createThing('BB')

        when:"list with  application/json accept"
        get("$localBase/api/part-of-things") {
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
        "List of partOfThing resources" == responseHeader('X-hedtech-message')

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
        get("$localBase/api/things/$parentId/part-of-things") {
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
        "List of partOfThing resources" == responseHeader('X-hedtech-message')

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
        json.errors[0].resource.class == 'net.hedtech.restfulapi.Thing'
        null != json.errors[0].errorMessage
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

    def "Test show thing with json-as-xml response"() {
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
//            headers['Authorization'] = TestUtils.authHeader('user','password')
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

    def "Test show thing with json-as-xml version 0"() {
        setup:
        def id = createThing('AA')
        createThing('BB')

        when:
        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/vnd.hedtech.v0+xml'
//            headers['Authorization'] = TestUtils.authHeader('user','password')
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

    def "Test save as json"() {
        when:
        post( "$localBase/api/things") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "code": "AC",
                    "description": "An AC thingy"
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

    def "Test saving json-as-xml"() {
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
        json.errors[0].resource.class == 'net.hedtech.restfulapi.Thing'
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
        json.errors[0].resource.class == 'net.hedtech.restfulapi.Thing'
        null != json.errors[0].errorMessage
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

        then:
        400 == response.status
        'Validation failed' == responseHeader("X-Status-Reason")
        // assert localization of the message
        "foo resource had errors" == responseHeader('X-hedtech-message')

        def xml = XML.parse response.text
        1 == xml.errors.size()
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
            headers['Accept']       = 'application/json'
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
        null != json.version
    }

    def "Test saving a thing with version 1 json-as-xml representation"() {
        when:
        post( "$localBase/api/things") {
            headers['Content-Type'] = 'application/vnd.hedtech.v1+xml'
            headers['Accept']       = 'application/json'
            body {
                """<?xml version="1.0" encoding="UTF-8"?>
                <json>
                    <code>AC</code>
                </json>
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

    private def createThing(String code) {
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