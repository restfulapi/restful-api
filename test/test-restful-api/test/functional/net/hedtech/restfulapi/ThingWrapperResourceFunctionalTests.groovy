/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import com.grailsrocks.functionaltest.*

import grails.converters.JSON

import static org.junit.Assert.*
import org.junit.*


class ThingWrapperResourceFunctionalTests extends BrowserTestCase {

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


    void testList_Json() {

        createThings()

        get( "$localBase/api/thing-wrapper" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }
        // assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assert json[0].xlarge
    }


    void testSave_json() {

        createThings()

        post( "$localBase/api/thing-wrapper") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            body {
                """
                {
                  "complexCode": "MMNN",
                  "things": [ {
                              "code":"MM",
                              "description": "An MM thingy"
                            },
                            {
                              "code": "NN",
                              "description": "An NN thingy"
                            } ]
                }
                """
            }
        }
        assertStatus 201
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assert "MMNN" == json.complexCode
        assert "MM" == json.things[0].code
        assert "An MM thingy" == json.things[0].description
    }



    private def createThing(String code) {
        def id
        Thing thing
        Thing.withTransaction {
            thing = new Thing(code: code, description: "An $code thing",
                              dateManufactured: new Date(), isGood: 'Y', isLarge: true)
            thing.addPart(new PartOfThing(code: 'aa', description: 'aa part').save())
            thing.addPart(new PartOfThing(code: 'bb', description: 'bb part').save())
            thing.save(failOnError:true, flush:true)
            thing.getId()
        }
        thing
    }


    private void createThings() {
        createThing('AA')
        createThing('BB')

    }


    private void deleteComplexThings() {
        Thing.withNewSession{
            ComplexThing.findAll().each() { it.delete(failOnError:true,flush:true) }
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