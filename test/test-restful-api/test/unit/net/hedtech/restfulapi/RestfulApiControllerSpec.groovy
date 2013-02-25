/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import grails.test.mixin.*
import spock.lang.*

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*

@TestFor(RestfulApiController)
class RestfulApiControllerSpec extends Specification {

    def setup() {
        JSONExtractorConfigurationHolder.clear()
    }

    def cleanup() {
        JSONExtractorConfigurationHolder.clear()
    }

    @Unroll
    def "Unmapped media type in Accept header returns 406"(String controllerMethod, String httpMethod, String id, String serviceMethod, def serviceReturn ) {
        setup:
        //use default extractor for any methods with a request body
        JSONExtractorConfigurationHolder.registerExtractor( "things", "json", new DefaultJSONExtractor() )

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        //simulate unrecognized media type so content negotiation falls back to html
        response.format = 'html'
        //incoming format always json, so no errors
        request.format = 'json'
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        406 == response.status
          0 == response.getContentLength()
        1 * mock."$serviceMethod"(_) >> { return serviceReturn }

        where:
        controllerMethod | httpMethod | id   | serviceMethod | serviceReturn
        'list'           | 'GET'      | null | 'list'        | [totalCount:0,instances:['foo']]
        'show'           | 'GET'      | '1'  | 'show'        | [instance:'foo']
        'save'           | 'POST'     | null | 'create'      | [instance:'foo']
        'update'         | 'PUT'      | '1'  | 'update'      | [instance:'foo']
    }

    def "Test delete with unsupported Accept header works, as there is no content returned"() {
        setup:
        //use default extractor for any methods with a request body
        JSONExtractorConfigurationHolder.registerExtractor( "things", "json", new DefaultJSONExtractor() )

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        //simulate unrecognized media type so content negotiation falls back to html
        response.format = 'html'
        request.format = 'json'
        request.method = 'DELETE'
        params.pluralizedResourceName = 'things'
        params.id = '1'

        when:
        controller.delete()

        then:
        200 == response.status
          0 == response.getContentLength()
          1 * mock.delete(_) >> {}

    }

    @Unroll
    def "Unmapped media type in Content-Type header returns 415"(String controllerMethod, String httpMethod, String id, String serviceMethod, def serviceReturn ) {
        setup:
        //mock the appropriate service method, but expect no method calls
        //(since the request cannot be understood, the service should not be contacted)
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        response.format = 'json'
        //incoming format not a registered media type, so
        //simulate fallback to html
        request.format = 'html'
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        415 == response.status
          0 == response.getContentLength()
        0 * _._

        where:
        controllerMethod | httpMethod | id   | serviceMethod | serviceReturn
        'save'           | 'POST'     | null | 'create'      | [instance:'foo']
        'update'         | 'PUT'      | '1'  | 'update'      | [instance:'foo']
        'delete'         | 'DELETE'   | '1'  | 'delete'      | null
    }

    @Unroll
    def " Media type in Content-Type header without extractor returns 415"(String controllerMethod, String httpMethod, String format, String id, String serviceMethod, def serviceReturn ) {
        setup:
        //mock the appropriate service method, but expect no method calls
        //(since the request cannot be understood, the service should not be contacted)
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        response.format = 'json'
        //incoming format not a registered media type, so
        //simulate fallback to html
        request.format = format
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        415 == response.status
          0 == response.getContentLength()
        0 * _._

        where:
        //test data for the current 3 'buckets' an incoming request falls into:
        //json content, json-as-xml content (xml), and custom xml (any format not)
        //starting with 'xml'
        controllerMethod | httpMethod | format        | id   | serviceMethod | serviceReturn
        'save'           | 'POST'     | 'json'        | null | 'create'      | [instance:'foo']
        'update'         | 'PUT'      | 'json'        | '1'  | 'update'      | [instance:'foo']
        'delete'         | 'DELETE'   | 'json'        | '1'  | 'delete'      | null
        'save'           | 'POST'     | 'xml'         | null | 'create'      | [instance:'foo']
        'update'         | 'PUT'      | 'xml'         | '1'  | 'update'      | [instance:'foo']
        'delete'         | 'DELETE'   | 'xml'         | '1'  | 'delete'      | null
        'save'           | 'POST'     | 'custom-xml'  | null | 'create'      | [instance:'foo']
        'update'         | 'PUT'      | 'custom-xml'  | '1'  | 'update'      | [instance:'foo']
        'delete'         | 'DELETE'   | 'cusotm-xml'  | '1'  | 'delete'      | null
    }

    //test list and show edge cases. since there is no request body accepted, ignore content-type header?
    @Unroll
    def "Unmapped media type in Content-Type header for list and show is ignored"(String controllerMethod, String httpMethod, String id, String serviceMethod, def serviceReturn ) {
        setup:
        //mock the appropriate service method, but expect no method calls
        //(since the request cannot be understood, the service should not be contacted)
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        response.format = 'json'
        //incoming format not a registered media type, so
        //simulate fallback to html
        request.format = 'html'
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        200 == response.status
          0 < response.text.length()
        1 * mock."$serviceMethod"(_) >> { return serviceReturn }

        where:
        controllerMethod | httpMethod | id   | serviceMethod | serviceReturn
        'list'           | 'GET'      | null | 'list'        | [totalCount:0,instances:['foo']]
        'show'           | 'GET'      | '1'  | 'show'        | [instance:[foo:'foo']]
    }



}