/*****************************************************************************
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

class ServiceOverrideSpec extends RestSpecification {

    static final String localBase = "http://127.0.0.1:8080/test-restful-api"

    def setup() {
        deleteThings()
    }

    def cleanup() {
        deleteThings()
    }

    @Unroll
    def "Test overriding service spec for all operations"(def method, boolean id, def status, def data) {
        setup:
        def aaID = createThing('AA')
        createThing('BB')
        def url = id ? "$localBase/api/thingamabobs/$aaID" : "$localBase/api/thingamabobs"


        when:
        "$method"("$url") {
            headers['Content-Type'] = 'application/json'
            headers['Accept']       = 'application/json'
            headers['Origin']       = localBase
            if(data) {
                body {
                    "$data"
                }
            }
        }

        then:
        status == response.status
        'application/json' == response.contentType
        // assert localization of the message
        null != responseHeader('X-hedtech-message')
        responseHeader('X-hedtech-message').contains( 'thingamabob' )

        where:
        method     | id    | status | data
        'get'      | false | 200    | null
        'get'      | true  | 200    | null
        'post'     | false | 201    | "{code:'ZZ',description:'ZZ thing'}"
        'put'      | true  | 200    | "{description:'changed',version:'0'}"
        'delete'   | true  | 200    | null
    }


    private void createThings() {
        createThing('AA')
        createThing('BB')

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

    private void deleteThings() {
        Thing.withNewSession{
            def things = Thing.findAll()
            things.each() { aThing ->
                aThing.delete(failOnError:true,flush:true)
            }
        }
    }

}