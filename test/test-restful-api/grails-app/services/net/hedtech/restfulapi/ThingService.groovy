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


import grails.validation.ValidationException

import java.security.*

import net.hedtech.restfulapi.query.Filter
import net.hedtech.restfulapi.query.HQLBuilder

import org.codehaus.groovy.grails.web.util.WebUtils

import org.hibernate.StaleObjectStateException

import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException as OptimisticLockException


class ThingService {

    def grailsApplication


    def list( Map params ) {

        log.trace "ThingService.list invoked with params $params"

        if (params.forceValidationError == 'y') {
            // This will throw a validation exception...
            new Thing( code:'FAIL', description: 'Code exceeds 2 chars' ).save( failOnError:true )
        }
        params.max    = Math.min( params.max ? params.max.toInteger() : 100,  100 )
        params.offset = params.offset ?: 0
        params.sort   = params.sort ?: 'code'

        if (params.pluralizedResourceName != 'things') {
            params.domainClass = Filter.getGrailsDomainClass( grailsApplication, 'things' )
        }
        log.trace "ThingService.list will query using adjusted params: $params"
        def query  = HQLBuilder.createHQL( grailsApplication, params )
        def result = Thing.executeQuery( query.statement, query.parameters, params )

        result.each {
            supplementThing( it )
        }

        log.trace "ThingService.list is returning a ${result.getClass().simpleName} containing ${result.size()} things"
        result
    }


    def count( Map params ) {
        log.trace "ThingService.count invoked"
        if (params.pluralizedResourceName != 'things') {
            params.domainClass = Filter.getGrailsDomainClass( grailsApplication, 'things' )
        }
        def query = HQLBuilder.createHQL( grailsApplication, params, true /*count*/ )
        def argMap = params.clone()
        if (argMap.max) argMap.remove('max')
        if (argMap.offset) argMap.remove('offset')
        def countResult = Thing.executeQuery( query.statement, query.parameters, argMap )
        countResult[0]
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


    def update( Map content, Map params ) {

        log.trace "ThingService.update invoked"
        checkForExceptionRequest()

        def result
        Thing.withTransaction {
            def thing = Thing.get(params.id)

            checkOptimisticLock( thing, content )

            thing.properties = content
            thing.save(failOnError:true)
            result = thing
            result.parts //force lazy loading
        }
        supplementThing( result )
        result
    }


    void delete( Map content, Map params ) {

        Thing.withTransaction {
            def thing = Thing.get(params.id)
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
        String tenant = TenantContext.get() ?: 'not-specified'
        def properties = [ tenant: tenant ?: 'not-specified',
                           sha1:new BigInteger( 1, digest.digest() ).toString(16).padLeft(40,'0') ]
        thing.metaClass.getSupplementalRestProperties << { -> properties }
    }
}
