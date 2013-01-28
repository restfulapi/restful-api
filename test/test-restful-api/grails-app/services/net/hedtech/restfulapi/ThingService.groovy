/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 
package net.hedtech.restfulapi


import grails.validation.ValidationException

import org.codehaus.groovy.grails.web.util.WebUtils



class ThingService {

    def list(Map params) {

        log.trace "ThingService.list invoked with params $params"

        def result = [:]

        // TODO: Do validation testing in create or update -- this is temporary
        if (params.forceValidationError == 'y') {
            // This will throw a validation exception...
            new Thing(code:'FAIL', description: 'Code exceeds 2 chars').save(failOnError:true)
        }
 
        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
        result.instances = Thing.list(fetch: [parts: "eager"], 
        	                          max: params.max, offset: params.offset ).sort { it.id }
        result.totalCount = result.instances.size()
 
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

    def create(Map params) {
        log.trace "ThingService.create invoked"

        if (WebUtils.retrieveGrailsWebRequest().getParameterMap().forceGenericError == 'y') {
            throw new Exception( "generic failure" )
        }

        def result = [:]

        Thing.withTransaction {
            def instance = new Thing( params )
            instance.save(failOnError:true)
            result.instance = instance 
            result.instance.parts //force lazy loading
        }
        result
    }

    def update(Map params) {
        log.trace "ThingService.update invoked"

        def result = [:]
        Thing.withTransaction {
            Thing.get(params.id)
        }

    }
}
