/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import grails.test.mixin.*
import spock.lang.*

import net.hedtech.restfulapi.config.*
import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*
import net.hedtech.restfulapi.extractors.xml.*

@TestFor(RestfulApiController)
class RestfulApiControllerSpec extends Specification {

    def setup() {
        JSONExtractorConfigurationHolder.clear()
    }

    def cleanup() {
        JSONExtractorConfigurationHolder.clear()
    }

    @Unroll
    def "Test unsupported media type in Accept header returns 406"(String controllerMethod, String httpMethod, String id, String serviceMethod, def serviceReturn ) {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        //controller.restConfig = RESTConfig.parse( null, restfulApiConfig )
        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/xml' )
        //incoming format always json, so no errors
        request.addHeader( 'Content-Type', 'application/json' )
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
        config.restfulApiConfig = {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    extractor = new DefaultJSONExtractor()
                }
            }
        }

        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/xml' )
        //incoming format always json, so no errors
        request.addHeader( 'Content-Type', 'application/json' )
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
    def "Unsupported media type in Content-Type header returns 415"(String controllerMethod, String httpMethod, String id, String serviceMethod, def serviceReturn ) {
        setup:
         config.restfulApiConfig = {
            resource {
                name = 'things'
            }
        }
        controller.init()

        //mock the appropriate service method, but expect no method calls
        //(since the request cannot be understood, the service should not be contacted)
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader('Accept','application/json')
        request.addHeader('Content-Type','application/json')
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
    def "Media type in Content-Type header without extractor returns 415"(String controllerMethod, String httpMethod, String mediaType, String id, String serviceMethod, def serviceReturn, def body ) {
        setup:
        config.restfulApiConfig = getConfigMissingExtractorsAndMarshallers()
        controller.init()

        //mock the appropriate service method, but expect no method calls
        //(since the request cannot be understood, the service should not be contacted)
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', mediaType  )
        //incoming format not a registered media type, so
        //simulate fallback to html
        request.method = httpMethod
        if (body != null) request.setContent( body.getBytes('UTF-8' ) )
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
        controllerMethod | httpMethod | mediaType                      | id   | serviceMethod | serviceReturn    | body
        'save'           | 'POST'     | 'application/json'             | null | 'create'      | [instance:'foo'] | null
        'update'         | 'PUT'      | 'application/json'             | '1'  | 'update'      | [instance:'foo'] | null
        'delete'         | 'DELETE'   | 'application/json'             | '1'  | 'delete'      | null             | null
        'save'           | 'POST'     | 'application/xml'              | null | 'create'      | [instance:'foo'] | """<?xml version="1.0" encoding="UTF-8"?><json><code>AC</code><description>An AC thingy</description></json>"""
        'update'         | 'PUT'      | 'application/xml'              | '1'  | 'update'      | [instance:'foo'] | """<?xml version="1.0" encoding="UTF-8"?><json><code>AC</code><description>An AC thingy</description></json>"""
        'delete'         | 'DELETE'   | 'application/xml'              | '1'  | 'delete'      | null             | """<?xml version="1.0" encoding="UTF-8"?><json><code>AC</code><description>An AC thingy</description></json>"""
        'save'           | 'POST'     | 'application/custom-xml'       | null | 'create'      | [instance:'foo'] | null
        'update'         | 'PUT'      | 'application/custom-xml'       | '1'  | 'update'      | [instance:'foo'] | null
        'delete'         | 'DELETE'   | 'application/custom-xml'       | '1'  | 'delete'      | null             | null
        'save'           | 'POST'     | 'application/custom-thing-xml' | null | 'create'      | [instance:'foo'] | null
        'update'         | 'PUT'      | 'application/custom-thing-xml' | '1'  | 'update'      | [instance:'foo'] | null
        'delete'         | 'DELETE'   | 'application/custom-thing-xml' | '1'  | 'delete'      | null             | null
    }

    //test list and show edge cases. since there is no request body accepted, ignore content-type header?
    @Unroll
    def "Media type without extractor in Content-Type header for list and show is ignored"(String controllerMethod, String httpMethod, String id, String serviceMethod, def serviceReturn ) {
        setup:
        config.restfulApiConfig = {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                }
            }
        }
        controller.init()
        //mock the appropriate service method, but expect no method calls
        //(since the request cannot be understood, the service should not be contacted)
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader('Accept','application/json')
        request.addHeader('Content-Type','application/json')
        //incoming format not a registered media type, so
        //simulate fallback to html
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

    /**
     * Work around for a weird bug where defining the config closure
     * in the test setup doesn't work.
     **/
    private def getConfigMissingExtractorsAndMarshallers() {
        return {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                }
                representation {
                    mediaType = 'application/xml'
                    jsonAsXml = true
                    extractor = new net.hedtech.restfulapi.extractors.xml.JSONObjectExtractor()
                }
                representation {
                    mediaType = 'application/custom-xml'
                }
                representation {
                    mediaType = 'application/custom-thing-xml'
                    jsonAsXml = true
                }
            }
        }
    }


}