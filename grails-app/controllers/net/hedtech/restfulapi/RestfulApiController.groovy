/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 

package net.hedtech.restfulapi

import grails.converters.JSON
import grails.converters.XML
import grails.validation.ValidationException

import org.springframework.dao.OptimisticLockingFailureException


/**
 * A default Restful API controller that delegates to a 
 * transactional service corresponding to the resource
 * identified on the URL. This controller may be subclassed
 * to create stateless resource-specific controllers when 
 * necessary.  (If a stateful controller is needed, this 
 * should not be used as a base class.)
 **/
class RestfulApiController implements org.springframework.beans.factory.InitializingBean {

    // Because this controller is stateless, a single instance
    // may be used to handle all requests. 
    //
    static scope = "singleton"

    private def defaultOptimisticLockExceptions = [
        org.springframework.dao.OptimisticLockingFailureException.class.getName(),
        'org.hibernate.StaleObjectStateException'
    ]

    private def optimisticLockClasses = []

    void afterPropertiesSet() {
        def clazzes = []
        log.info ""
        def names = []
        //see if the configuration wants to completely override the classes to be mapped
        //to the optimistic lock handler, otherwise, use the default set.
        if (grailsApplication.config.grails.restfulapi.optimisticLockExceptions) {
            names.addAll( grailsApplication.config.grails.restfulapi.optimisticLockExceptions )
        } else {
            names.addAll( defaultOptimisticLockExceptions )
        }

        //Add additional classes to the default set.
        if (grailsApplication.config.grails.restfulapi.addOptimisticLockExceptions) {
            names.addAll( grailsApplication.config.grails.restfulapi.addOptimisticLockExceptions )
        }

        names.each() { name->
            log.info( "Loading class '$name' as an Optimistic lock exception" )
            optimisticLockClasses.add( grailsApplication.getClassLoader().loadClass( name ) )
        }
    }


// ---------------------------------- ACTIONS ---------------------------------


    // GET /api/pluralizedResourceName
    //
    public def list() {

        log.trace "list invoked for ${params.pluralizedResourceName}"
        def result
        try {
            result = getService().list(params)

            renderSuccessResponse( [
                success:     true,
                data:        result.instances,
                totalCount:  result.totalCount,
                pageOffset:  params.offset ? params?.offset : 0,
                pageMaxSize: params.max ? params?.max : totalCount
            ], 'default.list.message' )            
        } 
        catch (e) {
//            log.error "Caught exception ${e.message}", e
            renderErrorResponse(e)
            return
        }


   }


    // GET /api/pluralizedResourceName/id
    //
    public def show() { 
        log.trace "show invoked for ${params.pluralizedResourceName}/${params.id}"
        def result
        try {
            result = getService().show(params)

            renderSuccessResponse( [
                success:     true,
                data:        result.instance,
            ], 'default.shown.message' )
        } 
        catch (e) {
            //log.error "Caught exception ${e.message}", e
            renderErrorResponse(e)
        }

        
    }


    // POST /api/pluralizedResourceName
    //
    public def save() {
        log.trace "save invoked for ${params.pluralizedResourceName}"
        def result

        try {
            def content = parseRequestContent( request )
            result = getService().create( content )
            response.setStatus( 201 )
            renderSuccessResponse( [
                    success:    true,
                    data:       result.instance, 
                    ], 'default.saved.message' )            
        }
        catch (e) {
            //log.error "Caught exception ${e.message}", e
            renderErrorResponse(e)
        }

    }


    // PUT/PATCH /api/pluralizedResourceName/id
    //
    public def update() {
        log.trace "update invoked for ${params.pluralizedResourceName}/${params.id}"
        def result

        try {
            def map = [:]
            map.id = params.id
            map.content = parseRequestContent( request )
            result = getService().update( map )
            response.setStatus( 200 )
            renderSuccessResponse( [
                    success:    true,
                    data:       result.instance, 
                    ], 'default.updated.message' )            
        }
        catch (e) {
            //log.error "Caught exception ${e.message}", e
            renderErrorResponse(e)
        }
    }


    // DELETE /api/pluralizedResourceName/id
    //
    public def delete() {
        log.trace "delete invoked for ${params.pluralizedResourceName}/${params.id}"
        def result
        try {
            def map = [:]
            map.id = params.id
            map.content = parseRequestContent( request )
            result = getService().delete( map )
            response.setStatus( 200 )
            renderSuccessResponse( [
                success:    true], 'default.deleted.mesage' )
        }
        catch (e) {
            //log.error "Caught exception ${e.message}", e
            renderErrorResponse(e)
        }
    }


// ---------------------------- Helper Methods -------------------------------    

    /**
     * Renders a successful response using the supplied map and the msg resource
     * code.  
     * A success value of true will be automatically added to the map.
     * A message property with a value translated from the message resource code
     * provided with a localized domainName will be automatically added to the map.
     * @param responseMap the Map to render
     * @param msgResourceCode the resource code used to create a message entry
     **/
     protected void renderSuccessResponse(Map responseMap, String msgResourceCode) {

        String localizedName = localize(domainName())

        responseMap << [ success: true ]
        
        responseMap << [ message: message( code: msgResourceCode,
                                           args: [ localizedName ] ) ] 
        renderResponse( responseMap )
     }


    /**
     * Renders the supplied map using a registered converter.
     * @param responseMap the Map to render
     * @param msgResourceCode the resource code to use to create message entry
     **/
    protected void renderResponse(Map responseMap ) {

        switch(response.format) {
            case 'json':
                render responseMap as JSON
            break
            case ~/.*json.*/:
                JSON.use(selectFormat()) {
                    render responseMap as JSON
                }
            break
            case 'xml':
                render responseMap as XML
            break
            case ~/.*xml.*/:
                XML.use(selectFormat()) {
                    render responseMap as XML
                }
            break
        }
    }

    /**
     * Parses the content from the request.
     * Returns a map representing the properties of content.
     * @param request the request containing the content
     **/
    protected Map parseRequestContent(request) {
        switch(request.format) {
            case 'json':
                return request.JSON
            break
        }
        throw new RuntimeException( "unknown request format ${request.format}")
    }

    /**
     * Renders an error response appropriate for the exception.
     * @param e the exception to render an error response for
     **/
    protected void renderErrorResponse( Throwable e ) {
        def handler = exceptionHandlers[ getErrorType( e ) ] 
        if (!handler) {
            handler = exceptionHandlers[ 'AnyOtherException' ]
        }
        this.response.setStatus( handler.httpStatusCode )
        if (handler.additionalHeaders) {
            handler.additionalHeaders().each() { header ->
                this.response.addHeader( header.key, header.value )
            }
        }
        def returnMap = handler.returnMap( e )
        returnMap << [ success : false ]
        renderResponse( returnMap )
    }

    /**
     * Maps an exception to an error type known to the controller.
     * @param e the exception to map
     **/
    protected String getErrorType(e) {
        if (isInstanceOf(e, this.optimisticLockClasses)) {
            return 'OptimisticLockException'
        } else if (e instanceof ValidationException) {
            return 'ValidationException'
        } else {
            return 'AnyOtherException'
        }
    }

    private boolean isInstanceOf( Throwable e, def classes ) {
        def isInstanceOf = false;
        classes.each { clazz ->
            if (clazz.isAssignableFrom( e.class )) {
                isInstanceOf = true
            }
        }
        return isInstanceOf
    }


    /**
     * Returns the transactional service corresponding to this resource.
     * The default implementation assumes the resource is a Grails 'domain' 
     * object, and that the service can be identified by using the pluralized
     * 'resource' name found on the URL. 
     * For example: If a URL of /api/pluralizedResourceName/id was invoked, 
     * a service named 
     * 'SingularizedResourceNameController' will be retrieved from the IoC container.
     **/
    protected def getService() {

        def svcName = "${domainName()}Service"
        def svc
        try {
            svc = applicationContext.getBean(svcName)
        } catch (e) {
            log.error "Caught exception ${e.message}", e
            //throw e
        }
        log.trace "getService() is returning $svc"
        svc
    }


    protected Class getDomainClass() {
        def singularizedName = Inflector.singularize(params.pluralizedResourceName)
        def className = Inflector.camelCase(singularizedName, true)
        grailsApplication.domainClasses.find { it.clazz.simpleName == className }.clazz
    }


    private String localize(String name) {
        message( code: "${name}.label", default: "$name" )
    }

    protected String selectFormat() {
        response.format == 'json' ? 'default' : response.format
    }


    private String domainName() {
        Inflector.asPropertyName(params.pluralizedResourceName)
    }

    private def exceptionHandlers = [

        'ValidationException': [
            httpStatusCode: 400,
            additionalHeaders: { ['X-Status-Reason':'Validation failed'] },
            returnMap: { e -> 
                            [   success: false,

                                message: message( code: "default.validation.errors.message",
                                                  args: [ localize(domainName()) ] ) as String,
                                errors: [ [ 
                                    type: "validation",
                                    resource: [ class: getDomainClass().name, id: params.id ],
                                    errorMessage: e.message 
                                    ]
                                ]
                            ]
                        }
        ],

        'OptimisticLockException': [
            httpStatusCode: 409,
            returnMap: { e -> 
                            [   success: false,
                                message: message( code: "default.optimistic.locking.failure",
                                                  args: [ localize(domainName()) ] ) as String,
                            ]
                        }
        ],

        // Catch-all.  Unknown exception type.
        'AnyOtherException': [
            httpStatusCode: 500,
            returnMap: { e ->
                            [   success: false,
                                message: message( code: "default.general.errors.message",
                                                  args: [ localize(domainName()) ] ) as String,
                                errors: [ [ 
                                    type: "general",
                                    resource: [ class: getDomainClass().name, id: params.id ],
                                    errorMessage: e.message 
                                ] ]
                            ]
                        }
            ]
    ]

}
