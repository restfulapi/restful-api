/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 

package net.hedtech.restfulapi

import grails.converters.JSON
import grails.converters.XML
import grails.validation.ValidationException


/**
 * A default Restful API controller that delegates to a 
 * transactional service corresponding to the resource
 * identified on the URL. This controller may be subclassed
 * to create stateless resource-specific controllers when 
 * necessary.  (If a stateful controller is needed, this 
 * should not be used as a base class.)
 **/
class RestfulApiController {

    // Because this controller is stateless, a single instance
    // may be used to handle all requests. 
    //
    static scope = "singleton"


// ---------------------------------- ACTIONS ---------------------------------


    // GET /api/pluralizedResourceName
    //
    public def list() {

        log.trace "list invoked for ${params.pluralizedResourceName}"
        def result
        try {
            result = getService().list(params)
        } 
        catch (e) {
//            log.error "Caught exception ${e.message}", e
            setError(e)
            renderResponse(errorMap(e), 'default.not.listed.message')
            return
        }

        renderResponse( [
                success:     true,
                data:        result.instances,
                totalCount:  result.totalCount,
                pageOffset:  params.offset ? params?.offset : 0,
                pageMaxSize: params.max ? params?.max : totalCount
            ], 'default.list.message' )
   }


    // GET /api/pluralizedResourceName/id
    //
    public def show() { 
        log.trace "show invoked for ${params.pluralizedResourceName}/${params.id}"
        def result
        try {
            result = getService().show(params)
        } 
        catch (e) {
            //log.error "Caught exception ${e.message}", e
            setError(e)
            renderResponse(errorMap(e), 'default.not.shown.message')
        }

        renderResponse( [
                success:     true,
                data:        result.instance,
            ], 'default.shown.message' )
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
            renderResponse( [
                    success:    true,
                    data:       result.instance, 
                    ], 'default.saved.message' )            
        }
        catch (e) {
            //log.error "Caught exception ${e.message}", e
            setError(e)
            renderResponse(errorMap(e), 'default.not.saved.message')
        }

    }


    // PUT/PATCH /api/pluralizedResourceName/id
    //
    public def update() {
        log.trace "update invoked for ${params.pluralizedResourceName}/${params.id}"
        def result

        try {
            def content = parseRequestContent( request )
            result = getService().update( content )
            response.setStatus( 20 )
            renderResponse( [
                    success:    true,
                    data:       result.instance, 
                    ], 'default.updated.message' )            
        }
        catch (e) {
            //log.error "Caught exception ${e.message}", e
            setError(e)
            renderResponse(errorMap(e), 'default.not.updated.message')
        }
    }


    // DELETE /api/pluralizedResourceName/id
    //
    public def delete() {
        log.trace "delete invoked for ${params.pluralizedResourceName}/${params.id}"
        throw new RuntimeException("Not yet implemented!")
    }


// ---------------------------- Helper Methods -------------------------------    


    /**
     * Renders the supplied map using a registered converter.
     * @param responseMap the Map to render
     * @param msgResourceCode the resource code to use to create message entry
     **/
    protected void renderResponse(Map responseMap, String msgResourceCode) {

        String localizedName = localize(domainName())
        
        responseMap << [ message: message( code: msgResourceCode,
                                           args: [ localizedName ] ) ] 

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
     * Returns a default error map that may be used to render a response.
     **/
    protected Map errorMap(e) {

        // TODO: Support different exceptions (validation, optimistic lock, etc.)

        // TODO: Is this the right way to test for a validation exception?
        if (e instanceof ValidationException) {
            [ success: false,
              errors: [ 
                type: "validation",
                resource: [ class: getDomainClass().name, id: params.id ],
                errorMessage: e.message 
              ]
            ]
        } else {
            [ success: false,
              errors: [ 
                type: "general",
                resource: [ class: getDomainClass().name, id: params.id ],
                errorMessage: e.message 
              ]
            ]

        }

    }

    /**
     * Sets the response status and appropriate header fields for 
     * the error.
     * @param e The exception to set response status and headers for
     **/
    protected void setError(e) {
        // We'll set the response status code based upon type of exception...

        // TODO: Is this the correct way to detect a ValidationException?
        if (e instanceof ValidationException) {
            this.response.setStatus( 400 )
            this.response.addHeader( 'X-Status-Reason', 'Validation failed' )
        } else {
            this.response.setStatus( 500 )
        }
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

}
