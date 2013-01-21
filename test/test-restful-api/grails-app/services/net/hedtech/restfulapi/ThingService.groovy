/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 
package net.hedtech.restfulapi

class ThingService {

    def list(Map params) {

        log.trace "ThingService.list invoked"

        def result = [:]
        def fail = { Map m ->
            result.error = [ code: m.code, args: ["Thing"] ]
            return result
        }
 
        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
        result.instances = Thing.list(fetch: [parts: "eager"], 
        	                          max: params.max, offset: params.offset )
        result.totalCount = Thing.count()
 
        if(!result.instances || !result.totalCount) {
            return fail(code:"default.list.failure")
        }
        log.trace "ThingService.list returning ${result}"
        result
    }


    def show(Map params) {
        log.trace "ThingService.show invoked"

        def result = [:]
        result.instance = Thing.get(params.id)
        result.instance.parts // force lazy loading
        log.trace "ThingService.show returning ${result}"
        result
    }
}
