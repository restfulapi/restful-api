/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 

package net.hedtech.restfulapi

import com.grailsrocks.functionaltest.*

import grails.converters.JSON


class RestfulApiControllerFunctionalTests extends BrowserTestCase {

    static final String localBase = "http://127.0.0.1:8080/test-restful-api"


    void testList_json() {

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

        // Assert that an '_href' property is present but not a 'numParts'
        // property, which proves the plugin-registered marshaller is 
        // being used versus the built-in grails marshaller or the 
        // resource-specific marshaller registered by this test app.
        //
        assert json.data[0]._href?.contains('things')  
        assertNull json.data[0].numParts
    }


    void testList_jsonv0() {

        get( "$localBase/api/things" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/vnd.hedtech.v0+json'
//            headers['Authorization'] = TestUtils.authHeader('user','password')
        }
        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assert "AA" == json.data[0].code
        assert "An AA thing" == json.data[0].description

        // Assert the 'numParts' property is present proving the 
        // resource-specific marshaller registered for the 'jsonv0'
        // configuration was used.
        //
        assert 2 == json.data[0].numParts
    }


    void testShow_json() {

        get( "$localBase/api/things/1" ) {
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
    }


    void testShow_jsonv0() {

        get( "$localBase/api/things/1" ) {
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
    }

}
