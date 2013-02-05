/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import com.grailsrocks.functionaltest.*

import grails.converters.JSON

import static org.junit.Assert.*
import org.junit.*


class ComplexThingResourceFunctionalTests extends BrowserTestCase {

    static final String localBase = "http://127.0.0.1:8080/test-restful-api"

    @Before
    void setUp() {
        super.setUp()
        deleteComplexThings()
        deleteThings()
    }

    @After
    void tearDown() {
        deleteComplexThings()
        deleteThings()
        super.tearDown()
    }


    void testNonPersistentProperty_json() {
        createComplexThing('AABB')

        get( "$localBase/api/complex-things" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }
        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assert json.data[0].xlarge
    }


    void testDateProperty_json() {
        createComplexThing('AABB')

        get( "$localBase/api/complex-things" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }
        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        def dataParser = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        def retrievedDate = dataParser.parse(json.data[0].buildDate)
        use(groovy.time.TimeCategory) {
            retrievedDate > 2.seconds.ago // we can be very loose...
        }
    }


    void testAssociatedResourceProperty_json() {
        def complexThing = createComplexThing('AABB')

        get( "$localBase/api/complex-things/${complexThing.id}" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }
        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assert 2 == json.data.things?.size()
        assert 0 <= json.data.things[0].id
        assert 0 <= json.data.things[1].id
    }


    void testTransientSerializionProperty_json() {
        def complexThing = createComplexThing('AABB')

        get( "$localBase/api/complex-things/${complexThing.id}" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }
        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
        assert "junk" == json.data.transientProp
    }


    void testBigDecimalProperty_json() {
        def complexThing = createComplexThing('AABB')

        get( "$localBase/api/complex-things/${complexThing.id}" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }
        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
println "******************************** JSON ****************************** \n$json"
        assert 2 == json.data.size
    }


    void testExcludedProperties_json() {
        def complexThing = createComplexThing('AABB')

        get( "$localBase/api/complex-things/${complexThing.id}" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }
        assertStatus 200
        assertEquals 'application/json', page?.webResponse?.contentType

        def stringContent = page?.webResponse?.contentAsString
        def json = JSON.parse stringContent
println "******************************** JSON ****************************** \n$json"
        assertNull json.data.dataOrigin
        assertNull json.data.lastModifiedBy
        assertNull json.data.modifiedBy
    }


    private def createComplexThing(String code) {
        def id
        ComplexThing thing
        Thing.withTransaction {
            thing = new ComplexThing(complexCode: code)
            thing.addThing(createThing('AA'))
            thing.addThing(createThing('BB'))
            thing.save(failOnError:true, flush:true)
            thing.getId()
        }
        thing
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