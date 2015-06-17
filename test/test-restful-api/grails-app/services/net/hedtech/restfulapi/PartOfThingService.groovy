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

import net.hedtech.restfulapi.query.HQLBuilder

import org.codehaus.groovy.grails.web.util.WebUtils

import org.hibernate.StaleObjectStateException

import org.springframework.dao.OptimisticLockingFailureException

import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException as OptimisticLockException

import java.security.*

class PartOfThingService {

    def grailsApplication


    def list(Map params) {

        log.trace "PartOfThingService.list invoked with params $params"
        params.max = Math.min( params.max ? params.max.toInteger() : 10,  100)
        params.offset = params.offset ? params.offset.toInteger() : 0

        def query = HQLBuilder.createHQL( grailsApplication, params )
        def result = PartOfThing.executeQuery( query.statement, query.parameters, params )
        log.trace "PartOfThingService.list returning ${result} of class ${result.getClass()}"
        result
    }


    def count(Map params) {

        log.trace "PartOfThingService.count invoked"
        if (params.pluralizedResourceName) {
            // this may be a RESTful endpoint query and we can use the restful-api HQLBuilder
            def query = HQLBuilder.createHQL( grailsApplication, params, true /*count*/ )
            def argMap = params.clone()
            if (argMap.max) argMap.remove('max')
            if (argMap.offset) argMap.remove('offset')
            def countResult = PartOfThing.executeQuery( query.statement, query.parameters, argMap )
            countResult[0]
        }
        else {
            PartOfThing.count()
        }
    }


    def show(Map params) {

        log.trace "PartOfThingService.show invoked"
        def result = PartOfThing.get(params.id)
        log.trace "PartOfThingService.show returning ${result}"
        result
    }


    def create(Map content, Map params) {

        log.trace "PartOfThingService.create invoked"

        if (WebUtils.retrieveGrailsWebRequest().getParameterMap().forceGenericError == 'y') {
            throw new Exception( "generic failure" )
        }

        def result
        PartOfThing.withTransaction {
            def instance = new PartOfThing( content )
            instance.save(failOnError:true)
            result = instance
        }
        result
    }


    def update(Map content, Map params) {

        log.trace "PartOfThingService.update invoked"
        checkForExceptionRequest()

        def result
        Thing.withTransaction {
            def thing = Thing.get(param.id)

            checkOptimisticLock( thing, content )

            thing.properties = content
            thing.save(failOnError:true)
            result = thing
            result.parts //force lazy loading
        }
        supplementThing( result )
        result
    }


    void delete(Map content, Map params) {

        Thing.withTransaction {
            def thing = Thing.get(param.id)
            thing.delete(failOnError:true)
        }
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


    protected Class getDomainClass(String pluralizedResourceName) {
        def domainClass = getGrailsDomainClass(pluralizedResourceName)?.clazz
        if (!domainClass) domainClass = grailsApplication.allClasses.find { it.getClass().simpleName == className }
        domainClass ? domainClass : ''.getClass()
    }


    protected Class getGrailsDomainClass(String pluralizedResourceName) {
        def singularizedName = Inflector.singularize(pluralizedResourceName)
        def className = Inflector.camelCase(singularizedName, true)
        grailsApplication.domainClasses.find { it.clazz.simpleName == className }
    }


    private String domainName(String pluralizedResourceName) {
        Inflector.asPropertyName(pluralizedResourceName)
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
