/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 

package net.hedtech.restfulapi

import com.grailsrocks.functionaltest.*

import grails.converters.JSON

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
//            headers['Authorization'] = TestUtils.authHeader('user','password')
        }
        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
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


    void testList_jsonv0() {
        createThing('AA')
        createThing('BB')

        get( "$localBase/api/things" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/vnd.hedtech.v0+json'
//            headers['Authorization'] = TestUtils.authHeader('user','password')
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

    void testSave_json() {
        post( "$localBase/api/things") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                { 
                    code:'AC',
                    description:'An AC thingy',
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
                    code:'AD',
                    description:'An AD thingy',
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

    void testSaveExisting() {
        createThing('AA')

        post( "$localBase/api/things" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                { 
                    code:'AA',
                    description:'An AA thingy',
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
                    code:'AA',
                    description:'An AA thingy',
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
                    description:'updated description',
                    version:'0'
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
                    description:'updated description',
                    version:'0'
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
                    description:'updated description',
                    version:'0'
                }
                """
            }
        }
        assertStatus 409
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertFalse json.success
        assert 1 == json.errors.size()
        assert 'optimisticlock' == json.errors[0].type
        assert json.errors[0].resource.class == 'net.hedtech.restfulapi.Thing'
        assertNotNull json.errors[0].errorMessage 
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
                    description:'updated description',
                    version:'0'
                }
                """
            }
        }

        assertStatus 409
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertFalse json.success
        assert 1 == json.errors.size()
        assert 'optimisticlock' == json.errors[0].type
        assert json.errors[0].resource.class == 'net.hedtech.restfulapi.Thing'
        assertNotNull json.errors[0].errorMessage 
    }

    void testStaleObject() {
        put( "$localBase/api/things/1?throwStaleObjectStateException=y" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/vnd.hedtech.v0+json'
            body {
                """
                { 
                    description:'updated description',
                    version:'0'
                }
                """
            }
        }

        assertStatus 409
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertFalse json.success
        assert 1 == json.errors.size()
        assert 'optimisticlock' == json.errors[0].type
        assert json.errors[0].resource.class == 'net.hedtech.restfulapi.Thing'
        assertNotNull json.errors[0].errorMessage 
    }    

    void testCustomOptimisticLock() {
        put( "$localBase/api/things/1?throwAppOptimisticLockException=y" ) {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/vnd.hedtech.v0+json'
            body {
                """
                { 
                    description:'updated description',
                    version:'0'
                }
                """
            }
        }

        assertStatus 409
        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assertFalse json.success
        assert 1 == json.errors.size()
        assert 'optimisticlock' == json.errors[0].type
        assert json.errors[0].resource.class == 'net.hedtech.restfulapi.Thing'
        assertNotNull json.errors[0].errorMessage         
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
