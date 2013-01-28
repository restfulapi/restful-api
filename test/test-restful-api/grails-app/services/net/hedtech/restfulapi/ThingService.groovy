/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 
package net.hedtech.restfulapi


import grails.validation.ValidationException

import org.codehaus.groovy.grails.web.util.WebUtils

import org.hibernate.StaleObjectStateException

import org.springframework.dao.OptimisticLockingFailureException



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
        if (WebUtils.retrieveGrailsWebRequest().getParameterMap().throwOptimisticLock == 'y') {
            throw new OptimisticLockingFailureException( "requested optimistic lock for testing" )
        }
        if (WebUtils.retrieveGrailsWebRequest().getParameterMap().throwStaleObjectStateException == 'y') {
            throw new StaleObjectStateException()
        }


        /*if (!params.content?.version) {
            throw new ValidationException( "Missing version field" )
        }*/

        def result = [:]
        Thing.withTransaction {
            def thing = Thing.get(params.id)
            thing.properties = params.content
            thing.save(failOnError:true)
            result.instance = thing
            result.instance.parts //force lazy loading
        }
        result
    }

    def delete(Map params) {
        def result = [:]
        Thing.withTransaction {
            def thing = Thing.get(params.id)
            thing.delete(failOnError:true)
        }
        result
    }
}
