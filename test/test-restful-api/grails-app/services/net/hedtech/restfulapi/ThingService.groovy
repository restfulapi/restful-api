/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi


import grails.validation.ValidationException

import org.codehaus.groovy.grails.web.util.WebUtils

import org.hibernate.StaleObjectStateException

import org.springframework.dao.OptimisticLockingFailureException

import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException as OptimisticLockException

import java.security.*

class ThingService {

    def list( Map params ) {

        log.trace "ThingService.list invoked with params $params"
        def result

        // TODO: Do validation testing in create or update -- this is temporary
        if (params.forceValidationError == 'y') {
            // This will throw a validation exception...
            new Thing(code:'FAIL', description: 'Code exceeds 2 chars').save(failOnError:true)
        }
        def max = Math.min( params.max ? params.max.toInteger() : 100,  100)
        def offset = params.offset ?: 0
        result = Thing.list( offset: offset, max: max, sort: 'code' )

        result.each {
            supplementThing( it )
        }

        log.trace "ThingService.list is returning a ${result.getClass().simpleName} containing ${result.size()} things"
        result
    }


    def count( Map params ) {
        log.trace "ThingService.count invoked"
        Thing.count()
    }


    def show( Map params ) {

        log.trace "ThingService.show invoked"
        def result
        result = Thing.get(params.id)
        result.parts // force lazy loading
        supplementThing( result )
        log.trace "ThingService.show returning ${result}"
        result
    }


    def create( Map content, Map params ) {

        log.trace "ThingService.create invoked"

        if (WebUtils.retrieveGrailsWebRequest().getParameterMap().forceGenericError == 'y') {
            throw new Exception( "generic failure" )
        }

        def result

        Thing.withTransaction {
            def instance = new Thing()
            instance.properties['code','description'] = content
            if (content['parts']) {
                content['parts']?.each { partMap ->
                    def part = new PartOfThing()
                    part.properties = partMap
                    part.thing = instance
                    instance.addToParts( part )
                }
            }
            instance.save(failOnError:true)
            result = instance
            if (content['parts']) result.parts //force lazy loading
            else result.parts = [] as Set
        }
        supplementThing( result )
        log.trace "ThingService.create returning $result"
        result
    }


    def update( def id, Map content, Map params ) {

        log.trace "ThingService.update invoked"
        checkForExceptionRequest()

        def result
        Thing.withTransaction {
            def thing = Thing.get(id)

            checkOptimisticLock( thing, content )

            thing.properties = content
            thing.save(failOnError:true)
            result = thing
            result.parts //force lazy loading
        }
        supplementThing( result )
        result
    }


    void delete( def id, Map content, Map params ) {

        Thing.withTransaction {
            def thing = Thing.get(id)
            thing.delete(failOnError:true)
        }
    }


    public def checkOptimisticLock( domainObject, content ) {

        if (domainObject.hasProperty( 'version' )) {
            if (!content?.version) {
                domainObject.errors.reject( 'version', "net.hedtech.restfulapi.Thing.missingVersion")
                throw new ValidationException( "Missing version field", domainObject.errors )
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
        def params = WebUtils.retrieveGrailsWebRequest().getParameterMap()
        if (params.throwOptimisticLock == 'y') {
            throw new OptimisticLockingFailureException( "requested optimistic lock for testing" )
        }
        if (params.throwApplicationException == 'y') {
            throw new DummyApplicationException( params.appStatusCode, params.appMsgCode, params.appErrorType )
        }
    }


    private void supplementThing( Thing thing ) {
        MessageDigest digest = MessageDigest.getInstance("SHA1")
        digest.update("code:${thing.getCode()}".getBytes("UTF-8"))
        digest.update("description${thing.getDescription()}".getBytes("UTF-8"))
        def properties = [sha1:new BigInteger(1,digest.digest()).toString(16).padLeft(40,'0')]
        thing.metaClass.getSupplementalRestProperties << {-> properties }
    }
}
