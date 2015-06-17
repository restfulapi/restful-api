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


class ComplexThingService {

    def thingService // injected by Spring


    def list(Map params) {

        log.trace "ComplexThingService.list invoked with params $params"

        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
        def result
        result = ComplexThing.list(fetch: [things: "eager"],
                                   max: params.max,
                                   offset: params.offset ).sort { it.id }

        log.trace "ComplexThingService.list returning ${result}"
        result
    }

    def count(Map params) {
        log.trace "ComplexThingService.count invoked"
        ComplexThing.count()
    }


    def show(Map params) {
        log.trace "ThingService.show invoked"
        def result
        result = ComplexThing.get(params.id)
        result.things // force lazy loading
        log.trace "ThingService.show returning ${result}"
        result
    }

    def create(Map content, Map params) {
        throw new RuntimeException("Not yet implemented!")
    }

    def update(Map content, Map params) {
        throw new RuntimeException("Not yet implemented!")
    }

    def delete(Map content, Map params) {
        throw new RuntimeException("Not yet implemented!")
    }

}
