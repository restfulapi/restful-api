/* ****************************************************************************
 * Copyright 2013-2019 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
package net.hedtech.restfulapi

import com.grailsrocks.cacheheaders.CacheHeadersService

import grails.converters.JSON
import grails.converters.XML
import grails.test.mixin.*

import net.hedtech.restfulapi.apiversioning.BasicApiVersionParser
import net.hedtech.restfulapi.config.*
import net.hedtech.restfulapi.extractors.*
import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*
import net.hedtech.restfulapi.extractors.xml.*
import net.hedtech.restfulapi.marshallers.*

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
import org.codehaus.groovy.grails.web.json.JSONObject

import spock.lang.*


@TestFor(RestfulApiController)
class RestfulApiControllerSpec extends Specification {

    def setup() {
        ExtractorConfigurationHolder.clear()
    }

    def cleanup() {
        ExtractorConfigurationHolder.clear()
    }

    @Unroll
    def "Test unsupported media type in Accept header returns 406"(String controllerMethod, String httpMethod, String id, String serviceMethod, def serviceReturn ) {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect 0 invocations
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
          0 * _._

        where:
        controllerMethod | httpMethod | id   | serviceMethod | serviceReturn
        'list'           | 'GET'      | null | 'list'        | ['foo']
        'show'           | 'GET'      | '1'  | 'show'        | 'foo'
        'create'         | 'POST'     | null | 'create'      | 'foo'
        'update'         | 'PUT'      | '1'  | 'update'      | 'foo'
    }

    @Unroll
    def "Test media type without marshaller framework returns 406"(String controllerMethod, String httpMethod, String id, String serviceMethod, def serviceReturn ) {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallerFramework = 'none' //no marshalling support for this representation
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect 0 invocations
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/json' )
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
          0 * _._

        where:
        controllerMethod | httpMethod | id   | serviceMethod | serviceReturn
        'list'           | 'GET'      | null | 'list'        | ['foo']
        'show'           | 'GET'      | '1'  | 'show'        | 'foo'
        'create'         | 'POST'     | null | 'create'      | 'foo'
        'update'         | 'PUT'      | '1'  | 'update'      | 'foo'
    }

    def "Test global service adapter is returned if configured"() {
        setup:
        defineBeans {
            restfulServiceAdapter(DummyServiceAdapter)
        }
        controller.init()
        //mock that the resource does not override the adapter
        controller.metaClass.getServiceAdapterName = {->null}

        when:
        def adapter = controller.getServiceAdapter()

        then:
        DummyServiceAdapter.class == adapter.class
    }


    def "Test resource-specific service adapter overrides global adapter"() {
        setup:
        defineBeans {
            restfulServiceAdapter(DummyServiceAdapter) {
                name = 'default'
            }
            foo(DummyServiceAdapter) {
                name = 'foo'
            }
        }
        controller.init()
        //mock that the resource does not override the adapter
        controller.metaClass.getServiceAdapterName = {->'foo'}

        when:
        def adapter = controller.getServiceAdapter()

        then:
        'foo' == adapter.name
    }

    def "Test delete with unsupported Accept header works, as there is no content returned"() {
        setup:
        //use default extractor for any methods with a request body
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
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
          1 * mock.delete(_,_) >> {}
    }

    @Unroll
    def "Unsupported media type in Content-Type header returns 415"(String controllerMethod, String httpMethod, String id, String serviceMethod, def serviceReturn ) {
        setup:
         config.restfulApiConfig = {
            resource 'things' config {
                bodyExtractedOnDelete = true
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
        //make sure the body isn't zero length
        //so that the controller has to look at the
        //content-type for delete
        request.setContent( new byte[1] )
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
        'create'         | 'POST'     | null | 'create'      | 'foo'
        'update'         | 'PUT'      | '1'  | 'update'      | 'foo'
        'delete'         | 'DELETE'   | '1'  | 'delete'      | null
    }

    @Unroll
    def "Media type in Content-Type header without extractor returns 415"(String controllerMethod, String httpMethod, String mediaType, String id, def serviceReturn, def body ) {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                bodyExtractedOnDelete = true
                representation {
                    mediaTypes = ['application/json']
                }
                representation {
                    mediaTypes = ['application/xml']
                }
            }
        }
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
        //test data for the current 2 'buckets' an incoming request falls into:
        //json content or xml content
        controllerMethod | httpMethod | mediaType                      | id   | serviceReturn    | body
        'create'         | 'POST'     | 'application/json'             | null | 'foo'            | null
        'update'         | 'PUT'      | 'application/json'             | '1'  | 'foo'            | null
        'delete'         | 'DELETE'   | 'application/json'             | '1'  | null             | null
        'create'         | 'POST'     | 'application/xml'              | null | 'foo'            | """<?xml version="1.0" encoding="UTF-8"?><json><code>AC</code><description>An AC thingy</description></json>"""
        'update'         | 'PUT'      | 'application/xml'              | '1'  | 'foo'            | """<?xml version="1.0" encoding="UTF-8"?><json><code>AC</code><description>An AC thingy</description></json>"""
        'delete'         | 'DELETE'   | 'application/xml'              | '1'  | null             | """<?xml version="1.0" encoding="UTF-8"?><json><code>AC</code><description>An AC thingy</description></json>"""
    }

    @Unroll
    def "Media type without extractor in Content-Type header for list and show is ignored"(String controllerMethod, String httpMethod, String id, String serviceMethod, def serviceReturn ) {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                }
            }
        }
        controller.init()
        //mock the appropriate service method, but expect no method calls
        //(since the request cannot be understood, the service should not be contacted)
        def mock = Mock(ThingService)
        controller.metaClass.getService = { -> mock }

        // Since both our show and list methods use a closure from the cache-headers
        // plugin, we need to mock that closure
        def cacheHeadersService = new CacheHeadersService()
        Closure withCacheHeadersClosure = { Closure c ->
            c.delegate = controller
            c.resolveStrategy = Closure.DELEGATE_ONLY
            cacheHeadersService.withCacheHeaders( c.delegate, c )
        }
        controller.metaClass.withCacheHeaders = withCacheHeadersClosure

        request.addHeader('Accept','application/json')
        request.addHeader('Content-Type','application/json')
        //incoming format not a registered media type, so
        //simulate fallback to html
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id
        if (serviceMethod == 'list') {
            1 * mock.count(_) >> {return 5}
        }

        when:
        controller."$controllerMethod"()

        then:
        200 == response.status
          0 < response.text.length()
        1 * mock."$serviceMethod"(_) >> { return serviceReturn }

        where:
        controllerMethod | httpMethod | id   | serviceMethod | serviceReturn
        'list'           | 'GET'      | null | 'list'        | ['foo']
        'show'           | 'GET'      | '1'  | 'show'        | [foo:'foo']
    }

    def "Test that mismatch between id in url and resource representation returns 400"(def controllerMethod, def httpMethod, def id, def body) {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {
                bodyExtractedOnDelete = true
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/json' )
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        params.id = id
        request.setContent( body.getBytes('UTF-8' ) )

        when:
        controller."$controllerMethod"()

        then:
        400 == response.status
          0 == response.getContentLength()
          0 * _._
        'Id mismatch' == response.getHeaderValue( 'X-Status-Reason' )
        'default.rest.idmismatch.message' == response.getHeaderValue( 'X-hedtech-message' )

        where:
        controllerMethod | httpMethod | id   | body
        'update'         | 'PUT'      | '1'  | '{id:"2"}'
        'delete'         | 'DELETE'   | '1'  | '{id:"2"}'
    }

    def "Test that mismatch between type for id in url and resource representation does not cause failure"(def controllerMethod, def httpMethod, def id, def body, def serviceMethod) {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {

                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/json' )
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        params.id = id
        request.setContent( body.getBytes('UTF-8' ) )

        when:
        controller."$controllerMethod"()

        then:
        200 == response.status
        if (serviceMethod == 'update') {
            1 * mock."$serviceMethod"(_,_) >> { [:] }
        } else {
            1 * mock."$serviceMethod"(_,_) >> { }
        }


        where:
        controllerMethod | httpMethod | id   | body       | serviceMethod
        'update'         | 'PUT'      | '1'  | '{id:1}'   | 'update'
        'delete'         | 'DELETE'   | '1'  | '{id:1}'   | 'delete'
        'update'         | 'PUT'      | '1'  | '{id:"1"}' | 'update'
        'delete'         | 'DELETE'   | '1'  | '{id:"1"}' | 'delete'
    }

    def "Test overriding enforcement of content id matching"(def controllerMethod, def httpMethod, def id, def body, def serviceMethod) {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {
                idMatchEnforced = false
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/json' )
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        params.id = id
        request.setContent( body.getBytes('UTF-8' ) )

        when:
        controller."$controllerMethod"()

        then:
        200 == response.status
        if (serviceMethod == 'update') {
            1 * mock."$serviceMethod"(_,_) >> { [:] }
        } else {
            1 * mock."$serviceMethod"(_,_) >> { }
        }


        where:
        controllerMethod | httpMethod | id   | body       | serviceMethod
        'update'         | 'PUT'      | '1'  | '{id:2}'   | 'update'
        'delete'         | 'DELETE'   | '1'  | '{id:2}'   | 'delete'
        'update'         | 'PUT'      | '1'  | '{id:"2"}' | 'update'
        'delete'         | 'DELETE'   | '1'  | '{id:"2"}' | 'delete'
    }


    def "Test that service name can be overridden in configuration"() {
      setup:
         config.restfulApiConfig = {
            resource 'things' config {
                serviceName = 'theThingService'
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()
        params.pluralizedResourceName = 'things'

        when:
        def serviceName = controller.getServiceName()

        then:
        'theThingService' == serviceName
    }

    def "Test that service adapter name can be overridden in configuration"() {
      setup:
         config.restfulApiConfig = {
            resource 'things' config {
                serviceAdapterName = 'thingServiceAdapter'
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()
        params.pluralizedResourceName = 'things'

        when:
        def serviceAdapterName = controller.getServiceAdapterName()

        then:
        'thingServiceAdapter' == serviceAdapterName
    }

    @Unroll
    def "Unsupported method returns 405"(String controllerMethod, def allowedMethods, def allowHeader ) {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                methods = allowedMethods
                representation {
                    mediaTypes = ['application/json']
                }
            }
        }
        controller.init()

        //mock the appropriate service method, but expect no method calls
        //(since the request cannot be understood, the service should not be contacted)
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/json' )
        params.pluralizedResourceName = 'things'

        when:
        controller."$controllerMethod"()

        then:
        405          == response.status
          0          == response.getContentLength()
        allowHeader.size()  == response.headers( 'Allow' ).size()
        allowHeader as Set == response.headers( 'Allow') as Set
        0 * _._

        where:
        controllerMethod | allowedMethods                       | allowHeader
        'list'           | ['show','update','delete']           | []
        'list'           | ['create','show','update','delete']  | ["POST"]
        'list'           | []                                   | []
        'list'           | ['create']                           | ["POST"]
        'create'         | ['show','update','delete']           | []
        'create'         | ['list','show','update','delete']    | ["GET"]
        'create'         | []                                   | []
        'create'         | ['list']                             | ["GET"]
        'show'           | []                                   | []
        'show'           | ['update','delete']                  | ["PUT","DELETE"]
        'show'           | ['update','list']                    | ["PUT"]
        'update'         | []                                   | []
        'update'         | ['delete','show']                    | ["DELETE","GET"]
        'update'         | ['show','list','create']             | ["GET"]
        'delete'         | []                                   | []
        'delete'         | ['show','update']                    | ["GET","PUT"]
        'delete'         | ['update','list','create']           | ["PUT"]
        'delete'         | ['update','show', 'list','create']   | ["GET","PUT"]
    }

    def "Test optimistic lock returns 409"() {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}


        request.addHeader( 'Accept', 'application/json' )
        //incoming format always json, so no errors
        request.addHeader( 'Content-Type', 'application/json' )
        params.pluralizedResourceName = 'things'

        when:
        controller.update()

        then:
        1*mock.update(_,_) >> { throw new org.springframework.dao.OptimisticLockingFailureException( "foo" ) }
        409 == response.status
          0 == response.getContentLength()
        'default.optimistic.locking.failure' == response.getHeaderValue( 'X-hedtech-message' )
    }

    def "Test generic error returns 500"() {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}


        request.addHeader( 'Accept', 'application/json' )
        //incoming format always json, so no errors
        request.addHeader( 'Content-Type', 'application/json' )
        params.pluralizedResourceName = 'things'

        when:
        controller.update()

        then:
        1*mock.update(_,_) >> { throw new Exception( 'foo' ) }
        500 == response.status
          0 == response.getContentLength()
        'default.rest.general.errors.message' == response.getHeaderValue( 'X-hedtech-message' )
    }

    def "Test checked application exception"() {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}


        request.addHeader( 'Accept', 'application/json' )
        //incoming format always json, so no errors
        request.addHeader( 'Content-Type', 'application/json' )
        params.pluralizedResourceName = 'things'

        when:
        controller.list()

        then:
        1*mock.list(_) >> { throw new CheckedApplicationException( 400, 'my message' ) }
        400 == response.status
          0 == response.getContentLength()
        'my message' == response.getHeaderValue( 'X-hedtech-message' )
    }

    def "Test adding exception handler"() {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
            exceptionHandlers {
                handler {
                    instance = new CheckedApplicationExceptionHandler()
                    priority = 1
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/json' )
        //incoming format always json, so no errors
        request.addHeader( 'Content-Type', 'application/json' )
        params.pluralizedResourceName = 'things'

        when:
        controller.list()

        then:
        1*mock.list(_) >> { throw new CheckedApplicationException( 400, 'my message' ) }
        //our handler should take priority
        403 == response.status
          0 == response.getContentLength()
        'dummy message' == response.getHeaderValue( 'X-hedtech-message' )
    }

    def "Test exception handlers override controller handlers"() {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
            exceptionHandlers {
                handler {
                    instance = new DefaultExceptionHandler()
                    priority = Integer.MIN_VALUE
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/json' )
        //incoming format always json, so no errors
        request.addHeader( 'Content-Type', 'application/json' )
        params.pluralizedResourceName = 'things'

        when:
        controller.list()

        then:
        1*mock.list(_) >> { throw new Exception() }
        //our handler should take priority, as it registers last
        403 == response.status
          0 == response.getContentLength()
        'dummy message' == response.getHeaderValue( 'X-hedtech-message' )
    }

    def "Test that delete with bodyExtractedOnDelete false ignores body and Content-Type"() {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {
                bodyExtractedOnDelete = false
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.setContent( '{"foo":"bar"}'.getBytes() )
        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/xml' )
        params.pluralizedResourceName = 'things'

        when:
        controller.delete()

        then:
        200 == response.status
          0 == response.getContentLength()
          1*mock.delete([:],_) >> { }
        'default.rest.deleted.message' == response.getHeaderValue( 'X-hedtech-message' )
    }

    @Unroll
    def "Test anyResource support"() {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            anyResource {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonBeanMarshaller {}
                    }
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}
        mockCacheHeaders()

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/json' )
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        status == response.status
        listCount * mock.list(_) >> { serviceReturn }
        listCount * mock.count(_) >> { serviceReturn.size() }
        showCount * mock.show(_) >> { serviceReturn }
        updateCount * mock.update(_,_) >> { serviceReturn }
        createCount * mock.create(_,_) >> { serviceReturn }
        deleteCount * mock.delete(_,_) >> {}
        0 * _._

        where:
        controllerMethod | httpMethod | id   | status | listCount | showCount | updateCount | createCount | deleteCount | serviceReturn
        'list'           | 'GET'      | null | 200    | 1         | 0         | 0           | 0           | 0           | ['foo']
        'show'           | 'GET'      | '1'  | 200    | 0         | 1         | 0           | 0           | 0           | [name:'foo']
        'create'         | 'POST'     | null | 201    | 0         | 0         | 0           | 1           | 0           | [name:'foo']
        'update'         | 'PUT'      | '1'  | 200    | 0         | 0         | 1           | 0           | 0           | [name:'foo']
        'delete'         | 'DELETE'   | '1'  | 200    | 0         | 0         | 0           | 0           | 1           | null
    }

    @Unroll
    def "Test support for 'request ID'"() {
        setup:
         if (headerName) config.restfulApi.header.requestId = "$headerName" as String
         config.restfulApiConfig = {
            anyResource {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonBeanMarshaller {}
                    }
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}
        mockCacheHeaders()

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/json' )
        if (headerName) request.addHeader( headerName, requestId )
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller.setRequestIdAttribute()
        controller."$controllerMethod"()
        def headerNameUsed = headerName ?: "X-Request-ID"

        then:
        status == response.status
        mock.list(_) >> { serviceReturn }
        mock.count(_) >> { serviceReturn.size() }
        null != response.getHeader(headerNameUsed)
        requestId == response.getHeader(headerNameUsed) || (requestId.size() == 0 && response.getHeader(headerNameUsed)?.size() == 36)

        where:
        controllerMethod | httpMethod | id   | status | serviceReturn | headerName         | requestId
        'list'           | 'GET'      | null | 200    | ['foo']       | ''                 | ''
        'list'           | 'GET'      | null | 200    | ['foo']       | 'X-LISTRequest-ID' | '123-LIST-789'
        'show'           | 'GET'      | '1'  | 200    | [name:'foo']  | 'X-Request-ID'     | '123-SHOW-9'
        'create'         | 'POST'     | null | 201    | [name:'foo']  | 'X-Request-ID'     | '123-CREATE-789'
        'update'         | 'PUT'      | '1'  | 200    | [name:'foo']  | 'X-Request-ID'     | '123-update-789'
        'delete'         | 'DELETE'   | '1'  | 200    | null          | 'U-Request-ID'     | 'My-DELETE-ID'
    }


    @Unroll
    def "Test delegation to JSONExtractor"() {
        setup:
        def theExtractor = Mock(JSONExtractor)
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            anyResource {
                bodyExtractedOnDelete = true
                representation {
                    mediaTypes = ['application/json']
                    extractor = theExtractor
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}
        mockCacheHeaders()

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/json' )
        request.method = 'POST'
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        1 * theExtractor.extract(_) >> { ['foo':'bar']}
        createCount * mock.create(_,_)
        updateCount * mock.update(_,_)
        deleteCount * mock.delete(_,_)

        where:
        id   | controllerMethod | createCount | updateCount | deleteCount
        null | 'create'         | 1           | 0           | 0
        null | 'update'         | 0           | 1           | 0
        null | 'delete'         | 0           | 0           | 1
    }

    @Unroll
    def "Test delegation to XMLExtractor"() {
        setup:
        def theExtractor = Mock(XMLExtractor)
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            anyResource {
                bodyExtractedOnDelete = true
                representation {
                    mediaTypes = ['application/xml']
                    extractor = theExtractor
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}
        mockCacheHeaders()

        request.addHeader( 'Accept', 'application/xml' )
        request.addHeader( 'Content-Type', 'application/xml' )
        request.method = 'POST'
        request.content = '<?xml version="1.0" encoding="UTF-8"?><thing/>'
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        1 * theExtractor.extract(_) >> { ['foo':'bar']}
        createCount * mock.create(_,_)
        updateCount * mock.update(_,_)
        deleteCount * mock.delete(_,_)

        where:
        id   | controllerMethod | createCount | updateCount | deleteCount
        null | 'create'         | 1           | 0           | 0
        null | 'update'         | 0           | 1           | 0
        null | 'delete'         | 0           | 0           | 1
    }

    @Unroll
    def "Test delegation to RequestExtractor"() {
        setup:
        def theExtractor = Mock(RequestExtractor)
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            anyResource {
                bodyExtractedOnDelete = true
                representation {
                    mediaTypes = ['application/json']
                    extractor = theExtractor
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}
        mockCacheHeaders()

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/json' )
        request.method = 'POST'
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        1 * theExtractor.extract(_) >> { ['foo':'bar']}
        createCount * mock.create(_,_)
        updateCount * mock.update(_,_)
        deleteCount * mock.delete(_,_)

        where:
        id   | controllerMethod | createCount | updateCount | deleteCount
        null | 'create'         | 1           | 0           | 0
        1    | 'update'         | 0           | 1           | 0
        1    | 'delete'         | 0           | 0           | 1
    }

    @Unroll
    def "Test custom marshaller that returns string"() {
        setup:
        def marshallerService = Mock(MarshallingService)
        config.restfulApiConfig = {
            anyResource {
                representation {
                    mediaTypes = ['application/json']
                    marshallerFramework = 'custom'
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {'string'}
        mock.create(_,_) >> {'string'}
        mock.update(_,_) >> {'string'}
        mock.delete(_,_) >> {}
        controller.metaClass.getService = {-> mock}
        controller.metaClass.getMarshallingService = {String name -> marshallerService}
        mockCacheHeaders()

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', 'application/json')
        request.addHeader('Content-Type', 'application/json')
        if (id != null) params.id = id
        int expectedCount = controllerMethod == 'delete' ? 0 : 1

        when:
        controller."$controllerMethod"()

        then:
        expectedCount * marshallerService.marshalObject(_,_ as RepresentationConfig) >> {object, config -> return value}
        value == response.getText()

        where:
        id   | controllerMethod | value
        null | 'list'           | 'custom1, custom2'
        1    | 'show'           | 'custom1'
        null | 'create'         | 'custom1'
        1    | 'update'         | 'custom1'
        1    | 'delete'         | ""
    }

    @Unroll
    def "Test custom marshaller that returns byte[]"() {
        setup:
        def marshallerService = Mock(MarshallingService)
        config.restfulApiConfig = {
            anyResource {
                representation {
                    mediaTypes = ['application/json']
                    marshallerFramework = 'custom'
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {'string'}
        mock.create(_,_) >> {'string'}
        mock.update(_,_) >> {'string'}
        mock.delete(_,_) >> {}
        controller.metaClass.getService = {-> mock}
        controller.metaClass.getMarshallingService = {String name -> marshallerService}
        mockCacheHeaders()

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', 'application/json')
        request.addHeader('Content-Type', 'application/json')
        if (id != null) params.id = id
        int expectedCount = controllerMethod == 'delete' ? 0 : 1
        boolean expectLength  = controllerMethod != 'delete'
        byte[] bytes = value.getBytes('UTF-8')

        when:
        controller."$controllerMethod"()

        then:
        expectedCount * marshallerService.marshalObject(_,_ as RepresentationConfig) >> {object, config -> return bytes}
        bytes        == response.getContentAsByteArray()
        bytes.length == response.getContentLength()
        expectLength == response.containsHeader('Content-Length')

        where:
        id   | controllerMethod | value
        null | 'list'           | 'custom1, custom2'
        1    | 'show'           | 'custom1'
        null | 'create'         | 'custom1'
        1    | 'update'         | 'custom1'
        1    | 'delete'         | ""
    }

    @Unroll
    def "Test custom marshaller that returns InputStream"() {
        setup:
        def marshallerService = Mock(MarshallingService)
        config.restfulApiConfig = {
            anyResource {
                representation {
                    mediaTypes = ['application/json']
                    marshallerFramework = 'custom'
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {'string'}
        mock.create(_,_) >> {'string'}
        mock.update(_,_) >> {'string'}
        mock.delete(_,_) >> {}
        controller.metaClass.getService = {-> mock}
        controller.metaClass.getMarshallingService = {String name -> marshallerService}
        mockCacheHeaders()

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', 'application/json')
        request.addHeader('Content-Type', 'application/json')
        if (id != null) params.id = id
        int expectedCount = controllerMethod == 'delete' ? 0 : 1
        byte[] bytes = value.getBytes('UTF-8')

        when:
        controller."$controllerMethod"()

        then:
        expectedCount * marshallerService.marshalObject(_,_ as RepresentationConfig) >> {object, config -> return new ByteArrayInputStream(bytes)}
        bytes == response.getContentAsByteArray()
        false == response.containsHeader('Content-Length')

        where:
        id   | controllerMethod | value
        null | 'list'           | 'custom1, custom2'
        1    | 'show'           | 'custom1'
        null | 'create'         | 'custom1'
        1    | 'update'         | 'custom1'
        1    | 'delete'         | ""
    }

    @Unroll
    def "Test custom marshaller that returns StreamWrapper"() {
        setup:
        def marshallerService = Mock(MarshallingService)
        config.restfulApiConfig = {
            anyResource {
                representation {
                    mediaTypes = ['application/json']
                    marshallerFramework = 'custom'
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {'string'}
        mock.create(_,_) >> {'string'}
        mock.update(_,_) >> {'string'}
        mock.delete(_,_) >> {}
        controller.metaClass.getService = {-> mock}
        controller.metaClass.getMarshallingService = {String name -> marshallerService}
        mockCacheHeaders()

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', 'application/json')
        request.addHeader('Content-Type', 'application/json')
        if (id != null) params.id = id
        int expectedCount = controllerMethod == 'delete' ? 0 : 1
        boolean expectLength  = controllerMethod != 'delete'
        byte[] bytes = value.getBytes('UTF-8')

        when:
        controller."$controllerMethod"()

        then:
        expectedCount * marshallerService.marshalObject(_,_ as RepresentationConfig) >> {object, config -> return new StreamWrapper(stream: new ByteArrayInputStream(bytes), totalSize:bytes.length)}
        bytes        == response.getContentAsByteArray()
        expectLength == response.containsHeader('Content-Length')

        where:
        id   | controllerMethod | value
        null | 'list'           | 'custom1, custom2'
        1    | 'show'           | 'custom1'
        null | 'create'         | 'custom1'
        1    | 'update'         | 'custom1'
        1    | 'delete'         | ""
    }



    @Unroll
    def "Test using json marshaller framework returns application/json Content-Type header"() {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/custom+json']
                    marshallers {
                        jsonDomainMarshaller {}
                    }
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {new Thing(code:'aa', description:'thing')}
        mock.create(_,_) >> {new Thing(code:'aa', description:'thing')}
        mock.update(_,_) >> {new Thing(code:'aa', description:'thing')}
        mockCacheHeaders()
        controller.metaClass.getService = {-> mock}

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', 'application/custom+json')
        request.addHeader('Content-Type', 'application/custom+json')
        if (id != null) params.id = id
        int expectedCount = controllerMethod == 'delete' ? 0 : 1

        when:
        controller."$controllerMethod"()

        then:
        'application/json;charset=utf-8' == response.contentType

        where:
        id   | controllerMethod
        null | 'list'
        1    | 'show'
        null | 'create'
        1    | 'update'
    }

    @Unroll
    def "Test using xml media type returns application/xml Content-Type header"() {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/custom+xml']
                    marshallers {
                        xmlDomainMarshaller {}
                    }
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {new Thing(code:'aa', description:'thing')}
        mock.create(_,_) >> {new Thing(code:'aa', description:'thing')}
        mock.update(_,_) >> {new Thing(code:'aa', description:'thing')}
        mockCacheHeaders()
        controller.metaClass.getService = {-> mock}

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', 'application/custom+xml')
        request.addHeader('Content-Type', 'application/custom+xml')
        if (id != null) params.id = id
        int expectedCount = controllerMethod == 'delete' ? 0 : 1

        when:
        controller."$controllerMethod"()

        then:
        'application/xml;charset=utf-8' == response.contentType

        where:
        id   | controllerMethod
        null | 'list'
        1    | 'show'
        null | 'create'
        1    | 'update'
    }

    @Unroll
    def "Test using json media type with custom marshaller returns application/json Content-Type header"() {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/custom+json']
                    marshallerFramework = 'custom'
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {new Thing(code:'aa', description:'thing')}
        mock.create(_,_) >> {new Thing(code:'aa', description:'thing')}
        mock.update(_,_) >> {new Thing(code:'aa', description:'thing')}
        def marshallerService = Mock(MarshallingService)
        marshallerService.marshalObject(_,_) >> {return 'string'}
        mockCacheHeaders()
        controller.metaClass.getMarshallingService = {String name -> marshallerService}
        controller.metaClass.getService = {-> mock}

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', 'application/custom+json')
        request.addHeader('Content-Type', 'application/custom+json')
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        'application/json;charset=utf-8' == response.contentType

        where:
        id   | controllerMethod
        null | 'list'
        1    | 'show'
        null | 'create'
        1    | 'update'
    }

    @Unroll
    def "Test using xml media type with custom marshaller returns application/xml Content-Type header"() {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/custom+xml']
                    marshallerFramework = 'custom'
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {new Thing(code:'aa', description:'thing')}
        mock.create(_,_) >> {new Thing(code:'aa', description:'thing')}
        mock.update(_,_) >> {new Thing(code:'aa', description:'thing')}
        def marshallerService = Mock(MarshallingService)
        marshallerService.marshalObject(_,_) >> {return 'string'}
        mockCacheHeaders()
        controller.metaClass.getMarshallingService = {String name -> marshallerService}
        controller.metaClass.getService = {-> mock}

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', 'application/custom+xml')
        request.addHeader('Content-Type', 'application/custom+xml')
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        'application/xml;charset=utf-8' == response.contentType

        where:
        id   | controllerMethod
        null | 'list'
        1    | 'show'
        null | 'create'
        1    | 'update'
    }

    @Unroll
    def "Test specifying content type for json media overrides convention"() {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    contentType = "application/custom"
                    marshallers {
                        marshaller {
                            instance = new DummyJSONMarshaller()
                        }
                    }
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {['dummy':'foo']}
        mock.create(_,_) >> {['dummy':'foo']}
        mock.update(_,_) >> {['dummy':'foo']}
        controller.metaClass.getService = {-> mock}
        mockCacheHeaders()

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', 'application/json')
        request.addHeader('Content-Type', 'application/json')
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        'application/custom;charset=utf-8' == response.contentType

        where:
        id   | controllerMethod
        null | 'list'
        1    | 'show'
        null | 'create'
        1    | 'update'
    }

    @Unroll
    def "Test specifying content type for xml media overrides convention"() {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/xml']
                    contentType = "application/custom"
                    marshallers {
                        marshaller {
                            instance = new DummyXMLMarshaller()
                        }
                    }
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {['dummy':'foo']}
        mock.create(_,_) >> {['dummy':'foo']}
        mock.update(_,_) >> {['dummy':'foo']}
        controller.metaClass.getService = {-> mock}
        mockCacheHeaders()

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', 'application/xml')
        request.addHeader('Content-Type', 'application/xml')
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        'application/custom;charset=utf-8' == response.contentType

        where:
        id   | controllerMethod
        null | 'list'
        1    | 'show'
        null | 'create'
        1    | 'update'
    }

    @Unroll
    def "Test specifying content type for json media overrides convention for custom marshaller"() {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    contentType = "application/custom"
                    marshallerFramework = 'custom'
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {['dummy':'foo']}
        mock.create(_,_) >> {['dummy':'foo']}
        mock.update(_,_) >> {['dummy':'foo']}
        controller.metaClass.getService = {-> mock}
        def marshallerService = Mock(MarshallingService)
        marshallerService.marshalObject(_,_ as RepresentationConfig) >> {object, config -> return 'dummy'}
        controller.metaClass.getMarshallingService = {String name -> marshallerService}
        mockCacheHeaders()

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', 'application/json')
        request.addHeader('Content-Type', 'application/json')
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        'application/custom;charset=utf-8' == response.contentType

        where:
        id   | controllerMethod
        null | 'list'
        1    | 'show'
        null | 'create'
        1    | 'update'
    }

    @Unroll
    def "Test specifying content type for xml media overrides convention for custom marshaller"() {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/xml']
                    contentType = "application/custom"
                    marshallerFramework = 'custom'
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {['dummy':'foo']}
        mock.create(_,_) >> {['dummy':'foo']}
        mock.update(_,_) >> {['dummy':'foo']}
        controller.metaClass.getService = {-> mock}
        def marshallerService = Mock(MarshallingService)
        marshallerService.marshalObject(_,_ as RepresentationConfig) >> {object, config -> return 'dummy'}
        controller.metaClass.getMarshallingService = {String name -> marshallerService}
        mockCacheHeaders()

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', 'application/xml')
        request.addHeader('Content-Type', 'application/xml')
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        'application/custom;charset=utf-8' == response.contentType

        where:
        id   | controllerMethod
        null | 'list'
        1    | 'show'
        null | 'create'
        1    | 'update'
    }

    @Unroll
    def "Test using */* without explicit anyMediaType mapping returns correct Content-Type"() {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/custom+json','application/json']
                    marshallerFramework = 'custom'
                    jsonExtractor {}
                }
                representation {
                    mediaTypes = ['application/custom+v1+json']
                    marshallerFramework = 'custom'
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {new Thing(code:'aa', description:'thing')}
        mock.create(_,_) >> {new Thing(code:'aa', description:'thing')}
        mock.update(_,_) >> {new Thing(code:'aa', description:'thing')}
        def marshallerService = Mock(MarshallingService)
        marshallerService.marshalObject(_,_) >> {return 'string'}
        mockCacheHeaders()
        controller.metaClass.getMarshallingService = {String name -> marshallerService}
        controller.metaClass.getService = {-> mock}

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', '*/*')
        request.addHeader('Content-Type', 'application/json')
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        'application/json;charset=utf-8' == response.contentType
        'application/custom+json'        == response.getHeader('X-hedtech-Media-Type')

        where:
        id   | controllerMethod
        null | 'list'
        1    | 'show'
        null | 'create'
        1    | 'update'
    }

    @Unroll
    def "Test using */* with explicit anyMediaType mapping returns correct Content-Type"() {
        setup:
        config.restfulApiConfig = {
            resource 'things' config {
                anyMediaType = 'application/custom+v1+json'
                representation {
                    mediaTypes = ['application/custom+json']
                    marshallerFramework = 'custom'
                    jsonExtractor {}
                }
                representation {
                    mediaTypes = ['application/json','application/custom+v1+json']
                    marshallerFramework = 'custom'
                    jsonExtractor {}
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {new Thing(code:'aa', description:'thing')}
        mock.create(_,_) >> {new Thing(code:'aa', description:'thing')}
        mock.update(_,_) >> {new Thing(code:'aa', description:'thing')}
        def marshallerService = Mock(MarshallingService)
        marshallerService.marshalObject(_,_) >> {return 'string'}
        mockCacheHeaders()
        controller.metaClass.getMarshallingService = {String name -> marshallerService}
        controller.metaClass.getService = {-> mock}

        params.pluralizedResourceName = 'things'
        params.id = 1
        request.addHeader('Accept', '*/*')
        request.addHeader('Content-Type', 'application/json')
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        'application/json;charset=utf-8' == response.contentType
        'application/custom+v1+json'     == response.getHeader('X-hedtech-Media-Type')

        where:
        id   | controllerMethod
        null | 'list'
        1    | 'show'
        null | 'create'
        1    | 'update'
    }

    @Unroll
    def "Test non json/xml media types can be used for extraction"() {
        setup:
        def theExtractor = Mock(RequestExtractor)
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            anyResource {
                bodyExtractedOnDelete = true
                representation {
                    mediaTypes = ['application/custom']
                    marshallerFramework = 'json'
                    marshallers {
                        jsonBeanMarshaller {}
                    }
                    extractor = theExtractor
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}
        mockCacheHeaders()

        request.addHeader( 'Accept', 'application/custom' )
        request.addHeader( 'Content-Type', 'application/custom' )
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        status == response.status
        1 * theExtractor.extract(_) >> { ['foo':'bar']}
        createCount * mock.create(_,_)
        updateCount * mock.update(_,_)
        deleteCount * mock.delete(_,_)

        where:
        id   | controllerMethod | httpMethod |createCount | updateCount | deleteCount | status
        null | 'create'         | 'POST'     | 1          | 0           | 0           | 201
        1    | 'update'         | 'POST'     | 0          | 1           | 0           | 200
        1    | 'delete'         | 'DELETE'   | 0          | 0           | 1           | 200
    }

    @Unroll
    def "Test non json/xml media types can be used for marshalling"() {
        setup:
        def theExtractor = Mock(RequestExtractor)
        theExtractor.extract(_) >> { ['foo':'bar']}
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            anyResource {
                representation {
                    mediaTypes = ['application/custom']
                    marshallerFramework = 'custom'
                    extractor = theExtractor
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {[:]}
        mock.create(_,_) >> {[:]}
        mock.update(_,_) >> {[:]}
        mock.delete(_,_) >> {}
        controller.metaClass.getService = {-> mock}

        def marshallerService = Mock(MarshallingService)
        controller.metaClass.getMarshallingService = {String name -> marshallerService}

        mockCacheHeaders()

        request.addHeader( 'Accept', 'application/custom' )
        request.addHeader( 'Content-Type', 'application/custom' )
        request.method = ['list','show'].contains(controllerMethod) ? 'GET' : 'POST'
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        status == response.status
        1 * marshallerService.marshalObject(_,_ as RepresentationConfig) >> {object, config -> return "foo"}

        where:
        id   | controllerMethod | status
        null | 'list'           | 200
        1    | 'show'           | 200
        null | 'create'         | 201
        1    | 'update'         | 200
    }

    @Unroll
    def "Test media type is returned as content-type for non-json/non-xml types"() {
        setup:
        def theExtractor = Mock(RequestExtractor)
        theExtractor.extract(_) >> { ['foo':'bar']}
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            anyResource {
                representation {
                    mediaTypes = ['application/custom']
                    marshallerFramework = 'custom'
                    extractor = theExtractor
                }
            }
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {[:]}
        mock.create(_,_) >> {[:]}
        mock.update(_,_) >> {[:]}
        mock.delete(_,_) >> {}
        controller.metaClass.getService = {-> mock}

        def marshallerService = Mock(MarshallingService)
        controller.metaClass.getMarshallingService = {String name -> marshallerService}

        mockCacheHeaders()

        request.addHeader( 'Accept', 'application/custom' )
        request.addHeader( 'Content-Type', 'application/custom' )
        request.method = ['list','show'].contains(controllerMethod) ? 'GET' : 'POST'
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        status == response.status
        1 * marshallerService.marshalObject(_,_ as RepresentationConfig) >> {object, config -> return "foo"}
        'application/custom;charset=utf-8' == response.getHeaderValue('Content-Type')

        where:
        id   | controllerMethod | status
        null | 'list'           | 200
        1    | 'show'           | 200
        null | 'create'         | 201
        1    | 'update'         | 200
    }

    @Unroll
    def "Test missing service returns 404"() {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect 0 invocations
        def mock = Mock(ThingService)
        controller.metaClass.getServiceName = {-> ""}

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/json' )
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        404 == response.status
          0 == response.getContentLength()
          0 * _._

        where:
        controllerMethod | httpMethod | id   | serviceMethod
        'list'           | 'GET'      | null | 'list'
        'show'           | 'GET'      | '1'  | 'show'
        'create'         | 'POST'     | null | 'create'
        'update'         | 'PUT'      | '1'  | 'update'
        'delete'         | 'DELETE'   | '1'  | 'delete'
    }

    @Unroll
    def "Test missing service adapter returns 404"() {
        setup:
         config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect 0 invocations
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}
        controller.metaClass.getServiceAdapterName = {->'noSuchAdapter'}

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/json' )
        request.method = httpMethod
        params.pluralizedResourceName = 'things'
        if (id != null) params.id = id

        when:
        controller."$controllerMethod"()

        then:
        404 == response.status
          0 == response.getContentLength()
          0 * _._

        where:
        controllerMethod | httpMethod | id   | serviceMethod
        'list'           | 'GET'      | null | 'list'
        'show'           | 'GET'      | '1'  | 'show'
        'create'         | 'POST'     | null | 'create'
        'update'         | 'PUT'      | '1'  | 'update'
        'delete'         | 'DELETE'   | '1'  | 'delete'
    }

    def "Test unparsable date in json returns 400"() {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    jsonExtractor {
                        property 'date' date true
                    }
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect 0 invocations
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/json' )
        request.method = 'POST'
        request.setJSON( new JSONObject( ["date":"not a date"] ) )
        params.pluralizedResourceName = 'things'

        when:
        controller.create()

        then:
        400                                     == response.status
        'Validation failed'                     == response.getHeaderValue( 'X-Status-Reason' )
        'default.rest.extractor.unparsableDate' == response.getHeaderValue( 'X-hedtech-message' )
        0 * _._
    }

    def "Test unparsable date in xml returns 400"() {
        setup:
         config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/xml']
                    xmlExtractor {
                        property 'date' date true
                    }
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect 0 invocations
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/xml' )
        request.addHeader( 'Content-Type', 'application/xml' )
        request.method = 'POST'
        request.setXml( "<object><date>not a date</date></object>" )
        params.pluralizedResourceName = 'things'

        when:
        controller.create()

        then:
        400                                     == response.status
        'Validation failed'                     == response.getHeaderValue( 'X-Status-Reason' )
        'default.rest.extractor.unparsableDate' == response.getHeaderValue( 'X-hedtech-message' )
        0 * _._
    }

    def "Test ShortObjectExtractionException returns 400"() {
        //not testing all paths that create this exception
        //just testing that if it occurs, it correctly returns a
        //400 response
        setup:
         config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    jsonExtractor {
                        property 'foo' shortObject true
                    }
                }
            }
        }

        controller.init()

        //mock the appropriate service method, expect 0 invocations
        def mock = Mock(ThingService)
        controller.metaClass.getService = {-> mock}

        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/json' )
        request.method = 'POST'
        request.setJSON( new JSONObject( ["foo":["not a short-object"]] ) )
        params.pluralizedResourceName = 'things'

        when:
        controller.create()

        then:
        400                                            == response.status
        'Validation failed'                            == response.getHeaderValue( 'X-Status-Reason' )
        'default.rest.extractor.unparsableShortObject' == response.getHeaderValue( 'X-hedtech-message' )
        0 * _._
    }

    def "Test PagedResultList"() {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        mock.list(_) >> {return new PagedResultArrayList([[name:'foo']], 5)}
        controller.metaClass.getService = {-> mock}

        mockCacheHeaders()

        request.addHeader( 'Accept', 'application/json' )
        //incoming format always json, so no errors
        request.addHeader( 'Content-Type', 'application/json' )
        params.pluralizedResourceName = 'things'

        when:
        controller.list()
        def json = JSON.parse response.text

        then:
        0*mock.count(_) >> {}
        200   == response.status
        '5'   == response.getHeaderValue( 'X-hedtech-totalCount' )
        1     == json.size()
        'foo' == json[0].name
    }

    def "Test PagedResultListNoTotalCount"() {
        setup:
        //use default extractor for any methods with a request body
        config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def mock = Mock(ThingService)
        mock.list(_) >> {return new PagedResultArrayList([[name:'foo']])}
        controller.metaClass.getService = {-> mock}

        mockCacheHeaders()

        request.addHeader( 'Accept', 'application/json' )
        //incoming format always json, so no errors
        request.addHeader( 'Content-Type', 'application/json' )
        params.pluralizedResourceName = 'things'

        when:
        controller.list()
        def json = JSON.parse response.text

        then:
        0*mock.count(_) >> {}
        200   == response.status
        null  == response.getHeaderValue( 'X-hedtech-totalCount' )
        1     == json.size()
        'foo' == json[0].name
    }

    def "Test PagedResult"() {
        setup:
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    extractor = new DefaultJSONExtractor()
                }
            }
        }
        controller.init()

        //mock the appropriate service method, expect exactly 1 invocation
        def criteria = Mock(org.hibernate.Criteria)
        criteria.list() >> {new ArrayList()}
        def mock = Mock(ThingService)
        mock.list(_) >> {
            def list = new grails.orm.PagedResultList(null,criteria)
            list.totalCount = 2
            list
        }
        controller.metaClass.getService = {-> mock}

        mockCacheHeaders()

        request.addHeader( 'Accept', 'application/json' )
        //incoming format always json, so no errors
        request.addHeader( 'Content-Type', 'application/json' )
        params.pluralizedResourceName = 'things'

        when:
        controller.list()
        def json = JSON.parse response.text

        then:
        0*mock.count(_) >> {}
        200   == response.status
        '2'   == response.getHeaderValue( 'X-hedtech-totalCount' )
    }

    def "Test initialization of ResourceDetailList bean"() {
        setup:
        defineBeans {
            resourceDetailList(ResourceDetailList)
        }
        ResourceDetailList resourceDetailList = applicationContext.getBean('resourceDetailList')
        config.restfulApiConfig =
            {
                resource 'things' config {
                    resourceMetadata = [title: "My Things", authoritative: true]
                    methods = ['list','show','create','update','delete']
                    unsupportedMediaTypeMethods = ['application/vnd.hedtech.v0+json': ['create','update','delete'],
                                                   'application/vnd.hedtech.v1+json': ['delete']]
                    representation {
                        mediaTypes = ['application/vnd.hedtech.v0+json',
                                      'application/vnd.hedtech.v1+json',
                                      'application/vnd.hedtech.v2+json']
                        representationMetadata = [filters: ["filter1", "filter2"]]
                        marshallers {
                            jsonBeanMarshaller {}
                        }
                        jsonExtractor {}
                    }
                }
            }

        when:
        controller.init()

        then:
        null != resourceDetailList
        null != resourceDetailList.resourceDetails
        1 == resourceDetailList.resourceDetails.size()
        def resourceDetail = resourceDetailList.resourceDetails.get(0)
        'things' == resourceDetail.name
        ['list','show','create','update','delete'] == resourceDetail.methods
        ['application/vnd.hedtech.v0+json',
         'application/vnd.hedtech.v1+json',
         'application/vnd.hedtech.v2+json'] == resourceDetail.mediaTypes
        ['application/vnd.hedtech.v0+json': ['create','update','delete'],
         'application/vnd.hedtech.v1+json': ['delete']] == resourceDetail.unsupportedMediaTypeMethods
        [title: "My Things", authoritative: true] == resourceDetail.resourceMetadata
        [filters: ["filter1", "filter2"]] == resourceDetail.representationMetadata.get('application/vnd.hedtech.v0+json')
        [filters: ["filter1", "filter2"]] == resourceDetail.representationMetadata.get('application/vnd.hedtech.v1+json')
        [filters: ["filter1", "filter2"]] == resourceDetail.representationMetadata.get('application/vnd.hedtech.v2+json')
    }

    @Unroll
    def "Unsupported media type method returns 405"(String controllerMethod, String acceptMediaType, String contentMediaType, boolean shouldFail, boolean bodyDelete, def allowHeader ) {
        setup:
        config.restfulApiConfig =
                {
                    resource 'things' config {
                        methods = ['list', 'show', 'create', 'update', 'delete']
                        unsupportedMediaTypeMethods = ['application/vnd.hedtech.v0+json': ['create', 'update', 'delete'],
                                                       'application/vnd.hedtech.v1+json': ['list', 'show']]
                        representation {
                            mediaTypes = ['application/vnd.hedtech.v0+json',
                                          'application/vnd.hedtech.v1+json',
                                          'application/json']
                            marshallerFramework = 'custom'
                            jsonExtractor {}
                        }
                        bodyExtractedOnDelete = bodyDelete
                    }
                }
        controller.init()

        //mock the appropriate service method, but expect no method calls
        //(since the request cannot be understood, the service should not be contacted)
        def marshallerService = Mock(MarshallingService)
        def mock = Mock(ThingService)
        mock.list(_) >> {[]}
        mock.count(_) >> {0}
        mock.show(_) >> {'string'}
        mock.create(_,_) >> {'string'}
        mock.update(_,_) >> {'string'}
        mock.delete(_,_) >> {}
        controller.metaClass.getService = {-> mock}
        controller.metaClass.getMarshallingService = {String name -> marshallerService}
        mockCacheHeaders()

        params.pluralizedResourceName = 'things'
        params.id = 1

        when:
        request.addHeader('Accept', acceptMediaType)
        request.addHeader('Content-Type', contentMediaType)
        controller."$controllerMethod"()

        then:
        (shouldFail ? 405 : (controllerMethod == 'create' ? 201 : 200)) == response.status
        0 == response.getContentLength()
        allowHeader.size() == response.headers( 'Allow' ).size()
        allowHeader as Set == response.headers( 'Allow') as Set

        where:
        controllerMethod | acceptMediaType                   | contentMediaType                  | shouldFail | bodyDelete | allowHeader
        'list'           | 'application/json'                | 'application/json'                | false      | false      | []
        'list'           | 'application/vnd.hedtech.v0+json' | 'application/json'                | false      | false      | []
        'list'           | 'application/vnd.hedtech.v1+json' | 'application/json'                | true       | false      | ["POST"]
        'show'           | 'application/json'                | 'application/json'                | false      | false      | []
        'show'           | 'application/vnd.hedtech.v0+json' | 'application/json'                | false      | false      | []
        'show'           | 'application/vnd.hedtech.v1+json' | 'application/json'                | true       | false      | ["PUT","DELETE"]
        'create'         | 'application/json'                | 'application/json'                | false      | false      | []
        'create'         | 'application/json'                | 'application/vnd.hedtech.v0+json' | true       | false      | ["GET"]
        'create'         | 'application/json'                | 'application/vnd.hedtech.v1+json' | false      | false      | []
        'update'         | 'application/json'                | 'application/json'                | false      | false      | []
        'update'         | 'application/json'                | 'application/vnd.hedtech.v0+json' | true       | false      | ["GET"]
        'update'         | 'application/json'                | 'application/vnd.hedtech.v1+json' | false      | false      | []
        'delete'         | 'application/json'                | 'application/json'                | false      | false      | []
        'delete'         | 'application/json'                | 'application/vnd.hedtech.v0+json' | true       | false      | ["GET"]
        'delete'         | 'application/json'                | 'application/vnd.hedtech.v1+json' | false      | false      | []
        'delete'         | 'application/json'                | 'application/json'                | false      | true       | []
        'delete'         | 'application/json'                | 'application/vnd.hedtech.v0+json' | true       | true       | ["GET"]
        'delete'         | 'application/json'                | 'application/vnd.hedtech.v1+json' | false      | true       | []
    }

    @Unroll
    def "Test API Versioning"() {
        setup:
        config.restfulApiConfig =
                {
                    resource 'things' config {
                        methods = ['list', 'show', 'create', 'update', 'delete']
                        representation {
                            mediaTypes = ['application/vnd.hedtech.v1+json',
                                          'application/vnd.hedtech.v2+json',
                                          'application/vnd.hedtech+json']
                            marshallers {
                                jsonBeanMarshaller {}
                            }
                            jsonExtractor {}
                        }
                        representation {
                            mediaTypes = ['application/vnd.hedtech.v3.0.0+json',
                                          'application/vnd.hedtech.v3.1.0+json',
                                          'application/json']
                            marshallers {
                                jsonBeanMarshaller {}
                            }
                            jsonExtractor {}
                        }
                        representation {
                            mediaTypes = ['application/vnd.hedtech.custom+json']
                            marshallers {
                                jsonBeanMarshaller {}
                            }
                            jsonExtractor {}
                        }
                    }
                }
        defineBeans {
            apiVersionParser(BasicApiVersionParser)
        }
        def genericMediaTypeList = ['application/json', 'application/vnd.hedtech+json', 'application/vnd.hedtech.custom+json']
        if (override) {
            controller.metaClass.getOverrideGenericMediaType = {-> override}
            controller.metaClass.getGenericMediaTypeList = {-> genericMediaTypeList}
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> { serviceReturn }
        mock.count(_) >> { serviceReturn.size() }
        mock.show(_) >> { serviceReturn }
        mock.create(_,_) >> { serviceReturn }
        mock.update(_,_) >> { serviceReturn }
        mock.delete(_,_) >> {}
        controller.metaClass.getService = {-> mock}
        mockCacheHeaders()
        params.pluralizedResourceName = 'things'
        params.id = 1
        def httpMethod = calculateHttpMethod(controllerMethod)

        when:
        request.addHeader('Accept', mediaType)
        request.addHeader('Content-Type', mediaType)
        request.method = httpMethod
        controller."$controllerMethod"()

        then:
        (controllerMethod == 'create' ? 201 : 200) == response.status
        mediaTypeHeader == response.getHeader('X-hedtech-Media-Type')
        apiVersion == request.getAttribute(RepresentationRequestAttributes.RESPONSE_REPRESENTATION)?.apiVersion?.version
        (httpMethod == 'GET' ? null : apiVersion) == request.getAttribute(RepresentationRequestAttributes.REQUEST_REPRESENTATION)?.apiVersion?.version

        where:
        controllerMethod | serviceReturn | override | mediaType          | apiVersion | mediaTypeHeader
        'list'           | ['foo']       | false    | 'application/json' | null       | 'application/json'
        'show'           | [name:'foo']  | false    | 'application/json' | null       | 'application/json'
        'create'         | [name:'foo']  | false    | 'application/json' | null       | 'application/json'
        'update'         | [name:'foo']  | false    | 'application/json' | null       | 'application/json'
        'delete'         | null          | false    | 'application/json' | null       | null
        // test override=true with generic mediaType=application/json
        'list'           | ['foo']       | true     | 'application/json' | 'v3.1.0'   | 'application/vnd.hedtech.v3.1.0+json'
        'show'           | [name:'foo']  | true     | 'application/json' | 'v3.1.0'   | 'application/vnd.hedtech.v3.1.0+json'
        'create'         | [name:'foo']  | true     | 'application/json' | 'v3.1.0'   | 'application/vnd.hedtech.v3.1.0+json'
        'update'         | [name:'foo']  | true     | 'application/json' | 'v3.1.0'   | 'application/vnd.hedtech.v3.1.0+json'
        'delete'         | null          | true     | 'application/json' | null       | null
        // test override=true with generic mediaType=application/vnd.hedtech+json
        'list'           | ['foo']       | true     | 'application/vnd.hedtech+json' | 'v2'   | 'application/vnd.hedtech.v2+json'
        'show'           | [name:'foo']  | true     | 'application/vnd.hedtech+json' | 'v2'   | 'application/vnd.hedtech.v2+json'
        'create'         | [name:'foo']  | true     | 'application/vnd.hedtech+json' | 'v2'   | 'application/vnd.hedtech.v2+json'
        'update'         | [name:'foo']  | true     | 'application/vnd.hedtech+json' | 'v2'   | 'application/vnd.hedtech.v2+json'
        'delete'         | null          | true     | 'application/vnd.hedtech+json' | null   | null
        // test override=true with generic mediaType=application/vnd.hedtech.custom+json
        'list'           | ['foo']       | true     | 'application/vnd.hedtech.custom+json' | null   | 'application/vnd.hedtech.custom+json'
        'show'           | [name:'foo']  | true     | 'application/vnd.hedtech.custom+json' | null   | 'application/vnd.hedtech.custom+json'
        'create'         | [name:'foo']  | true     | 'application/vnd.hedtech.custom+json' | null   | 'application/vnd.hedtech.custom+json'
        'update'         | [name:'foo']  | true     | 'application/vnd.hedtech.custom+json' | null   | 'application/vnd.hedtech.custom+json'
        'delete'         | null          | true     | 'application/vnd.hedtech.custom+json' | null   | null
        // test override=true with actual mediaType=application/vnd.hedtech.v1+json
        'list'           | ['foo']       | true     | 'application/vnd.hedtech.v1+json' | 'v1'   | 'application/vnd.hedtech.v1+json'
        'show'           | [name:'foo']  | true     | 'application/vnd.hedtech.v1+json' | 'v1'   | 'application/vnd.hedtech.v1+json'
        'create'         | [name:'foo']  | true     | 'application/vnd.hedtech.v1+json' | 'v1'   | 'application/vnd.hedtech.v1+json'
        'update'         | [name:'foo']  | true     | 'application/vnd.hedtech.v1+json' | 'v1'   | 'application/vnd.hedtech.v1+json'
        'delete'         | null          | true     | 'application/vnd.hedtech.v1+json' | null   | null
    }

    @Unroll
    def "Test Use Highest Semantic Version"() {
        setup:
        config.restfulApiConfig =
                {
                    resource 'things' config {
                        methods = ['list', 'show', 'create', 'update', 'delete']
                        representation {
                            mediaTypes = ['application/vnd.hedtech.v6+json',
                                          'application/vnd.hedtech.v7+json',
                                          'application/vnd.hedtech.v7.0.0+json',
                                          'application/vnd.hedtech.v7.1.0+json',
                                          'application/vnd.hedtech.v7.2.0+json',
                                          'application/vnd.hedtech.v7.2.1+json',
                                          'application/vnd.hedtech.v8+json',
                                          'application/vnd.hedtech.v8.0.0+json',
                                          'application/json']
                            marshallers {
                                jsonBeanMarshaller {}
                            }
                            jsonExtractor {}
                        }
                    }
                }
        defineBeans {
            apiVersionParser(BasicApiVersionParser)
        }
        if (override) {
            controller.metaClass.getUseHighestSemanticVersion = {-> override}
        }
        if (legacy) {
            controller.metaClass.getUseAcceptHeaderAsMediaTypeHeader = {-> legacy}
        }
        controller.init()

        def mock = Mock(ThingService)
        mock.list(_) >> { serviceReturn }
        mock.count(_) >> { serviceReturn.size() }
        mock.show(_) >> { serviceReturn }
        mock.create(_,_) >> { serviceReturn }
        mock.update(_,_) >> { serviceReturn }
        mock.delete(_,_) >> {}
        controller.metaClass.getService = {-> mock}
        mockCacheHeaders()
        params.pluralizedResourceName = 'things'
        params.id = 1
        def httpMethod = calculateHttpMethod(controllerMethod)

        when:
        request.addHeader('Accept', mediaType)
        request.addHeader('Content-Type', mediaType)
        request.method = httpMethod
        controller."$controllerMethod"()

        then:
        (controllerMethod == 'create' ? 201 : 200) == response.status
        (overrideMediaTypeHeader ?: mediaType) == response.getHeader('X-hedtech-Media-Type')
        apiVersion == request.getAttribute(RepresentationRequestAttributes.RESPONSE_REPRESENTATION)?.apiVersion?.version
        (httpMethod == 'GET' ? null : apiVersion) == request.getAttribute(RepresentationRequestAttributes.REQUEST_REPRESENTATION)?.apiVersion?.version

        where:
        controllerMethod | serviceReturn | override | legacy | mediaType                             | apiVersion | overrideMediaTypeHeader
        // test useHighestSemanticVersion=false and useAcceptHeaderAsMediaTypeHeader=false
        'list'           | ['foo']       | false    | false  | 'application/vnd.hedtech.v6+json'     | 'v6'       | null
        'list'           | ['foo']       | false    | false  | 'application/vnd.hedtech.v7+json'     | 'v7'       | null
        'list'           | ['foo']       | false    | false  | 'application/vnd.hedtech.v7.0.0+json' | 'v7.0.0'   | null
        'list'           | ['foo']       | false    | false  | 'application/vnd.hedtech.v7.1.0+json' | 'v7.1.0'   | null
        'list'           | ['foo']       | false    | false  | 'application/vnd.hedtech.v7.2.0+json' | 'v7.2.0'   | null
        'list'           | ['foo']       | false    | false  | 'application/vnd.hedtech.v7.2.1+json' | 'v7.2.1'   | null
        'list'           | ['foo']       | false    | false  | 'application/vnd.hedtech.v8+json'     | 'v8'       | null
        'list'           | ['foo']       | false    | false  | 'application/vnd.hedtech.v8.0.0+json' | 'v8.0.0'   | null
        'list'           | ['foo']       | false    | false  | 'application/json'                    | null       | null
        // test useHighestSemanticVersion=true and useAcceptHeaderAsMediaTypeHeader=false
        'list'           | ['foo']       | true     | false  | 'application/vnd.hedtech.v6+json'     | 'v6'       | null
        'list'           | ['foo']       | true     | false  | 'application/vnd.hedtech.v7+json'     | 'v7.2.1'   | 'application/vnd.hedtech.v7.2.1+json'
        'list'           | ['foo']       | true     | false  | 'application/vnd.hedtech.v7.0.0+json' | 'v7.2.1'   | 'application/vnd.hedtech.v7.2.1+json'
        'list'           | ['foo']       | true     | false  | 'application/vnd.hedtech.v7.1.0+json' | 'v7.2.1'   | 'application/vnd.hedtech.v7.2.1+json'
        'list'           | ['foo']       | true     | false  | 'application/vnd.hedtech.v7.2.0+json' | 'v7.2.1'   | 'application/vnd.hedtech.v7.2.1+json'
        'list'           | ['foo']       | true     | false  | 'application/vnd.hedtech.v7.2.1+json' | 'v7.2.1'   | null
        'list'           | ['foo']       | true     | false  | 'application/vnd.hedtech.v8+json'     | 'v8.0.0'   | 'application/vnd.hedtech.v8.0.0+json'
        'list'           | ['foo']       | true     | false  | 'application/vnd.hedtech.v8.0.0+json' | 'v8.0.0'   | null
        'list'           | ['foo']       | true     | false  | 'application/json'                    | null       | null
        // test useHighestSemanticVersion=true and useAcceptHeaderAsMediaTypeHeader=false
        'create'         | ['foo']       | true     | false  | 'application/vnd.hedtech.v6+json'     | 'v6'       | null
        'create'         | ['foo']       | true     | false  | 'application/vnd.hedtech.v7+json'     | 'v7.2.1'   | 'application/vnd.hedtech.v7.2.1+json'
        'create'         | ['foo']       | true     | false  | 'application/vnd.hedtech.v7.0.0+json' | 'v7.2.1'   | 'application/vnd.hedtech.v7.2.1+json'
        'create'         | ['foo']       | true     | false  | 'application/vnd.hedtech.v7.1.0+json' | 'v7.2.1'   | 'application/vnd.hedtech.v7.2.1+json'
        'create'         | ['foo']       | true     | false  | 'application/vnd.hedtech.v7.2.0+json' | 'v7.2.1'   | 'application/vnd.hedtech.v7.2.1+json'
        'create'         | ['foo']       | true     | false  | 'application/vnd.hedtech.v7.2.1+json' | 'v7.2.1'   | null
        'create'         | ['foo']       | true     | false  | 'application/vnd.hedtech.v8+json'     | 'v8.0.0'   | 'application/vnd.hedtech.v8.0.0+json'
        'create'         | ['foo']       | true     | false  | 'application/vnd.hedtech.v8.0.0+json' | 'v8.0.0'   | null
        'create'         | ['foo']       | true     | false  | 'application/json'                    | null       | null
        // test useHighestSemanticVersion=false and useAcceptHeaderAsMediaTypeHeader=true
        'list'           | ['foo']       | false    | true   | 'application/vnd.hedtech.v6+json'     | 'v6'       | null
        'list'           | ['foo']       | false    | true   | 'application/vnd.hedtech.v7+json'     | 'v7'       | null
        'list'           | ['foo']       | false    | true   | 'application/vnd.hedtech.v7.0.0+json' | 'v7.0.0'   | null
        'list'           | ['foo']       | false    | true   | 'application/vnd.hedtech.v7.1.0+json' | 'v7.1.0'   | null
        'list'           | ['foo']       | false    | true   | 'application/vnd.hedtech.v7.2.0+json' | 'v7.2.0'   | null
        'list'           | ['foo']       | false    | true   | 'application/vnd.hedtech.v7.2.1+json' | 'v7.2.1'   | null
        'list'           | ['foo']       | false    | true   | 'application/vnd.hedtech.v8+json'     | 'v8'       | null
        'list'           | ['foo']       | false    | true   | 'application/vnd.hedtech.v8.0.0+json' | 'v8.0.0'   | null
        'list'           | ['foo']       | false    | true   | 'application/json'                    | null       | null
        // test useHighestSemanticVersion=true and useAcceptHeaderAsMediaTypeHeader=true
        'list'           | ['foo']       | true     | true   | 'application/vnd.hedtech.v6+json'     | 'v6'       | null
        'list'           | ['foo']       | true     | true   | 'application/vnd.hedtech.v7+json'     | 'v7.2.1'   | null
        'list'           | ['foo']       | true     | true   | 'application/vnd.hedtech.v7.0.0+json' | 'v7.2.1'   | null
        'list'           | ['foo']       | true     | true   | 'application/vnd.hedtech.v7.1.0+json' | 'v7.2.1'   | null
        'list'           | ['foo']       | true     | true   | 'application/vnd.hedtech.v7.2.0+json' | 'v7.2.1'   | null
        'list'           | ['foo']       | true     | true   | 'application/vnd.hedtech.v7.2.1+json' | 'v7.2.1'   | null
        'list'           | ['foo']       | true     | true   | 'application/vnd.hedtech.v8+json'     | 'v8.0.0'   | null
        'list'           | ['foo']       | true     | true   | 'application/vnd.hedtech.v8.0.0+json' | 'v8.0.0'   | null
        'list'           | ['foo']       | true     | true   | 'application/json'                    | null       | null
        // test useHighestSemanticVersion=true and useAcceptHeaderAsMediaTypeHeader=true
        'create'         | ['foo']       | true     | true   | 'application/vnd.hedtech.v6+json'     | 'v6'       | null
        'create'         | ['foo']       | true     | true   | 'application/vnd.hedtech.v7+json'     | 'v7.2.1'   | null
        'create'         | ['foo']       | true     | true   | 'application/vnd.hedtech.v7.0.0+json' | 'v7.2.1'   | null
        'create'         | ['foo']       | true     | true   | 'application/vnd.hedtech.v7.1.0+json' | 'v7.2.1'   | null
        'create'         | ['foo']       | true     | true   | 'application/vnd.hedtech.v7.2.0+json' | 'v7.2.1'   | null
        'create'         | ['foo']       | true     | true   | 'application/vnd.hedtech.v7.2.1+json' | 'v7.2.1'   | null
        'create'         | ['foo']       | true     | true   | 'application/vnd.hedtech.v8+json'     | 'v8.0.0'   | null
        'create'         | ['foo']       | true     | true   | 'application/vnd.hedtech.v8.0.0+json' | 'v8.0.0'   | null
        'create'         | ['foo']       | true     | true   | 'application/json'                    | null       | null
    }

    private calculateHttpMethod(String controllerMethod) {
        def httpMethod
        switch (controllerMethod) {
            case 'list':
                httpMethod = 'GET'
                break
            case 'show':
                httpMethod = 'GET'
                break
            case 'create':
                httpMethod = 'POST'
                break
            case 'update':
                httpMethod = 'PUT'
                break
            case 'delete':
                httpMethod = 'DELETE'
                break
            default:
                fail("Unable to set request.method based on controllerMethod: " + controllerMethod)
        }
        return httpMethod
    }

    private void mockCacheHeaders() {
        def cacheHeadersService = new CacheHeadersService()
        Closure withCacheHeadersClosure = { Closure c ->
            c.delegate = controller
            c.resolveStrategy = Closure.DELEGATE_ONLY
            cacheHeadersService.withCacheHeaders( c.delegate, c )
        }
        controller.metaClass.withCacheHeaders = withCacheHeadersClosure
    }

    static class DummyJSONMarshaller implements ObjectMarshaller<JSON> {
        @Override
        public boolean supports(Object object) {
            true
        }

         @Override
        public void marshalObject(Object value, JSON json) throws ConverterException {
            def writer = json.getWriter()
            writer.object()
            json.property('name','dummy')
        }
    }

    static class DummyXMLMarshaller implements ObjectMarshaller<XML> {
        @Override
        public boolean supports(Object object) {
            true
        }

         @Override
        public void marshalObject(Object value, XML xml) throws ConverterException {
            xml.startNode('dummy')
            xml.end()
        }
    }

    static class CheckedApplicationException extends Exception {

        private int statusCode
        private def message

        CheckedApplicationException( def statusCode, def message ) {
            this.statusCode = statusCode.toInteger()
            this.message = message
        }

        public def getHttpStatusCode() {
            return statusCode
        }

        String getMessage() {
            return this.message
        }

        public returnMap = { localize ->
            def map = [:]
            if (getMessage()) {
                map['message'] = getMessage()
            }
            map
        }
    }

    static class DummyServiceAdapter implements RestfulServiceAdapter {
        String name

        def list(def service, Map params) throws Throwable {
            service.list(params)
        }

        def count(def service, Map params) throws Throwable {
            service.count(params)
        }

        def show(def service, Map params) throws Throwable {
            service.show(params)
        }

        def create(def service, Map content, Map params) throws Throwable {
             service.create(content, params)
        }

        def update(def service, Map content, Map params) throws Throwable {
            service.update(content,params)
        }

        void delete(def service, Map content, Map params) throws Throwable {
            service.delete(content,params)
        }
    }

    static class CheckedApplicationExceptionHandler implements ExceptionHandler {
        boolean supports(Throwable t) {
            t instanceof CheckedApplicationException
        }

        ErrorResponse handle(Throwable t, ExceptionHandlerContext context) {
            new ErrorResponse(
                httpStatusCode: 403,
                message: 'dummy message'
            )
        }
    }

    static class DefaultExceptionHandler implements ExceptionHandler {
        boolean supports(Throwable t) {
            true
        }

        ErrorResponse handle(Throwable t, ExceptionHandlerContext context) {
            new ErrorResponse(
                httpStatusCode: 403,
                message: 'dummy message'
            )
        }
    }
}
