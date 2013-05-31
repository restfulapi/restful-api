/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
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

    def update(def id, Map content, Map params) {
        throw new RuntimeException("Not yet implemented!")
    }

    def delete(def id, Map content, Map params) {
        throw new RuntimeException("Not yet implemented!")
    }

}
