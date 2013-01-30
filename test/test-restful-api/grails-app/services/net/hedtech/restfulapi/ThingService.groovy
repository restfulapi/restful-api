/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 
package net.hedtech.restfulapi


import grails.validation.ValidationException

import org.codehaus.groovy.grails.web.util.WebUtils

import org.hibernate.StaleObjectStateException

import org.springframework.dao.OptimisticLockingFailureException

import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException as OptimisticLockException

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

        checkForExceptionRequest()

        def result = [:]
        Thing.withTransaction {
            def thing = Thing.get(params.id)

            checkOptimisticLock( thing, params.content )

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

    public def checkOptimisticLock( domainObject, content ) {
        
        if (domainObject.hasProperty( 'version' )) {
            if (!content?.version) {
                thing.errors.reject( 'version', "net.hedtech.restfulapi.Thing.missingVersion")
                throw new ValidationException( "Missing version field", thing.errors )
            }            
            int ver = content.version instanceof String ? content.version.toInteger() : content.version
            if (ver != domainObject.version) {
                throw exceptionForOptimisticLock( domainObject, content )
            }
        }
    }

    private def exceptionForOptimisticLock( domainObject, content ) {
        new OptimisticLockException( new StaleObjectStateException( domainObject.class.getName(), domainObject.id ) )
    }

    /**
     * Checks the request for a flag asking for a specific exception to be thrown
     * so error handling can be tested.
     * This is a method to support testing of the plugin, and should not be taken
     * as an example of good service construction.
     **/
    private void checkForExceptionRequest() {
        if (WebUtils.retrieveGrailsWebRequest().getParameterMap().throwOptimisticLock == 'y') {
            throw new OptimisticLockingFailureException( "requested optimistic lock for testing" )
        }
        if (WebUtils.retrieveGrailsWebRequest().getParameterMap().throwStaleObjectStateException == 'y') {
            throw new StaleObjectStateException( Thing.class.getName(), null )
        }

    }
}
