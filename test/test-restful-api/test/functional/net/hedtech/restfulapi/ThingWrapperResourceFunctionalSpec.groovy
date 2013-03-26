/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

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
        get( "$localBase/api/thing-wrapper" ) {
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