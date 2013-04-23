/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
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

        def queryStatement = HQLBuilder.createHQL( grailsApplication, params )
        def result = PartOfThing.executeQuery( queryStatement, [], params ) // TODO: Sorting, paging
        log.trace "PartOfThingService.list returning ${result} of class ${result.getClass()}"
        result
    }


    def count(Map params) {

        log.trace "PartOfThingService.count invoked"
        if (params.pluralizedResourceName) {
            // this may be a RESTful endpoint query and we can use the restful-api HQLBuilder
            def queryStatement = HQLBuilder.createHQL( grailsApplication, params, true /*count*/ )
            def argMap = params.clone()
            if (argMap.max) argMap.remove('max')
            if (argMap.offset) argMap.remove('offset')
            def countResult = PartOfThing.executeQuery( queryStatement, [], argMap )
            countResult[0]
        }
        else {
            PartOfThing.count()
        }
    }


    def show(Map params) {
        log.trace "PartOfThingService.show invoked"
        def result
        result = Thing.get(params.id)
        result.parts // force lazy loading
        supplementThing( result )
        log.trace "PartOfThingService.show returning ${result}"
        result
    }


    def create(Map content) {
        log.trace "PartOfThingService.create invoked"

        if (WebUtils.retrieveGrailsWebRequest().getParameterMap().forceGenericError == 'y') {
            throw new Exception( "generic failure" )
        }

        def result

        Thing.withTransaction {
            def instance = new Thing( content )
            instance.parts = [] as Set //workaround for GRAILS-9775 until the bindable constraint works on associtations
            content['parts'].each { partID ->
                instance.addPart( PartOfThing.get( partID ) )
            }
            instance.save(failOnError:true)
            result = instance
            result.parts //force lazy loading
        }
        supplementThing( result )
        result
    }

    def update(def id, Map content) {
        log.trace "PartOfThingService.update invoked"

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

    void delete(id,Map content) {
        Thing.withTransaction {
            def thing = Thing.get(id)
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
