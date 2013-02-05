/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi


class ComplexThingService {

    def thingService // injected by Spring


    def list(Map params) {

        log.trace "ComplexThingService.list invoked with params $params"

        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
        def result = [:]
        result.instances = ComplexThing.list(fetch: [things: "eager"],
                                             max: params.max,
                                             offset: params.offset ).sort { it.id }
        result.totalCount = result.instances.size()

        log.trace "ComplexThingService.list returning ${result}"
        result
    }


    def show(Map params) {
        log.trace "ThingService.show invoked"
        def result = [:]
        result.instance = ComplexThing.get(params.id)
        result.instance.things // force lazy loading
        log.trace "ThingService.show returning ${result}"
        result
    }

    def create(Map params) {
        throw new RuntimeException("Not yet implemented!")
    }

    def update(Map params) {
        throw new RuntimeException("Not yet implemented!")
    }

    def delete(Map params) {
        throw new RuntimeException("Not yet implemented!")
    }


}
