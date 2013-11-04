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

import net.hedtech.restfulapi.spock.*

import grails.converters.JSON

import static org.junit.Assert.*
import org.junit.*


class ComplexThingResourceFunctionalSpec extends RestSpecification {
    static final String localBase = "http://127.0.0.1:8080/test-restful-api"

    def setup() {
        deleteComplexThings()
        deleteThings()
    }

    def cleanup() {
        deleteComplexThings()
        deleteThings()
    }

    def "Test non-persistent property for json"() {
        setup:
        createComplexThing('AABB')

        when:
        get( "$localBase/api/complex-things" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        null != json[0].xlarge
    }

    def "Test date property in json response"() {
        setup:
        createComplexThing('AABB')

        when:
        get( "$localBase/api/complex-things" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }
        def dataParser = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        def retrievedDate = dataParser.parse(json[0].buildDate)
        use(groovy.time.TimeCategory) {
            //we can be very loose...
            retrievedDate > 2.seconds.ago
        }
    }


    def "Test associated resource property in json response"() {
        setup:
        def complexThing = createComplexThing('AABB')

        when:
        get( "$localBase/api/complex-things/${complexThing.id}" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        2 == json.things?.size()
        null != json.things[0]._link
        null != json.things[1]._link
    }

    def "Test transient serialization property in json response"() {
        setup:
        def complexThing = createComplexThing('AABB')

        when:
        get( "$localBase/api/complex-things/${complexThing.id}" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        "junk" == json.transientProp
    }


    def "Test BigDecimal property in json response"() {
        setup:
        def complexThing = createComplexThing('AABB')

        when:
        get( "$localBase/api/complex-things/${complexThing.id}" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        2 == json.size
    }

    def "Test excluded properties in json response"() {
        setup:
        def complexThing = createComplexThing('AABB')

        when:
        get( "$localBase/api/complex-things/${complexThing.id}" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType

        def json = JSON.parse response.text
        null == json.dataOrigin
        null == json.lastModifiedBy
        null == json.modifiedBy
    }


    private def createComplexThing(String code) {
        ComplexThing thing
        Thing.withTransaction {
            thing = new ComplexThing(complexCode: code)
            thing.addThing(createThing('AA'))
            thing.addThing(createThing('BB'))
            thing.save(failOnError:true, flush:true)
        }
        thing
    }


    private def createThing(String code) {
        Thing thing
        Thing.withTransaction {
            thing = new Thing(code: code, description: "An $code thing",
                              dateManufactured: new Date(), isGood: 'Y', isLarge: true)
                .addToParts(new PartOfThing(code: 'aa', description: 'aa part'))
                .addToParts(new PartOfThing(code: 'bb', description: 'bb part'))
                .save(failOnError:true, flush:true)
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

