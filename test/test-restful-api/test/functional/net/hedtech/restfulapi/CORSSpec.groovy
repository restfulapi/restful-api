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

import grails.test.mixin.*
import grails.plugins.rest.client.*

import grails.converters.JSON
import grails.converters.XML

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.spock.*

import spock.lang.*


class CORSSpec extends RestSpecification {

    static final String localBase = "http://127.0.0.1:8080/test-restful-api"

    def setup() {
        deleteThings()
    }

    def cleanup() {
        deleteThings()
    }

    @Unroll
    def "Test CORS headers on all operations"(def method, boolean id, def status, def data) {
        setup:
        def aaID = createThing('AA')
        createThing('BB')
        def url = id ? "$localBase/api/things/$aaID" : "$localBase/api/things"
        def expectedType = method == 'delete' ? 'text/plain' : 'application/json'


        when:
        "$method"("$url") {
            //java HttpURLConnection blocks CORS headers by default
            //using HttpClient so we can send Origin header
            requestFactory new HttpComponentsClientHttpRequestFactory()
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
        status       == response.status
        expectedType == response.contentType
        // assert localization of the message
        null != responseHeader('X-hedtech-message')

        //check CORS headers
        null   != responseHeader('Access-Control-Allow-Origin')
        "true" == responseHeader('Access-Control-Allow-Credentials')
        null   != responseHeader('Access-Control-Expose-Headers')

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

    private void createManyThings(int num = 99) {
        assert num < 100 // index is used as 2 char code
        int index = 0
        num.times {
            String code = String.format("%02d", index++)
            createThing(code)
        }
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
