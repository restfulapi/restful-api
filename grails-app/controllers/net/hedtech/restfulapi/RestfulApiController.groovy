/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import grails.converters.JSON
import grails.converters.XML
import grails.validation.ValidationException

import net.hedtech.restfulapi.extractors.*
import net.hedtech.restfulapi.extractors.configuration.*

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject

import org.springframework.dao.OptimisticLockingFailureException

import org.springframework.dao.OptimisticLockingFailureException

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException

import org.codehaus.groovy.grails.web.servlet.HttpHeaders


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
            ResponseHolder holder = new ResponseHolder()
            holder.data = result.instances
            holder.addHeader('X-hedtech-totalCount',result.totalCount)
            holder.addHeader('X-hedtech-pageOffset',params.max ? params?.max : result.totalCount)
            holder.addHeader('X-hedtech-pageMaxSize',params.offset ? params?.offset : 0)

            renderSuccessResponse( holder, 'default.rest.list.message' )
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
            renderSuccessResponse( new ResponseHolder( data: result.instance ),
                                   'default.rest.shown.message' )
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
            renderSuccessResponse( new ResponseHolder( data: result.instance ),
                                   'default.rest.saved.message' )
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
            renderSuccessResponse( new ResponseHolder( data: result.instance ),
                                   'default.rest.updated.message' )
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
        try {
            def map = [:]
            map.id = params.id
            map.content = parseRequestContent( request )
            getService().delete( map )
            response.setStatus( 200 )
            renderSuccessResponse( new ResponseHolder(), 'default.rest.deleted.message' )
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
     * A message property with a value translated from the message resource code
     * provided with a localized domainName will be automatically added to the map.
     * @param responseMap the Map to render
     * @param msgResourceCode the resource code used to create a message entry
     **/
     protected void renderSuccessResponse(ResponseHolder holder, String msgResourceCode) {

        String localizedName = localize(domainName())
        holder.message = message( code: msgResourceCode, args: [ localizedName ] )
        renderResponse( holder )
     }


     /**
      * Renders an error response appropriate for the exception.
      * @param e the exception to render an error response for
      **/
     protected void renderErrorResponse( Throwable e ) {
        ResponseHolder responseHolder = new ResponseHolder()
        try {
            def handler = exceptionHandlers[ getErrorType( e ) ]
            if (!handler) {
                handler = exceptionHandlers[ 'AnyOtherException' ]
            }
            def result = handler(e)

            if (result.headers) {
                result.headers.each() { header ->
                    responseHolder.addHeader( header.key, header.value )
                }
            }
            responseHolder.data = result.returnMap
            responseHolder.message = result.message
            this.response.setStatus( result.httpStatusCode )
         }
         catch (t) {
            //We generated an exception trying to generate an error response.
            //Log the error, and attempt to fall back on a generic fail-whale response
            log.error( "Caught exception attemping to prepare an error response: ${t.message}", t )
            responseHolder.data = null

            responseHolder.message = message( code: 'default.rest.unexpected.exception.messages' )
            this.response.setStatus( 500 )
         }

         //The versioning applies to resource representations, not to
         //errors.  In fact, it can't, as the error may be that an unrecognized format
         //was requested.  So if we are returning an error response, we switch the format
         //to either json or xml.
         //We can't depend on response.format for this, because if the error was caused by an
         //unknown media type, Grails may have set the format to a fallback option, like 'html'.
         //So we will look at the Accept-Header directly and try to determine if JSON or XML was
         //requested.  If we can't decide, we will return JSON.
         String format = null
         String contentType = null
println "picking response based on " + request.getHeader(HttpHeaders.ACCEPT)
         switch(request.getHeader(HttpHeaders.ACCEPT)) {
            case ~/.*json.*/:
                format = "json"
                contentType = 'application/json'
            break
            case ~/.*xml.*/:
                format = "xml"
                contentType = 'application/xml'
            break
            default:
                format = "json"
                contentType = 'application/json'
            break
         }
println "picked contentType " + contentType
println ""
         renderResponse( responseHolder, format, contentType )
     }


    /**
     * Renders the content of the supplied map using a registered converter.
     * @param responseMap the Map containing the data and headers to render
     * @param format if specified, use the as the response format.  Otherwise
     *        use the format on the response (taken from the Accept-Header)
     * @param mediaType if specified, use as the media type for the response.
    *         Otherwise, use the media-type type specified by the Accept header.
     **/
    protected void renderResponse(ResponseHolder responseHolder, String format=null, String mediaType=null) {
println "rendering with mediaType overridden to " +mediaType
        if (!format) {
            format = response.format
        }
        if (!mediaType) {
            mediaType = request.getHeader(HttpHeaders.ACCEPT)
        }
        def content
        if (responseHolder.data != null) {
            switch(format) {
                case 'json':
                    content = responseHolder.data as JSON
                break
                case ~/.*json.*/:
                    useJSON(selectResponseFormat(format)) {
                        content = responseHolder.data as JSON
                    }
                break
                case 'xml':
                    def s = (responseHolder.data as JSON) as String
                    def json = toJSONElement( s )
                    content = json as XML
                break
                case ~/xml.*/:
                    format = selectResponseFormat(format)
                    def jsonFormat = 'json' + format.substring( 3 )
                    def json
                    useJSON(jsonFormat) {
                        def s = (responseHolder.data as JSON) as String
                        json = toJSONElement( s )
                    }
                    useXML(selectResponseFormat(format)) {
                        content = json as XML
                    }
                break
                case ~/.*xml.*/:
                    useXML(selectResponseFormat(format)) {
                         content = responseHolder.data as XML
                    }
                break
                default:
                    //Default grails behavior for determining response format is to parse the Accept-Header, and if the media
                    //type isn't defined, to fallback on default mime-types.
                    throw new UnsupportedResponseRepresentationException( params.pluralizedResourceName, request.getHeader(HttpHeaders.ACCEPT) )
                break
            }
        }
        //Select the content type
        //Content type will always be application/json or application/xml
        //to make it easy for a response to be displayed in a browser or other tools
        //The X-hedtech-Media-Type header will hold the custom media type (if any)
        //describing the content in more detail.
        def contentType
        switch(mediaType) {
            case ~/application\/.*json.*/:
                contentType = "application/json"
            break
            case ~/application\/.*xml.*/:
                contentType = "application/xml"
            break
            default:
                contentType = "application/json"
            break
        }
        if (content != null && mediaType != null) {
            response.addHeader( 'X-hedtech-Media-Type', mediaType )
        }
        responseHolder.headers.each { header ->
            if (header.value instanceof Collection) {
                header.value.each() { val ->
                    response.addHeader( header.key, val )
                }
            } else {
                response.addHeader( header.key, header.value )
            }
        }
        if (responseHolder.message) {
            response.addHeader( "X-hedtech-message", responseHolder.message )
        }
        render(text: content ? content : "", contentType: contentType )
    }


    /**
     * Parses the content from the request.
     * Returns a map representing the properties of content.
     * @param request the request containing the content
     **/
    protected Map parseRequestContent(request) {
        switch(request.format) {
            case ~/.*json.*/:
                JSONExtractor extractor = JSONExtractorConfigurationHolder.getExtractor( params.pluralizedResourceName, request.format )
                if (!extractor) {
                    throw new UnsupportedRequestRepresentationException( params.pluralizedResourceName, request.contentType )
                }
                return extractor.extract( request.JSON )
            break
            case ~/xml.*/:
                XMLExtractor xmlExtractor = XMLExtractorConfigurationHolder.getExtractor( params.pluralizedResourceName, request.format )
                if (!xmlExtractor) {
                    throw new UnsupportedRequestRepresentationException( params.pluralizedResourceName, request.contentType )
                }
                def json = xmlExtractor.extract( request.XML )
                def format = request.format
                format = 'json' + format.substring( 3 )
                JSONExtractor jsonExtractor = JSONExtractorConfigurationHolder.getExtractor( params.pluralizedResourceName, format )
                if (!jsonExtractor) {
                    throw new UnsupportedRequestRepresentationException( params.pluralizedResourceName, request.contentType )
                }
                return jsonExtractor.extract( json )
            break
            case ~/.*xml.*/:
                XMLExtractor extractor = XMLExtractorConfigurationHolder.getExtractor( params.pluralizedResourceName, request.format )
                if (!extractor) {
                    throw new UnsupportedRequestRepresentationException( params.pluralizedResourceName, request.contentType )
                }
                return extractor.extract( request.XML )
            break
        }
        throw new UnsupportedRequestRepresentationException( params.pluralizedResourceName, request.contentType )

    }



    /**
     * Maps an exception to an error type known to the controller.
     * @param e the exception to map
     **/
    protected String getErrorType(e) {
        if (e.metaClass.respondsTo( e, "getHttpStatusCode") &&
            e.hasProperty( "returnMap" ) &&
            e.returnMap && (e.returnMap instanceof Closure)) {
            //treat as an 'ApplicationException'.  That is, assume the exception is taking
            //responsibility for specifying the correct status code and
            //response message elements
            return 'ApplicationException'
        } else if (e instanceof OptimisticLockingFailureException) {
            return 'OptimisticLockException'
        } else if (e instanceof ValidationException) {
            return 'ValidationException'
        } else if (e instanceof UnsupportedRequestRepresentationException) {
            return 'UnsupportedRequestRepresentationException'
        } else if (e instanceof UnsupportedResponseRepresentationException) {
            return 'UnsupportedResponseRepresentationException'
        } else {
            return 'AnyOtherException'
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
        def domainClass = grailsApplication.domainClasses.find { it.clazz.simpleName == className }?.clazz
        if (!domainClass) domainClass = grailsApplication.allClasses.find { it.getClass().simpleName == className }

        return domainClass ? domainClass : ''.getClass()
    }


    protected Class getNonDomainClass(String className) {

    }


    private String localize(String name) {
        message( code: "${name}.label", default: "$name" )
    }

    protected String selectResponseFormat( String format = null) {
        if (!format) {
            format = response.format
        }
        format == 'json' ? 'default' : format
    }


    private String domainName() {
        Inflector.asPropertyName(params.pluralizedResourceName)
    }

    /**
     * If we try to use an unknown configuration for a grails converter, a ConverterException
     * is thrown, which can't be programmatically distinguished from other marshalling errors.
     * So we'll test for the existence of the named configuration upfront, so if we don't
     * support it, we can return an appropriate error response.
     **/
    private Object useJSON( String config, Closure closure ) {
        try {
            JSON.getNamedConfig( config )
        } catch (ConverterException e) {
            //failure to retrieve the named config.  Treat as an unknown format.
            throw new UnsupportedResponseRepresentationException( params.pluralizedResourceName, request.getHeader(HttpHeaders.ACCEPT) )
        }
        JSON.use(config,closure)
    }

    /**
     * If we try to use an unknown configuration for a grails converter, a ConverterException
     * is thrown, which can't be programmatically distinguished from other marshalling errors.
     * So we'll test for the existence of the named configuration upfront, so if we don't
     * support it, we can return an appropriate error response.
     **/
    private Object useXML( String config, Closure closure ) {
        try {
            XML.getNamedConfig( config )
        } catch (ConverterException e) {
            //failure to retrieve the named config.  Treat as an unknown format.
            throw new UnsupportedResponseRepresentationException( params.pluralizedResourceName, request.getHeader(HttpHeaders.ACCEPT) )
        }
        XML.use(config,closure)
    }

    private JSONElement toJSONElement( String s ) {
        if (s == null || s.trim().size() == 0) {
            return null
        }
        //is there a better way to detect this?
        if (s.startsWith('[')) {
            return new JSONArray(s)
        } else {
            return new JSONObject(s)
        }
    }

    private def exceptionHandlers = [

        'ValidationException': { e->
            [
                httpStatusCode: 400,
                headers: ['X-Status-Reason':'Validation failed'],
                message: message( code: "default.rest.validation.errors.message",
                                          args: [ localize(domainName()) ] ) as String,
                returnMap: [
                    errors: [
                        [
                            type: "validation",
                            resource: [ class: getDomainClass().name, id: params.id ],
                            errorMessage: e.message
                        ]
                    ]
                ]
            ]
        },

        'OptimisticLockException': { e ->
            [
                httpStatusCode: 409,
                message: message( code: "default.optimistic.locking.failure",
                                          args: [ localize(domainName()) ] ) as String,
            ]
        },


        'UnsupportedResponseRepresentationException': { e ->
            [
                httpStatusCode: 406,
                message: message( code: "default.rest.unknownrepresentation.message",
                                          args: [ e.getPluralizedResourceName(), e.getContentType() ] ) as String,
            ]
        },

        'UnsupportedRequestRepresentationException': { e ->
            [
                httpStatusCode: 415,
                message: message( code: "default.rest.unknownrepresentation.message",
                                          args: [ e.getPluralizedResourceName(), e.getContentType() ] ) as String,
            ]
        },

        'ApplicationException': { e ->
            // wrap the 'message' invocation within a closure, so it can be passed into an ApplicationException to localize error messages
            def localizer = { mapToLocalize ->
                this.message( mapToLocalize )
            }

            def map = [:]

            def appMap = e.returnMap( localizer )

            map.httpStatusCode = e.getHttpStatusCode()
            if (appMap.headers) {
                map.headers = appMap.headers
            }
            if (appMap.message) {
                map.message = appMap.message
            }

            def returnMap = [:]
            if (appMap.errors) {
                returnMap.errors = appMap.errors
            }
            map.returnMap = returnMap

            return map
        },

        // Catch-all.  Unknown exception type.
        'AnyOtherException': { e ->
            [
                httpStatusCode: 500,
                message: message( code: "default.rest.general.errors.message",
                                          args: [ localize(domainName()) ] ) as String,
                returnMap: [
                    errors: [ [
                        type: "general",
                        resource: [ class: getDomainClass().name, id: params.id ],
                        errorMessage: e.message
                        ]
                    ]
                ]
            ]
        }
    ]

}
