/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import com.grailsrocks.functionaltest.*

import grails.converters.JSON
import grails.converters.XML

import static org.junit.Assert.*
import org.junit.*


class RestfulApiControllerFunctionalTests extends BrowserTestCase {

    static final String localBase = "http://127.0.0.1:8080/test-restful-api"

    @Before
    void setUp() {
        super.setUp()
        deleteThings()
    }

    @After
    void tearDown() {
        deleteThings()
        super.tearDown()
    }

    void testList_json() {
        createThing('AA')
        createThing('BB')

        get( "$localBase/api/things" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }
        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assert 2 == json.size()
        assert "AA" == json[0].code
        assert "An AA thing" == json[0].description

        // assert localization of the message
        assertHeader 'X-hedtech-message', "List of thing resources"

        //check pagination headers
        assertHeader 'X-hedtech-totalCount', "2"
        assertNotNull page?.webResponse?.getResponseHeaderValue( 'X-hedtech-pageOffset' )
        assertNotNull page?.webResponse?.getResponseHeaderValue( 'X-hedtech-pageMaxSize' )

        // Assert that an '_href' property is present but not a 'numParts'
        // property, which proves the plugin-registered marshaller is
        // being used versus the built-in grails marshaller or the
        // resource-specific marshaller registered by this test app.
        //
        assert json[0]._href?.contains('things')
        assertNull json[0].numParts
        assertNull json[0].sha1
    }

    void testList_json_as_xml() {
        createThing('AA')
        createThing('BB')

        get( "$localBase/api/things" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/xml'
//            headers['Authorization'] = TestUtils.authHeader('user','password')
        }
        assertStatus 200
        assertEquals 'application/xml', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString

        def xml = XML.parse stringContent
        assert 2 == xml.'net-hedtech-object'.size()
        assert "AA" == xml.'net-hedtech-object'[0].code[0].text()
        assert "An AA thing" == xml.'net-hedtech-object'[0].description[0].text()

        // assert localization of the message
        assertHeader 'X-hedtech-message', "List of thing resources"

        //check pagination headers
        assertHeader 'X-hedtech-totalCount', "2"
        assertNotNull page?.webResponse?.getResponseHeaderValue( 'X-hedtech-pageOffset' )
        assertNotNull page?.webResponse?.getResponseHeaderValue( 'X-hedtech-pageMaxSize' )

        // Assert that an '_href' property is present but not a 'numParts'
        // property, which proves the plugin-registered marshaller is
        // being used versus the built-in grails marshaller or the
        // resource-specific marshaller registered by this test app.
        //
        assert xml.'net-hedtech-object'[0]._href[0].text().contains('things')
        assert 0 == xml.'net-hedtech-object'[0].numParts.size()
        assert 0 == xml.'net-hedtech-object'[0].sha1.size()

    }


    void testList_jsonv0() {
        createThing('AA')
        createThing('BB')

        get( "$localBase/api/things" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/vnd.hedtech.v0+json'
        }
        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType
        assertHeader "X-hedtech-Media-Type", 'application/vnd.hedtech.v0+json'

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent

        // Assert the 'numParts' property is present proving the
        // resource-specific marshaller registered for the 'jsonv0'
        // configuration was used.
        //
        assert 2 == json[0].numParts
        assertNotNull json[0].sha1

        assert "AA" == json[0].code
        assert "An AA thing" == json[0].description
    }

    void testList_jsonv0_as_xml() {
        createThing('AA')
        createThing('BB')

        get( "$localBase/api/things" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/vnd.hedtech.v0+xml'
//            headers['Authorization'] = TestUtils.authHeader('user','password')
        }
        assertStatus 200
        assertEquals 'application/xml', page?.webResponse?.contentType
        assertHeader "X-hedtech-Media-Type", 'application/vnd.hedtech.v0+xml'

        def stringContent = page?.webResponse?.contentAsString

        def xml = XML.parse stringContent
        assert 2 == xml.'net-hedtech-object'.size()
        assert "AA" == xml.'net-hedtech-object'[0].code[0].text()
        assert "An AA thing" == xml.'net-hedtech-object'[0].description[0].text()

        // assert localization of the message
        assertHeader 'X-hedtech-message', "List of thing resources"

        // Assert that an '_href' property is present but not a 'numParts'
        // property, which proves the plugin-registered marshaller is
        // being used versus the built-in grails marshaller or the
        // resource-specific marshaller registered by this test app.
        //
        assert xml.'net-hedtech-object'[0]._href[0].text().contains('things')
        assert '2' == xml.'net-hedtech-object'[0].numParts[0].text()
        assertNotNull xml.'net-hedtech-object'[0].sha1[0].text()

    }


    void testValidationError() {
        createThing('AA')
        createThing('BB')

        // TODO: Replace with real validation testing in create/update
        get( "$localBase/api/things/?forceValidationError=y" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
//            headers['Authorization'] = TestUtils.authHeader('user','password')
        }
        assertStatus 400
        assertEquals 'application/json', page?.webResponse?.contentType

        assertHeader "X-Status-Reason", 'Validation failed'

        // assert localization of the message
        assertHeader 'X-hedtech-message', "thing resource had validation errors"


        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assert 1 == json.errors.size()
        assert "validation" == json.errors[0].type
        assert json.errors[0].resource.class == 'net.hedtech.restfulapi.Thing'
        assertNotNull json.errors[0].errorMessage
    }


    void testShow_json() {
        def id = createThing('AA')
        createThing('BB')

        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
//            headers['Authorization'] = TestUtils.authHeader('user','password')
        }
        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString

        def json = JSON.parse stringContent
        assert "AA" == json.code
        assert "An AA thing" == json.description
        assert json._href?.contains('things')
        assertNull json.numParts
        assertNull json.sha1
        assertNotNull json.version

        // assert localization of the message
        assertHeader 'X-hedtech-message', "Details for the thing resource"
    }

    void testShow_xml_from_json() {
        def id = createThing('AA')
        createThing('BB')

        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/xml'
//            headers['Authorization'] = TestUtils.authHeader('user','password')
        }
        assertStatus 200
        assertEquals 'application/xml', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString

        def xml = XML.parse stringContent
        assert "AA" == xml.code[0].text()
        assert "An AA thing" == xml.description[0].text()
        assert xml._href[0]?.text().contains('things')
        assert xml.numParts.isEmpty()
        assert xml.sha1.isEmpty()
        assertNotNull xml.version[0].text()

        // assert localization of the message
        assertHeader 'X-hedtech-message', "Details for the thing resource"
    }

    void testShow_jsonv0() {
        def id = createThing('AA')
        createThing('BB')

        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/vnd.hedtech.v0+json'
//            headers['Authorization'] = TestUtils.authHeader('user','password')
        }
        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType
        assertHeader "X-hedtech-Media-Type", 'application/vnd.hedtech.v0+json'

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assert "AA" == json.code
        assert "An AA thing" == json.description
        assert 2 == json.numParts
        assertNotNull json.version
    }

    void testShow_xml_from_json_v0() {
        def id = createThing('AA')
        createThing('BB')

        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/vnd.hedtech.v0+xml'
//            headers['Authorization'] = TestUtils.authHeader('user','password')
        }
        assertStatus 200
        assertEquals 'application/xml', page?.webResponse?.contentType
        assertHeader "X-hedtech-Media-Type", 'application/vnd.hedtech.v0+xml'

        def stringContent = page?.webResponse?.contentAsString
        def xml = XML.parse stringContent
        assert "AA" == xml.code[0].text()
        assert "An AA thing" == xml.description[0].text()
        assert '2' == xml.numParts[0].text()
        assertNotNull xml.version[0].text()

    }


    void testSave_json() {
        post( "$localBase/api/things") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                    "code": "AC",
                    "description": "An AC thingy",
                }
                """
            }
        }
        assertStatus 201
        assertEquals 'application/json', page?.webResponse?.contentType

        // assert localization of the message
        assertHeader 'X-hedtech-message', "thing resource created"

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertNotNull json.id
        assert "AC" == json.code
        assert "An AC thingy" == json.description
        assert 0 == json.parts.size()
        assertNotNull json.version
    }

    void testSave_json_response_jsonv0() {
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
        assertStatus 201
        assertEquals 'application/json', page?.webResponse?.contentType

        assertHeader "X-Hedtech-Media-Type", 'application/vnd.hedtech.v0+json'

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertNotNull json.id
        assert "AD" == json.code
        assert "An AD thingy" == json.description
        assert 0 == json.numParts
        assertNotNull json.version
    }


    void testSave_json_as_xml() {
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
        assertStatus 201
        assertEquals 'application/xml', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def xml = XML.parse stringContent

        assertNotNull xml.id[0].text()
        assert "AC" == xml.code[0].text()
        assert "An AC thingy" == xml.description[0].text()
        assertNotNull xml.version[0].text()
        assert xml.version[0].text().length() > 0
    }

    void testSaveExisting() {
        createThing('AA')

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
        assertStatus 400
        assertHeader "X-Status-Reason", 'Validation failed'

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assert 1 == json.errors.size()
        assert "validation" == json.errors[0].type
        assert json.errors[0].resource.class == 'net.hedtech.restfulapi.Thing'
        assertNotNull json.errors[0].errorMessage
    }

    void testGenericErrorOnSave() {
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
        assertStatus 500
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assert 1 == json.errors.size()
        assert "general" == json.errors[0].type
        assert json.errors[0].resource.class == 'net.hedtech.restfulapi.Thing'
        assertNotNull json.errors[0].errorMessage
    }

    void testUpdate_json() {
        def id = createThing('AA')

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

        assertStatus 200

        // assert localization of the message
        assertHeader 'X-hedtech-message', "thing resource updated"

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertNotNull json.id
        assertNotNull json.version
        assert json.version > 0
        assert "updated description" == json.description
        assertNull json.numParts
        assert 2 == json.parts.size()
    }

    void testUpdate_json_response_jsonv0() {
        def id = createThing('AA')

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

        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType
        assertHeader "X-hedtech-Media-Type", 'application/vnd.hedtech.v0+json'
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertNotNull json.id
        assertNotNull json.version
        assert json.version > 0
        assert "updated description" == json.description
        assert 2 == json.numParts
        assert 2 == json.parts.size()
    }

    void testUpdateOptimisticLock() {
        def id = createThing('AA')
        updateThing( id, [description:'changed'] )

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
        assertStatus 409

        // assert localization of the message
        assertHeader 'X-hedtech-message', "Another user has updated this thing while you were editing"

        def stringContent = page?.webResponse?.contentAsString
        assert "" == stringContent
    }



    void testDelete() {
        def id = createThing('AA')

        delete( "$localBase/api/things/$id" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }

        assertStatus 200

        // assert localization of the message
        assertHeader 'X-hedtech-message', "thing resource deleted"

        def count

        Thing.withNewSession{
            count = Thing.findAll().size()
        }

        assert 0 == count

    }

    void testOptimisticLock() {
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

        assertStatus 409

        // assert localization of the message
        assertHeader 'X-hedtech-message', "Another user has updated this thing while you were editing"
        def stringContent = page?.webResponse?.contentAsString
        assert "" == stringContent
    }

    void testUpdateOptimisticLock_json_as_xml() {
        def id = createThing('AA')
        updateThing( id, [description:'changed'] )

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
        assertStatus 409
        assertHeader 'X-hedtech-message', "Another user has updated this thing while you were editing"
        def stringContent = page?.webResponse?.contentAsString
        assert "" == stringContent
    }

    void testApplicationException() {
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

        assertStatus 400
        assertHeader "X-Status-Reason", 'Validation failed'

        // assert localization of the message
        assertHeader 'X-hedtech-message', "foo resource had errors"

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertNotNull json.errors
        assert 1 == json.errors.size()
    }

    void testApplicationException_json_as_xml() {
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

        assertStatus 400
        assertHeader "X-Status-Reason", 'Validation failed'

        // assert localization of the message
        assertHeader 'X-hedtech-message', "foo resource had errors"

        def stringContent = page?.webResponse?.contentAsString
        def xml = XML.parse stringContent
        assert 1 == xml.errors.size()
    }

    void testApplicationExceptionWithoutMessageAndErrorBlock() {
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

        assertStatus 409

        //should be no message header in this case
        def header = page?.webResponse?.getResponseHeaders().find { it.getName() == 'X-hedtech-message' ? it : null }
        assertNull header

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertNull json.errors
    }

    void testBrokenErrorHandling() {
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

        assertStatus 500
        // assert localization of the message
        assertHeader 'X-hedtech-message', "Encountered unexpected error generating a response"

        def stringContent = page?.webResponse?.contentAsString
        assert "" == stringContent
    }

    void testBrokenErrorHandling_json_as_xml() {
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

        assertStatus 500
        // assert localization of the message
        assertHeader 'X-hedtech-message', "Encountered unexpected error generating a response"
        def stringContent = page?.webResponse?.contentAsString
        assert "" == stringContent
    }

    void testSave_jsonv1() {
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
        assertStatus 201
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertNotNull json.id
        assert "AC" == json.code
        assert "Default description" == json.description
        assert 0 == json.parts.size()
        assertNotNull json.version
    }

    void testSave_xmlv1() {
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
        assertStatus 201
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertNotNull json.id
        assert "AC" == json.code
        assert "Default description" == json.description
        assert 0 == json.parts.size()
        assertNotNull json.version
    }

    void testRequestNoExtractor_json() {
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

        assertStatus 415

        // assert localization of the message
        assertHeader 'X-hedtech-message', "Unsupported media type 'application/vnd.hedtech.no_extractor+json' for resource 'things'"

        def stringContent = page?.webResponse?.contentAsString
        assert "" == stringContent
    }

    void testRequestNoExtractor_xml() {
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

        assertStatus 415

        // assert localization of the message
        assertHeader 'X-hedtech-message', "Unsupported media type 'application/vnd.hedtech.no_extractor+xml' for resource 'things'"
        def stringContent = page?.webResponse?.contentAsString
        assert "" == stringContent
    }

    void testUnknownResponseFormat_xml() {
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

        assertStatus 406
        assertEquals 'application/xml', page?.webResponse?.contentType

        // assert localization of the message
        assertHeader 'X-hedtech-message', "Unsupported media type 'application/vnd.hedtech.no_such_type+xml' for resource 'things'"
        def stringContent = page?.webResponse?.contentAsString
        assert "" == stringContent
    }

    void testUnknownResponseFormat_json() {
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

        assertStatus 406
        assertEquals 'application/json', page?.webResponse?.contentType

        // assert localization of the message
        assertHeader 'X-hedtech-message', "Unsupported media type 'application/vnd.hedtech.no_such_type+json' for resource 'things'"
        def stringContent = page?.webResponse?.contentAsString
        assert "" == stringContent
    }

    void testCustomXMLMarshalling_v0() {
        def id = createThing('AA')

        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/xml'
            headers['Accept']        = 'application/vnd.hedtech.thing.v0+xml'
        }
        assertStatus 200
        assertEquals 'application/xml', page?.webResponse?.contentType
        assertHeader "X-hedtech-Media-Type", 'application/vnd.hedtech.thing.v0+xml'

        assertHeader 'X-hedtech-message', "Details for the thing resource"

        def stringContent = page?.webResponse?.contentAsString

        def xml = XML.parse stringContent
        println xml.@id.getClass().getName()
        assert id.toString() == xml.@id.text()
        assertNotNull xml.@version
        assert "AA" == xml.code.text()
        assert "An AA thing" == xml.description.text()
        assert 0 == xml.parts.size()
    }

    void testCustomXMLMarshalling_v1() {
        def id = createThing('AA')

        get( "$localBase/api/things/$id" ) {
            headers['Content-Type']  = 'application/xml'
            headers['Accept']        = 'application/vnd.hedtech.thing.v1+xml'
        }
        assertStatus 200
        assertEquals 'application/xml', page?.webResponse?.contentType
        assertHeader "X-hedtech-Media-Type", 'application/vnd.hedtech.thing.v1+xml'

        assertHeader 'X-hedtech-message', "Details for the thing resource"

        def stringContent = page?.webResponse?.contentAsString

        def xml = XML.parse stringContent
        println xml.@id.getClass().getName()
        assert id.toString() == xml.@id.text()
        assertNotNull xml.@version
        assert "AA" == xml.code.text()
        assert "An AA thing" == xml.description.text()

        assert 2 == xml.parts.part.size()

    }

    private void createThings() {
        createThing('AA')
        createThing('BB')

    }

    private def createThing(String code) {
        Thing.withTransaction {
            Thing thing = new Thing(code: code, description: "An $code thing",
                      dateManufactured: new Date(), isGood: 'Y', isLarge: true)
            thing.addPart(new PartOfThing(code: 'aa', description: 'aa part').save())
            thing.addPart(new PartOfThing(code: 'bb', description: 'bb part').save())
            thing.save(failOnError:true, flush:true)
            thing.getId()
        }
    }

    private def createPartOfThing( String code, String desc ) {
        PartOfThing.withTransaction {
            PartOfThing part = new PartOfThing( code:code, description:desc )
            part.save(failOnError:true, flush:true)
            part.getId()
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
