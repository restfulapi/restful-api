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
        assert 2 == json.data.size()
        assert "AA" == json.data[0].code
        assert "An AA thing" == json.data[0].description

        // assert localization of the message
        assert "List of thing resources" == json.message

        // Assert that an '_href' property is present but not a 'numParts'
        // property, which proves the plugin-registered marshaller is
        // being used versus the built-in grails marshaller or the
        // resource-specific marshaller registered by this test app.
        //
        assert json.data[0]._href?.contains('things')
        assertNull json.data[0].numParts
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
        assertEquals 'text/xml', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString

        def xml = XML.parse stringContent
        assert 2 == xml.data.'net-hedtech-array'.'net-hedtech-arrayElement'.size()
        assert "AA" == xml.data.'net-hedtech-array'.'net-hedtech-arrayElement'[0].code[0].text()
        assert "An AA thing" == xml.data.'net-hedtech-array'.'net-hedtech-arrayElement'[0].description[0].text()

        // assert localization of the message
        assert "List of thing resources" == xml.message.text()

        // Assert that an '_href' property is present but not a 'numParts'
        // property, which proves the plugin-registered marshaller is
        // being used versus the built-in grails marshaller or the
        // resource-specific marshaller registered by this test app.
        //
        assert xml.data.'net-hedtech-array'.'net-hedtech-arrayElement'[0]._href[0].text().contains('things')
        assert 0 == xml.data.'net-hedtech-array'.'net-hedtech-arrayElement'[0].numParts.size()

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

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent

        // Assert the 'numParts' property is present proving the
        // resource-specific marshaller registered for the 'jsonv0'
        // configuration was used.
        //
        assert 2 == json.data[0].numParts

        assert "AA" == json.data[0].code
        assert "An AA thing" == json.data[0].description
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
        assertEquals 'text/xml', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString

        def xml = XML.parse stringContent
        assert 2 == xml.data.'net-hedtech-array'.'net-hedtech-arrayElement'.size()
        assert "AA" == xml.data.'net-hedtech-array'.'net-hedtech-arrayElement'[0].code[0].text()
        assert "An AA thing" == xml.data.'net-hedtech-array'.'net-hedtech-arrayElement'[0].description[0].text()

        // assert localization of the message
        assert "List of thing resources" == xml.message.text()

        // Assert that an '_href' property is present but not a 'numParts'
        // property, which proves the plugin-registered marshaller is
        // being used versus the built-in grails marshaller or the
        // resource-specific marshaller registered by this test app.
        //
        assert xml.data.'net-hedtech-array'.'net-hedtech-arrayElement'[0]._href[0].text().contains('things')
        assert '2' == xml.data.'net-hedtech-array'.'net-hedtech-arrayElement'[0].numParts[0].text()

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

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertFalse json.success
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
        assert "AA" == json.data.code
        assert "An AA thing" == json.data.description
        assert json.data._href?.contains('things')
        assertNull json.data.numParts
        assertNotNull json.data.version

        // test localization of the message
        assert "Details for the thing resource" == json.message
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
        assertEquals 'text/xml', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString

        def xml = XML.parse stringContent
        assert "AA" == xml.data.code[0].text()
        assert "An AA thing" == xml.data.description[0].text()
        assert xml.data._href[0]?.text().contains('things')
        assert xml.data.numParts.isEmpty()
        assertNotNull xml.data.version[0].text()

        // test localization of the message
        assert "Details for the thing resource" == xml.message[0].text()
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

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assert "AA" == json.data.code
        assert "An AA thing" == json.data.description
        assert 2 == json.data.numParts
        assertNotNull json.data.version
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
        assertEquals 'text/xml', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def xml = XML.parse stringContent
        assert "AA" == xml.data.code[0].text()
        assert "An AA thing" == xml.data.description[0].text()
        assert '2' == xml.data.numParts[0].text()
        assertNotNull xml.data.version[0].text()

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

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertNotNull json.data.id
        assert "AC" == json.data.code
        assert "An AC thingy" == json.data.description
        assert 0 == json.data.parts.size()
        assertNotNull json.data.version
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

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertNotNull json.data.id
        assert "AD" == json.data.code
        assert "An AD thingy" == json.data.description
        assert 0 == json.data.numParts
        assertNotNull json.data.version
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
        assertEquals 'text/xml', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def xml = XML.parse stringContent

        assertNotNull xml.data.id[0].text()
        assert "AC" == xml.data.code[0].text()
        assert "An AC thingy" == xml.data.description[0].text()
        assertNotNull xml.data.version[0].text()
        assert xml.data.version[0].text().length() > 0
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
        assertFalse json.success
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
        assertFalse json.success
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
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertTrue json.success
        assertNotNull json.data.id
        assertNotNull json.data.version
        assert json.data.version > 0
        assert "updated description" == json.data.description
        assertNull json.data.numParts
        assert 2 == json.data.parts.size()
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
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertTrue json.success
        assertNotNull json.data.id
        assertNotNull json.data.version
        assert json.data.version > 0
        assert "updated description" == json.data.description
        assert 2 == json.data.numParts
        assert 2 == json.data.parts.size()
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
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertFalse json.success
        assertNull json.errors
        assert "Another user has updated this thing while you were editing" == json.message
    }



    void testDelete() {
        def id = createThing('AA')

        delete( "$localBase/api/things/$id" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
        }

        assertStatus 200
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertTrue json.success

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
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertFalse json.success
        assertNull json.errors
        assert "Another user has updated this thing while you were editing" == json.message
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
        def stringContent = page?.webResponse?.contentAsString
        def xml = XML.parse stringContent
        assertFalse new Boolean( xml.success[0].text() )
        assert 0 == xml.errors.size()
        assert "Another user has updated this thing while you were editing" == xml.message[0].text()
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
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertFalse json.success
        assertNotNull json.errors
        assert 1 == json.errors.size()
        assert "foo resource had errors" == json.message
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
        def stringContent = page?.webResponse?.contentAsString
        def xml = XML.parse stringContent
        assertFalse new Boolean( xml.success.text() )
        assert 1 == xml.errors.size()
        assert "foo resource had errors" == xml.message.text()
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
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertFalse json.success
        assertNull json.errors
        assertNull json.message
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
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertFalse json.success
        assert "Encountered unexpected error generating a response" == json.message
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
        def stringContent = page?.webResponse?.contentAsString
        def xml = XML.parse stringContent
        assertFalse new Boolean( xml.success.text() )
        assert "Encountered unexpected error generating a response" == xml.message.text()
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

        assertStatus 400
        assertHeader "X-Status-Reason", 'Unknown resource representation'
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertFalse json.success
        assert "Unsupported media type 'application/vnd.hedtech.no_extractor+json' for resource 'things'" == json.message
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

        assertStatus 400
        assertHeader "X-Status-Reason", 'Unknown resource representation'
        def stringContent = page?.webResponse?.contentAsString
        def xml = XML.parse stringContent
        assertFalse new Boolean( xml.success.text() )
        assert "Unsupported media type 'application/vnd.hedtech.no_extractor+xml' for resource 'things'" == xml.message.text()
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

        assertStatus 400
        assertEquals 'text/xml', page?.webResponse?.contentType
        assertHeader "X-Status-Reason", 'Unknown resource representation'
        def stringContent = page?.webResponse?.contentAsString
        def xml = XML.parse stringContent
        assertFalse new Boolean( xml.success.text() )
        assert "Unsupported media type 'application/vnd.hedtech.no_such_type+xml' for resource 'things'" == xml.message.text()
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

        assertStatus 400
        assertEquals 'application/json', page?.webResponse?.contentType
        assertHeader "X-Status-Reason", 'Unknown resource representation'
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertFalse json.success
        assert "Unsupported media type 'application/vnd.hedtech.no_such_type+json' for resource 'things'" == json.message
    }

    private void createThings() {
        createThing('AA')
        createThing('BB')

    }

    private def createThing(String code) {
        def id
        Thing.withTransaction {
            Thing thing = new Thing(code: code, description: "An $code thing",
                      dateManufactured: new Date(), isGood: 'Y', isLarge: true)
            thing.addPart(new PartOfThing(code: 'aa', description: 'aa part').save())
            thing.addPart(new PartOfThing(code: 'bb', description: 'bb part').save())
            thing.save(failOnError:true, flush:true)
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
