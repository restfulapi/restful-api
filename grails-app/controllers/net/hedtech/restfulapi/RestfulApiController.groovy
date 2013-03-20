/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import grails.converters.JSON
import grails.converters.XML
import grails.validation.ValidationException

import javax.annotation.PostConstruct

import net.hedtech.restfulapi.config.*

import net.hedtech.restfulapi.extractors.*
import net.hedtech.restfulapi.extractors.configuration.*

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject

import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.beans.factory.InitializingBean

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException

import org.codehaus.groovy.grails.web.servlet.HttpHeaders

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.apache.commons.logging.LogFactory


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

    private mediaTypeParser = new MediaTypeParser()

    private RestConfig restConfig

    private messageLog = LogFactory.getLog( 'RestfulApiController_messageLog' )

    private static final String RESPONSE_REPRESENTATION = 'net.hedtech.restfulapi.RestfulApiController.response_representation'

    @PostConstruct
    void init() {
        restConfig = RestConfig.parse( grailsApplication, grailsApplication.config.restfulApiConfig )
        restConfig.validate()

        restConfig.resources.values().each() { resource ->
            resource.representations.values().each() { representation ->
                switch(representation.mediaType) {
                    case ~/.*json$/:
                        JSON.createNamedConfig("restfulapi:" + resource.name + ":" + representation.mediaType) { json ->
                            representation.marshallers.each() {
                                json.registerObjectMarshaller(it.marshaller,it.priority)
                            }
                        }
                        JSONExtractorConfigurationHolder.registerExtractor(resource.name, representation.mediaType, representation.extractor )
                    break
                    case ~/.*xml$/:
                        XML.createNamedConfig("restfulapi:" + resource.name + ":" + representation.mediaType) { xml ->
                            representation.marshallers.each() {
                                xml.registerObjectMarshaller(it.marshaller,it.priority)
                            }
                        }
                        XMLExtractorConfigurationHolder.registerExtractor(resource.name, representation.mediaType, representation.extractor )
                    break
                    default:
                        throw new RuntimeException("Cannot support media type ${representation.mediaType} in resource ${resource.name}.  All media types must end in xml or json.")
                }
            }
        }

        JSON.createNamedConfig('restapi-error:json') {
        }

        XML.createNamedConfig('restapi-error:xml') {
            it.registerObjectMarshaller(new net.hedtech.restfulapi.marshallers.xml.JSONObjectMarshaller(), 200)
        }

    }


// ---------------------------------- ACTIONS ---------------------------------


    // GET /api/pluralizedResourceName
    //
    public def list() {

        log.trace "list invoked for ${params.pluralizedResourceName}"
        try {
            getResponseRepresentation()
            def service = getService()
            log.trace "... will delegate list() to service $service"
            def result = service.list(params)
            def count = service.count()
            log.trace "... service returned $result"
            ResponseHolder holder = new ResponseHolder()
            holder.data = result
            holder.addHeader('X-hedtech-totalCount',count)
            holder.addHeader('X-hedtech-pageOffset',params.max ? params?.max : result.count)
            holder.addHeader('X-hedtech-pageMaxSize',params.offset ? params?.offset : 0)
            renderSuccessResponse( holder, 'default.rest.list.message' )
        }
        catch (e) {
            messageLog.error "Caught exception ${e.message}", e
            renderErrorResponse(e)
            return
        }


   }


    // GET /api/pluralizedResourceName/id
    //
    public def show() {
        log.trace "show() invoked for ${params.pluralizedResourceName}/${params.id}"
        def result
        try {
            getResponseRepresentation()
            result = getService().show(params)
            renderSuccessResponse( new ResponseHolder( data: result ),
                                   'default.rest.shown.message' )
        }
        catch (e) {
            messageLog.error "Caught exception ${e.message}", e
            renderErrorResponse(e)
        }


    }


    // POST /api/pluralizedResourceName
    //
    public def save() {
        log.trace "save() invoked for ${params.pluralizedResourceName}"
        def result

        try {
            def content = parseRequestContent( request )
            getResponseRepresentation()
            result = getService().create( content )
            response.setStatus( 201 )
            renderSuccessResponse( new ResponseHolder( data: result ),
                                   'default.rest.saved.message' )
        }
        catch (e) {
            messageLog.error "Caught exception ${e.message}", e
            renderErrorResponse(e)
        }

    }


    // PUT/PATCH /api/pluralizedResourceName/id
    //
    public def update() {
        log.trace "update() invoked for ${params.pluralizedResourceName}/${params.id}"
        def result

        try {
            def content = parseRequestContent( request )
            if (content.id && content.id != params.id) {
                // TODO: Handle this exception -- temporarilly just throwing RuntimeException
                throw new RuntimeException("Bad request - ID on url differs from ID in content")
            }

            getResponseRepresentation()
            result = getService().update( params.id, content )
            response.setStatus( 200 )
            renderSuccessResponse( new ResponseHolder( data: result ),
                                   'default.rest.updated.message' )
        }
        catch (e) {
            messageLog.error "Caught exception ${e.message}", e
            renderErrorResponse(e)
        }
    }


    // DELETE /api/pluralizedResourceName/id
    //
    public def delete() {
        log.trace "delete() invoked for ${params.pluralizedResourceName}/${params.id}"
        try {
            def map = [:]
            map.id = params.id
            def content = parseRequestContent( request )
            if (content && content.id && content.id != params.id) {
                // TODO: Handle this exception -- temporarilly just throwing RuntimeException
                throw new RuntimeException("Bad request - ID on url differs from ID in content")
            }
            getService().delete( params.id, content )
            response.setStatus( 200 )
            renderSuccessResponse( new ResponseHolder(), 'default.rest.deleted.message' )
        }
        catch (e) {
            messageLog.error "Caught exception ${e.message}", e
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
        ResponseHolder responseHolder = createErrorResponse( e )
         //The versioning applies to resource representations, not to
         //errors.  In fact, it can't, as the error may be that an unrecognized format
         //was requested.  So if we are returning an error response, we switch the format
         //to either json or xml.
         //So we will look at the Accept-Header directly and try to determine if JSON or XML was
         //requested.  If we can't decide, we will return JSON.
         String contentType = null
         def content
         MediaType[] acceptedTypes = mediaTypeParser.parse(request.getHeader(HttpHeaders.ACCEPT))
         switch(acceptedTypes[0].name) {
            case ~/.*xml.*/:
                contentType = 'application/xml'
                if (responseHolder.data != null) {
                    def jsonObject
                    useJSON("restapi-error:json") {
                        def s = (responseHolder.data as JSON) as String
                        jsonObject = toJSONElement(s)
                    }
                    useXML("restapi-error:xml") {
                        content = jsonObject as XML
                    }
                }
            break
            default:
                contentType = 'application/json'
                if (responseHolder.data != null) {
                    useJSON("restapi-error:json") {
                        content = responseHolder.data as JSON
                    }
                }
            break
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

     protected ResponseHolder createErrorResponse( Throwable e ) {
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
         return responseHolder
     }

     protected def generateResponseContent( RepresentationConfig representation, def data ) {
        def content
        switch(representation.mediaType) {
            case ~/.*json$/:
                useJSON(representation) {
                    content = data as JSON
                }
                break
            case ~/.*xml$/:
                def objectToRender = data
                if (representation.jsonAsXml) {
                    def jsonRepresentation = getJsonEquivalent( representation )
                    useJSON(jsonRepresentation) {
                        def s = (data as JSON) as String
                        objectToRender = toJSONElement(s)
                    }
                }

                useXML(representation) {
                    content = objectToRender as XML
                }
                break
            default:
                unsupportedResponseRepresentation()
                break
        }
        return content
     }


    /**
     * Renders the content of the supplied map using a registered converter.
     * @param responseMap the Map containing the data and headers to render
     * @param format if specified, use the as the response format.  Otherwise
     *        use the format on the response (taken from the Accept-Header)
     * @param mediaType if specified, use as the media type for the response.
    *         Otherwise, use the media-type type specified by the Accept header.
     **/
    protected void renderResponse(ResponseHolder responseHolder) {
        //def acceptedTypes = mediaTypeParser.parse( request.getHeader(HttpHeaders.ACCEPT) )
        def representation
        def content

        if (responseHolder.data != null) {
            representation = getResponseRepresentation()
            content = generateResponseContent( representation, responseHolder.data )
        }

        def contentType = selectResponseContentType( representation )

        if (content != null) {
            response.addHeader( 'X-hedtech-Media-Type', representation.mediaType )
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

    private String selectResponseContentType( RepresentationConfig representation ) {
        //Select the content type
        //Content type will always be application/json or application/xml
        //to make it easy for a response to be displayed in a browser or other tools
        //The X-hedtech-Media-Type header will hold the custom media type (if any)
        //describing the content in more detail.
        def mediaType = representation ? representation.mediaType : 'json'
        def contentType
        switch(mediaType) {
            case ~/.*json$/:
                contentType = "application/json"
            break
            case ~/.*xml$/:
                contentType = "application/xml"
            break
            default:
                contentType = "application/json"
            break
        }
        return contentType
    }

    /**
     * Parses the content from the request.
     * Returns a map representing the properties of content.
     * @param request the request containing the content
     **/
    protected Map parseRequestContent(request) {
        def representation = getRequestRepresentation()
        switch(representation.mediaType) {
            case ~/.*json$/:
                return extractJSON( representation.mediaType )
            break
            case ~/.*xml$/:
                Map content = extractXML( representation.mediaType )
                if (representation.jsonAsXml) {
                    def jsonMediaType = getJSONMediaType( representation.mediaType )
                    content = extractJSON( content, jsonMediaType )
                }
                return content
            break
            default:
                unsupportedRequestRepresentation()
            break
        }
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

    /**
     * Returns the best match, or null if no supported representation for the resource exists.
     **/
    protected RepresentationConfig getRepresentation(pluralizedResourceName, allowedTypes) {
        return restConfig.getRepresentation( pluralizedResourceName, allowedTypes.name )
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

    private Object useJSON(RepresentationConfig config, Closure closure) {
        useJSON( "restfulapi:" + params.pluralizedResourceName + ":" + config.mediaType, closure )
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

    private Object useXML( RepresentationConfig config, Closure closure ) {
        useXML( "restfulapi:" + params.pluralizedResourceName + ":" + config.mediaType, closure )
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

    private RepresentationConfig getResponseRepresentation() {
        def representation = request.getAttribute( RESPONSE_REPRESENTATION )
        if (representation == null) {
            def acceptedTypes = mediaTypeParser.parse( request.getHeader(HttpHeaders.ACCEPT) )
            representation = getRepresentation( params.pluralizedResourceName, acceptedTypes )
            if (representation == null) {
                unsupportedResponseRepresentation()
            }
            request.setAttribute( RESPONSE_REPRESENTATION, representation )
        }
        representation
    }

    private RepresentationConfig getRequestRepresentation() {
        def type = mediaTypeParser.parse( request.getHeader(HttpHeaders.CONTENT_TYPE) )[0]
        def representation = getRepresentation( params.pluralizedResourceName, [type] )
        if (representation == null) {
            unsupportedRequestRepresentation()
        }
        return representation
    }

    private RepresentationConfig getJsonEquivalent( RepresentationConfig representation ) {
        def jsonType = getJSONMediaType( representation.mediaType )
        def jsonRepresentation = getRepresentation( params.pluralizedResourceName, [new MediaType( jsonType )] )
        if (!jsonRepresentation) {
            unsupportedResponseRepresentation()
        }
        jsonRepresentation
    }

     private unsupportedResponseRepresentation() {
        throw new UnsupportedResponseRepresentationException( params.pluralizedResourceName, request.getHeader(HttpHeaders.ACCEPT) )
     }


    private unsupportedRequestRepresentation() {
        throw new UnsupportedRequestRepresentationException( params.pluralizedResourceName, request.getHeader(HttpHeaders.CONTENT_TYPE ) )
    }


    private Map extractJSON( String mediaType ) {
        extractJSON( request.JSON, mediaType )
    }

    private Map extractJSON( JSONObject json, String mediaType ) {
        JSONExtractor extractor = JSONExtractorConfigurationHolder.getExtractor( params.pluralizedResourceName, mediaType )
        if (!extractor) {
            unsupportedRequestRepresentation()
        }
        return extractor.extract( json )
    }

    private Map extractXML( String mediaType ) {
        XMLExtractor xmlExtractor = XMLExtractorConfigurationHolder.getExtractor( params.pluralizedResourceName, mediaType )
        if (!xmlExtractor) {
            unsupportedRequestRepresentation()
        }
        return xmlExtractor.extract( request.XML )
    }

    private String getJSONMediaType( String xmlType ) {
        restConfig.getJsonEquivalentMediaType( xmlType )
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
