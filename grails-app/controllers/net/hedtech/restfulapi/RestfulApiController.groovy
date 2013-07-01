/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import grails.converters.JSON
import grails.converters.XML
import grails.validation.ValidationException

import java.security.*

import static java.util.UUID.randomUUID

import javax.annotation.PostConstruct

import net.hedtech.restfulapi.config.*

import net.hedtech.restfulapi.extractors.*
import net.hedtech.restfulapi.extractors.configuration.*

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject

import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.beans.factory.InitializingBean

import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.codehaus.groovy.grails.web.converters.configuration.ConverterConfiguration
import org.codehaus.groovy.grails.web.converters.configuration.DefaultConverterConfiguration
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
import org.codehaus.groovy.grails.web.converters.Converter
import org.codehaus.groovy.grails.web.converters.configuration.ChainedConverterConfiguration
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

    // The default adapter simply passes through the method invocations to the service.
    // RestfulServiceAdapter.
    private RestfulServiceAdapter defaultServiceAdapter =
        [ list:   { def service, Map params                      -> service.list(params) },
          count:  { def service, Map params                      -> service.count(params) },
          show:   { def service, Map params                      -> service.show(params) },
          create: { def service, Map content, Map params         -> service.create(content, params) },
          update: { def service, def id, Map content, Map params -> service.update(id, content, params) },
          delete: { def service, def id, Map content, Map params -> service.delete(id, content, params) }
        ] as RestfulServiceAdapter

    private ExtractorAdapter extractorAdapter = new DefaultExtractorAdapter()

    /**
     * Initializes the controller by registering the configured marshallers.
     **/
    // NOTE: The timing of PostConstruct works only when running 'test-app'
    //       -- it does *not* work for 'run-app' or 'test-app functional:'.
    //       'init()' is invoked explicitly from RestfulApiGrailsPlugin.
    // @PostConstruct
    void init() {

        log.trace 'Initializing RestfulApiController...'
        if (!(grailsApplication.config.restfulApiConfig instanceof Closure)) {
            throw new RuntimeException( "Missing restfulApiConfig" )
        }
        restConfig = RestConfig.parse( grailsApplication, grailsApplication.config.restfulApiConfig )
        restConfig.validate()

        restConfig.resources.values().each() { resource ->
            resource.representations.values().each() { representation ->
                switch(representation.mediaType) {
                    case ~/.*json$/:
                        JSON.createNamedConfig("restfulapi:" + resource.name + ":" + representation.mediaType) { json ->
                            log.trace "Creating named config: 'restfulapi:${resource.name}:${representation.mediaType}'"
                            representation.marshallers.each() {
                                log.trace "    ...registering json marshaller ${it.instance}"
                                json.registerObjectMarshaller(it.instance,it.priority)
                            }
                        }
                        ExtractorConfigurationHolder.registerExtractor(resource.name, representation.mediaType, representation.extractor )
                    break
                    case ~/.*xml$/:
                        XML.createNamedConfig("restfulapi:" + resource.name + ":" + representation.mediaType) { xml ->
                            representation.marshallers.each() {
                                log.trace "    ...registering xml marshaller ${it.instance}"
                                xml.registerObjectMarshaller(it.instance,it.priority)
                            }
                        }
                        ExtractorConfigurationHolder.registerExtractor(resource.name, representation.mediaType, representation.extractor )
                    break
                    default:
                        throw new RuntimeException("Cannot support media type ${representation.mediaType} in resource ${resource.name}.  All media types must end in xml or json.")
                }
            }
        }

        JSON.createNamedConfig('restapi-error:json') {
        }

        XML.createNamedConfig('restapi-error:xml') {
        }

        log.trace 'Done initializing RestfulApiController...'
    }


// ---------------------------------- ACTIONS ---------------------------------


    // GET /api/pluralizedResourceName
    //
    public def list() {

        log.trace "list invoked for ${params.pluralizedResourceName}"
        try {
            checkMethod( Methods.LIST )
            def responseRepresentation = getResponseRepresentation() // adds representation attribute to request

            def requestParams = params  // accessible from within withCacheHeaders
            def logger = log            // ditto

            def service = getService()
            def delegateToService = getServiceAdapter()
            logger.trace "... will delegate list() to service $service using adapter $delegateToService"

            def result = delegateToService.list(service, requestParams)
            logger.trace "... service returned $result"

            def count
            if (result instanceof grails.orm.PagedResultList) {
                count = result.totalCount
            } else {
                count  = delegateToService.count(service, requestParams)
            }

            // Need to create etagValue outside of 'etag' block:
            // http://jira.grails.org/browse/GPCACHEHEADERS-14
            String etagValue = shaFor( result, count, responseRepresentation.mediaType )

            withCacheHeaders {
                etag {
                    etagValue
                }
                delegate.lastModified {
                    lastModifiedFor( result )
                }
                generate {
                    ResponseHolder holder = new ResponseHolder()
                    holder.data = result
                    holder.addHeader('X-hedtech-totalCount', count)
                    holder.addHeader('X-hedtech-pageOffset', requestParams.offset ? requestParams?.offset : 0)
                    holder.addHeader('X-hedtech-pageMaxSize', requestParams.max ? requestParams?.max : result.size())
                    renderSuccessResponse( holder, 'default.rest.list.message' )
                }
            }
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
        try {
            checkMethod( Methods.SHOW )
            def responseRepresentation = getResponseRepresentation()

            def requestParams = params  // accessible from within withCacheHeaders
            def logger = log            // ditto

            def result = getServiceAdapter().show( getService(), requestParams )
            // Need to create etagValue outside of 'etag' block:
            // http://jira.grails.org/browse/GPCACHEHEADERS-14
            String etagValue = shaFor( result, responseRepresentation.mediaType )

            withCacheHeaders {

                etag {
                    etagValue
                }
                delegate.lastModified {
                    if (hasProperty( result, "lastUpdated" ))       result.lastUpdated
                    else if (hasProperty( result, "lastModified" )) result.lastModified
                    else                                            new Date()
                }
                generate {
                    renderSuccessResponse( new ResponseHolder( data: result ),
                                           'default.rest.shown.message' )
                }
            }
        }
        catch (e) {
            messageLog.error "Caught exception ${e.message}", e
            renderErrorResponse(e)
        }
    }


    // POST /api/pluralizedResourceName
    //
    public def create() {
        log.trace "create() invoked for ${params.pluralizedResourceName}"
        def result

        try {
            checkMethod( Methods.CREATE )
            def content = parseRequestContent( request )
            log.trace "Extracted content $content"
            getResponseRepresentation()
            result = getServiceAdapter().create( getService(), content, params )
            response.setStatus( 201 )
            renderSuccessResponse( new ResponseHolder( data: result ),
                                   'default.rest.created.message' )
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
            checkMethod( Methods.UPDATE )
            def content = parseRequestContent( request )
            if (content && content.id && content.id != params.id) {
                throw new IdMismatchException( params.pluralizedResourceName )
            }

            getResponseRepresentation()
            result = getServiceAdapter().update( getService(), params.id, content, params )
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
            checkMethod( Methods.DELETE )
            def content = [:]
            //Using angular in some browsers causes the Content-Type header
            //to be set as application/xml or some other default for zero-length
            //bodies, instead of a configured type.
            //If we have a delete with a zero-length body,
            //we will skip parsing the request content and use an
            //empty map.
            if (request.getContentLength() != 0) {
                content = parseRequestContent( request )
            }
            if (content && content.id && content.id != params.id) {
                throw new IdMismatchException( params.pluralizedResourceName )
            }
            getServiceAdapter().delete( getService(), params.id, content, params )
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
     * provided with a localized and singularized resource name will be automatically
     * added to the map.
     * @param responseMap the Map to render
     * @param msgResourceCode the resource code used to create a message entry
     **/
     protected void renderSuccessResponse(ResponseHolder holder, String msgResourceCode) {
        String localizedName = localize(Inflector.singularize(params.pluralizedResourceName))
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
                    /*def jsonObject
                    useJSON("restapi-error:json") {
                        //def s = (responseHolder.data as JSON) as String
                        //jsonObject = toJSONElement(s)
                        conent = responseHolder.data as JSON
                    }
                    useXML("restapi-error:xml") {
                        content = jsonObject as XML
                    }*/
                    useXML("restapi-error:xml") {
                        content = responseHolder.data as XML
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
            header.value.each() { val ->
                response.addHeader( header.key, val )
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
            def result = handler(params.pluralizedResourceName, e)
            if (result.headers) {
                result.headers.each() { header ->
                    if (header.value instanceof Collection) {
                        header.value.each { val ->
                            responseHolder.addHeader( header.key, val )
                        }
                    } else {
                        responseHolder.addHeader( header.key, header.value )
                    }
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
                log.trace "Going to useJSON with representation $representation"
                useJSON(representation) {
                    content = data as JSON
                }
                break
            case ~/.*xml$/:
            log.trace "Going to useXML with representation $representation"
                useXML(representation) {
                    content = data as XML
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
     *        Otherwise, use the media-type type specified by the Accept header.
     **/
    protected void renderResponse( ResponseHolder responseHolder ) {
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
            header.value.each() { val ->
                response.addHeader( header.key, val )
            }
        }
        if (responseHolder.message) {
            response.addHeader( "X-hedtech-message", responseHolder.message )
        }

        render(text: content ? content : "", contentType: contentType )
    }


    private String selectResponseContentType( RepresentationConfig representation ) {

        // Select the content type
        // Content type will always be application/json or application/xml
        // to make it easy for a response to be displayed in a browser or other tools
        // The X-hedtech-Media-Type header will hold the custom media type (if any)
        // describing the content in more detail.
        //
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


    protected boolean hasProperty( Object obj, String name ) {
        obj.getMetaClass().hasProperty(obj, "$name") && obj."$name"
    }


    protected String shaFor( resourceModel, String requestedMediaType ) {
        MessageDigest digest = MessageDigest.getInstance( 'SHA1' )
        shaFor( resourceModel, digest, requestedMediaType )
    }


    protected String shaFor( resourceModel, MessageDigest digest, String requestedMediaType ) {

        digest.update( requestedMediaType.getBytes( 'UTF-8' ) )

        if (resourceModel.getMetaClass().respondsTo( resourceModel, "getEtag" )) {
            log.trace "Will create ETag based upon a model's 'getEtag()' method"
            digest.update( "${resourceModel.getEtag()}".getBytes( 'UTF-8' ) )
            return "\"${new BigInteger( 1, digest.digest() ).toString( 16 ).padLeft( 40,'0' )}\""
        }

        if (!hasProperty( resourceModel, "id" )) {
            log.trace "Cannot create ETag using a resource's identity, returning a UUID"
            return randomUUID() as String
        }
        digest.update( "${resourceModel.id}".getBytes( 'UTF-8' ) )

        // we'll require either version, lastModified, or (worst case) all properties
        boolean changeIndictorFound = false
        if (hasProperty( resourceModel, "version") ) {
            changeIndictorFound = true
            digest.update( "${resourceModel.version}".getBytes( 'UTF-8' ) )
        }
        else if (hasProperty( resourceModel, "lastUpdated" )) {
            changeIndictorFound = true
            digest.update( "${resourceModel.lastUpdated}".getBytes( 'UTF-8' ) )
        }
        else if (hasProperty( resourceModel, "lastModified" )) {
            changeIndictorFound = true
            digest.update( "${resourceModel.lastModified}".getBytes( 'UTF-8' ) )
        }

        if (changeIndictorFound) {
            log.trace "Returning an ETag based on id and a known change indicator"
            return "\"${new BigInteger( 1, digest.digest() ).toString( 16 ).padLeft( 40,'0' )}\""
        } else {
            // Note: we don't return empty ETags as doing so may cause some caching
            //       infrastructure to reset connections.
            log.trace "Cannot create ETag using a resource's change indicator, returning a UUID"
            return randomUUID() as String
        }
    }


    protected String shaFor( Collection resourceModels, long totalCount, String requestedMediaType ) {

        if (!(resourceModels && totalCount)) return ''
        MessageDigest digest = MessageDigest.getInstance( 'SHA1' )

        // we'll use the collection size, the totalCount of resources,
        // and the sha1 calculated for each item in the collection
        digest.update( "${resourceModels.size()}".getBytes( 'UTF-8' ) )
        digest.update( "${totalCount}".getBytes( 'UTF-8' ) )
        resourceModels.each {
            shaFor( it, digest, requestedMediaType )
        }
        "\"${new BigInteger( 1, digest.digest() ).toString( 16 ).padLeft( 40,'0' )}\""
    }


    protected Date lastModifiedFor( Collection resourceModels ) {

        if (!resourceModels) return new Date()

        Date latestDate
        resourceModels.each {
            if (hasProperty( it, 'lastUpdated' )) {
                if (it.lastUpdated > latestDate) latestDate = it.lastUpdated
            }
            else if (hasProperty( it, 'lastModified' )) {
                if (it.lastModified > latestDate) latestDate = it.lastModified
            }
        }
        latestDate ?: new Date()
        latestDate
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
                return extractContent( representation.mediaType )
            break
            case ~/.*xml$/:
                return extractContent( representation.mediaType )
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
        } else if (e instanceof UnsupportedResourceException) {
            return 'UnsupportedResourceException'
        } else if (e instanceof UnsupportedRequestRepresentationException) {
            return 'UnsupportedRequestRepresentationException'
        } else if (e instanceof UnsupportedResponseRepresentationException) {
            return 'UnsupportedResponseRepresentationException'
        } else if (e instanceof IdMismatchException) {
            return 'IdMismatchException'
        } else if (e instanceof UnsupportedMethodException) {
            return 'UnsupportedMethodException'
        } else {
            return 'AnyOtherException'
        }
    }


    /**
     * Returns the name of the service to which this controller will delegate.
     * This implementation assumes the resource is a Grails 'domain'
     * object, and that the service name can be constructed by using the pluralized
     * 'resource' name found on the URL and appending 'Service'.
     * For example: If a URL of /api/pluralizedResourceName/id was invoked,
     * a service name of 'SingularizedResourceNameService' will be returned.
     **/
    protected String getServiceName() {
        def svcName = getResourceConfig()?.serviceName
        if (svcName == null) {
            svcName = "${domainName()}Service"
        }
        log.trace "getServiceName() will return $svcName"
        svcName
    }


    /**
     * Returns the transactional service corresponding to this resource.
     * The default implementation assumes the resource is a Grails 'domain'
     * object, and that the service can be identified by using the pluralized
     * 'resource' name found on the URL.
     * For example: If a URL of /api/pluralizedResourceName/id was invoked,
     * a service named 'SingularizedResourceNameService' will be retrieved
     * from the IoC container.
     * @see #getServiceName()
     **/
    protected def getService() {
        def svc
        try {
            svc = applicationContext.getBean(getServiceName())
        } catch (e) {
            log.error "Caught exception ${e.message}", e
            //throw e
        }
        log.trace "getService() will return service $svc"
        svc
    }


    /**
     * Returns an adapter supporting the service for which this
     * controller will delegate.
     * This implementation will look for the single adapter that has
     * been registered within the Spring container.
     * This adapter will be used when delegating to all services.
     * If no adapter is found in the Spring container, this
     * implementation will return a built-in pass-through adapter.
     **/
    protected RestfulServiceAdapter getServiceAdapter() {
        def adapterName = 'restfulServiceAdapter' // name of the single adapter
        log.trace "Looking for an adapter named $adapterName"
        RestfulServiceAdapter adapter
        try {
            adapter = applicationContext.getBean(adapterName)
        } catch (e) { // it is not an error if we cannot find an adapter
            log.trace "Did not find an adapter - ${e.message}"
        }

        log.trace "getServiceAdapter() will return adapter $adapter"
        adapter = adapter ?: defaultServiceAdapter
        adapter
    }

    protected ExtractorAdapter getExtractorAdapter() {
        extractorAdapter
    }


    /**
     * Returns the best match, or null if no supported representation for the resource exists.
     **/
    protected RepresentationConfig getRepresentation(pluralizedResourceName, allowedTypes) {
        return restConfig.getRepresentation( pluralizedResourceName, allowedTypes.name )
    }


    protected String selectResponseFormat( String format = null) {
        if (!format) {
            format = response.format
        }
        format == 'json' ? 'default' : format
    }

    protected void checkMethod( String method ) {
        def resource = getResourceConfig()
        if (!resource) {
            throw new UnsupportedResourceException( params.pluralizedResourceName )
        }
        if (!resource.allowsMethod( method ) ) {
            def allowed = resource.getMethods().intersect( Methods.getMethodGroup( method ) )
            throw new UnsupportedMethodException( supportedMethods:allowed )
        }
    }

    private String localize(String name) {
        message( code: "${name}.label", default: "$name" )
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
        ResourceConfig resource = getResourceConfig()
        useJSON( "restfulapi:" + resource.name + ":" + config.mediaType, closure )
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
        ResourceConfig resource = getResourceConfig()
        useXML( "restfulapi:" + resource.name + ":" + config.mediaType, closure )
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


    private unsupportedResponseRepresentation() {
        throw new UnsupportedResponseRepresentationException( params.pluralizedResourceName, request.getHeader(HttpHeaders.ACCEPT) )
    }


    private unsupportedRequestRepresentation() {
        throw new UnsupportedRequestRepresentationException( params.pluralizedResourceName, request.getHeader(HttpHeaders.CONTENT_TYPE ) )
    }


    private Map extractContent( String mediaType ) {
        ResourceConfig resourceConfig = getResourceConfig()
        Extractor extractor = ExtractorConfigurationHolder.getExtractor( resourceConfig.name, mediaType )
        if (!extractor) {
            unsupportedRequestRepresentation()
        }
        getExtractorAdapter().extract(extractor, request)
    }


    private Map extractXML( String mediaType ) {
        ResourceConfig resourceConfig = getResourceConfig()
        Extractor extractor = ExtractorConfigurationHolder.getExtractor( resourceConfig.name, mediaType )
        if (!extractor) {
            unsupportedRequestRepresentation()
        }
        return extractor.extract( request.XML )
    }

    private ResourceConfig getResourceConfig() {
        restConfig.getResource( params.pluralizedResourceName )
    }

    private def exceptionHandlers = [

        'ValidationException': { pluralizededResourceName, e->
            [
                httpStatusCode: 400,
                headers: ['X-Status-Reason':'Validation failed'],
                message: message( code: "default.rest.validation.errors.message",
                                          args: [ Inflector.singularize( pluralizededResourceName ) ] ) as String,
                returnMap: [
                    errors: [
                        [
                            type: "validation",
                            errorMessage: e.message
                        ]
                    ]
                ]
            ]
        },

        'OptimisticLockException': { pluralizededResourceName, e ->
            [
                httpStatusCode: 409,
                message: message( code: "default.optimistic.locking.failure",
                                          args: [ Inflector.singularize( pluralizededResourceName ) ] ) as String,
            ]
        },


        'UnsupportedResourceException': { pluralizededResourceName, e ->
            [
                httpStatusCode: 404,
                message: message( code: "default.rest.unknownresource.message",
                                          args: [ e.getPluralizedResourceName() ] ) as String,
            ]
        },


        'UnsupportedResponseRepresentationException': { pluralizededResourceName, e ->
            [
                httpStatusCode: 406,
                message: message( code: "default.rest.unknownrepresentation.message",
                                          args: [ e.getPluralizedResourceName(), e.getContentType() ] ) as String,
            ]
        },

        'UnsupportedRequestRepresentationException': { pluralizededResourceName, e ->
            [
                httpStatusCode: 415,
                message: message( code: "default.rest.unknownrepresentation.message",
                                          args: [ e.getPluralizedResourceName(), e.getContentType() ] ) as String,
            ]
        },

        'IdMismatchException': { pluralizededResourceName, e ->
            [
                httpStatusCode: 400,
                headers: ['X-Status-Reason':'Id mismatch'],
                message: message( code: "default.rest.idmismatch.message",
                                  args: [ e.getPluralizedResourceName() ] ) as String
            ]
        },

        'UnsupportedMethodException': { pluralizedResourceName, e ->
            def allowedHTTPMethods = []
            e.getSupportedMethods().each {
                allowedHTTPMethods.add( Methods.getHttpMethod( it ) )
            }
            def r = [
                httpStatusCode: 405,
                headers: ['Allow':allowedHTTPMethods],
                message: message( code: 'default.rest.method.not.allowed.message' ) as String
            ]
        },

        'ApplicationException': { pluralizededResourceName, e ->
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
        'AnyOtherException': { pluralizededResourceName, e ->
            [
                httpStatusCode: 500,
                message: message( code: "default.rest.general.errors.message",
                                  args: [ pluralizededResourceName ] ) as String,
                returnMap: [
                    errors: [ [
                        type: "general",
                        errorMessage: e.message
                        ]
                    ]
                ]
            ]
        }
    ]

}
