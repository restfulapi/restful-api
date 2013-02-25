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
        1 * mock."$serviceMethod"(_) >> { return serviceReturn }
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

        where:
        controllerMethod | httpMethod | id   | serviceMethod | serviceReturn
        'list'           | 'GET'      | null | 'list'        | [totalCount:0,instances:['foo']]
        'show'           | 'GET'      | '1'  | 'show'        | [instance:'foo']
        'save'           | 'POST'     | null | 'create'      | [instance:'foo']
        'update'         | 'PUT'      | '1'  | 'update'      | [instance:'foo']
    }

}