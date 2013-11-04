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


class ThingWrapperResourceFunctionalSpec extends RestSpecification {

    static final String localBase = "http://127.0.0.1:8080/test-restful-api"

    void setup() {
        deleteThings()
    }

    void cleanup() {
        deleteThings()
    }


    def "Test list with json response"() {
        setup:
        createThings()

        when:
        get( "$localBase/api/thing-wrappers" ) {
            headers['Content-Type']  = 'application/json'
            headers['Accept']        = 'application/json'
        }

        then:
        200 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        null != json[0].xlarge
    }


    def "Test saving with json"() {
        setup:
        createThings()

        when:
        post( "$localBase/api/thing-wrappers") {
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

        then:
        201 == response.status
        'application/json' == response.contentType
        def json = JSON.parse response.text
        "MMNN" == json.complexCode
        "MM" == json.things[0].code
        "An MM thingy" == json.things[0].description
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
