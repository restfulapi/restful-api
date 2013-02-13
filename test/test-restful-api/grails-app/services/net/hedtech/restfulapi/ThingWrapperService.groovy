/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi

import org.codehaus.groovy.grails.web.util.WebUtils

class ThingWrapperService {

    def thingService // injected by Spring


    def list(Map params) {

        log.trace "ComplexThingService.list invoked with params $params"

        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
        def result = [:] as Map

        def things = Thing.list(fetch: [things: "eager"],
                                        max: params.max,
                                        offset: params.offset ).sort { it.id }

        // We'll use the list of things for two different purposes:
        // 1) We'll have each wrapper hold onto the entire list
        // 2) We'll create a wrapper for each thing instance, to return a list
        //
        result.instances = [] as List
        things?.eachWithIndex { thing, index ->
            def wrapper = new ComplexWrapper( complexCode: "C-{$index}",
                                              buildDate: new Date(),
                                              things: things )
            result.instances << wrapper
        }

        result.totalCount = result.instances.size()
        log.trace "ComplexWrapperService.list returning ${result}"
        result
    }


    def show(Map params) {
        log.trace "ThingWrapperService.show invoked"
        def result = [:]
        def things = Thing.list(fetch: [things: "eager"],
                                        max: params.max,
                                        offset: params.offset ).sort { it.id }

        result.instance = new ComplexWrapper( complexCode: "C-{index}",
                                              buildDate: new Date(),
                                              things: things )
        log.trace "ThingWrapperService.show returning ${result}"
        result
    }


    def create(Map params) {
        log.trace "ThingWrapperService.create invoked"

        if (WebUtils.retrieveGrailsWebRequest().getParameterMap().forceGenericError == 'y') {
            throw new Exception( "generic failure" )
        }

        def result
        def things = []
        Thing.withTransaction {
            params.things?.each {
                def thing = new Thing(it)
                thing.save(failOnError:true)
                things << thing
            }
            def instance = new ThingWrapper(complexCode: params.complexCode,
                                            things: things)
            result = [:]
            result.instance = instance
        }
        result
    }

    def update(Map params) {
        throw new RuntimeException("Not yet implemented!")
    }

    def delete(Map params) {
        throw new RuntimeException("Not yet implemented!")
    }


}
