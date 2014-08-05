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

import org.codehaus.groovy.grails.web.util.WebUtils

class ThingWrapperService {

    def thingService // injected by Spring


    def list(Map params) {

        log.trace "ThingWrapperService.list invoked with params $params"

        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
        def result

        def things = Thing.list(fetch: [parts: "eager"],
                                        max: params.max,
                                        offset: params.offset ).sort { it.id }

        // We'll use the list of things for two different purposes:
        // 1) We'll have each wrapper hold onto the entire list
        // 2) We'll create a wrapper for each thing instance, to return a list
        //
        result = [] as List
        things?.eachWithIndex { thing, index ->
            def wrapper = new ThingWrapper( complexCode: "C-${index}",
                                              buildDate: new Date(),
                                              things: things )
            result << wrapper
        }

        log.trace "ThingWrapperService.list returning ${result}"
        result
    }

    def count(Map params) {
        log.trace "ThingWrapperService.count invoked"
        Thing.count()
    }


    def show(Map params) {

        log.trace "ThingWrapperService.show invoked"
        def result
        def things = Thing.list(fetch: [parts: "eager"],
                                        max: params.max,
                                        offset: params.offset ).sort { it.id }

        result = new ThingWrapper( complexCode: "C-{index}",
                                   buildDate: new Date(),
                                   things: things )
        log.trace "ThingWrapperService.show returning ${result}"
        result
    }


    def create(Map content, Map params) {

        log.trace "ThingWrapperService.create invoked"

        if (WebUtils.retrieveGrailsWebRequest().getParameterMap().forceGenericError == 'y') {
            throw new Exception( "generic failure" )
        }

        def result
        def things = []
        Thing.withTransaction {
            content.things?.each {
                def thing = thingService.create(it, params)
                things << thing
            }
            result = new ThingWrapper(complexCode: content.complexCode,
                                      things: things)
        }
        result
    }

    def update(Map content, Map params) {
        throw new RuntimeException("Not implemented!")
    }

    def delete(Map content, Map params) {
        throw new RuntimeException("Not implemented!")
    }

}
