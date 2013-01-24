/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 

package net.hedtech.restfulapi

import grails.converters.JSON
import grails.converters.XML

import org.modeshape.common.text.Inflector

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
            log.error "Caught exception ${e.message}", e
            this.response.setStatus( 500 )
            renderResponse(errorMap(e), 'default.not.listed.message')
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
            log.error "Caught exception ${e.message}", e
            this.response.setStatus( 500 )
            renderResponse(errorMap(e), 'default.not.shown.message')
        }

        renderResponse( [
                success:     true,
                data:        result.instance,
            ], 'default.shown.message' )
    }


    // POST /api/pluralizedResourceName/id
    //
    public def save() {
        log.trace "save invoked for ${params.pluralizedResourceName}/${params.id}"
        throw new RuntimeException("Not yet implemented!")
    }


    // PUT/PATCH /api/pluralizedResourceName/id
    //
    public def update() {
        log.trace "update invoked for ${params.pluralizedResourceName}/${params.id}"
        throw new RuntimeException("Not yet implemented!")
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

        String singularName = domainName()
        String localizedName = message( code: "${singularName}.label", 
                                        default: "$singularName" )
        
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
     * Returns a default error map that may be used to render a response.
     **/
    protected Map errorMap(e) {

        [ success: false,
          errorMessage: e.message 
        ]
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


    protected String selectFormat() {
        response.format == 'json' ? 'default' : response.format
    }


    private String domainName() {
        def s = Inflector.instance.singularize(params.pluralizedResourceName as Object)
        lowerCamelCase(s)
    }


    // we'll wrap the Inflector method with a closure so we can curry it...
    private def inflectorCamelCase = { String text, boolean capitalizeFirst, 
                                       char... delimChars ->
        Inflector.instance.camelCase( text, capitalizeFirst, delimChars )
    }


    private def lowerCamelCase = 
        inflectorCamelCase.rcurry(false, '_' as char, '-' as char)
}
