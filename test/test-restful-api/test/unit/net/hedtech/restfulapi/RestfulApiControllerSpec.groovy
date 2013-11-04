/* ****************************************************************************
 * Copyright 2013 Ellucian Company L.P. and its affiliates.
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
          1 * mock.delete(_,_,_) >> {}
    }

    @Unroll
    def "Unsupported media type in Content-Type header returns 415"(String controllerMethod, String httpMethod, String id, String serviceMethod, def serviceReturn ) {
        setup:
         config.restfulApiConfig = {
            resource 'things' config {
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
        'update'         | 'PUT'      | '1'  | '{id:2}'
        'delete'         | 'DELETE'   | '1'  | '{id:2}'
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
        1*mock.update(_,_,_) >> { throw new org.springframework.dao.OptimisticLockingFailureException( "foo" ) }
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
        1*mock.update(_,_,_) >> { throw new Exception( 'foo' ) }
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

    def "Test that delete with empty body ignores Content-Type"() {
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

        request.setContent( new byte[0] )
        request.addHeader( 'Accept', 'application/json' )
        request.addHeader( 'Content-Type', 'application/xml' )
        params.pluralizedResourceName = 'things'

        when:
        controller.delete()

        then:
        200 == response.status
          0 == response.getContentLength()
          1*mock.delete(_,_,_) >> { }
        'default.rest.deleted.message' == response.getHeaderValue( 'X-hedtech-message' )
    }

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
        updateCount * mock.update(_,_,_) >> { serviceReturn }
        createCount * mock.create(_,_) >> { serviceReturn }
        deleteCount * mock.delete(_,_,_) >> {}
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
    def "Test delegation to JSONExtractor"() {
        setup:
        def theExtractor = Mock(JSONExtractor)
        //use default extractor for any methods with a request body
         config.restfulApiConfig = {
            anyResource {
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
        updateCount * mock.update(_,_,_)
        deleteCount * mock.delete(_,_,_)

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
        updateCount * mock.update(_,_,_)
        deleteCount * mock.delete(_,_,_)

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
        updateCount * mock.update(_,_,_)
        deleteCount * mock.delete(_,_,_)

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
        mock.update(_,_,_) >> {'string'}
        mock.delete(_,_,_) >> {}
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
        mock.update(_,_,_) >> {'string'}
        mock.delete(_,_,_) >> {}
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
        mock.update(_,_,_) >> {'string'}
        mock.delete(_,_,_) >> {}
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
        mock.update(_,_,_) >> {'string'}
        mock.delete(_,_,_) >> {}
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
        mock.update(_,_,_) >> {new Thing(code:'aa', description:'thing')}
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
        mock.update(_,_,_) >> {new Thing(code:'aa', description:'thing')}
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
        mock.update(_,_,_) >> {new Thing(code:'aa', description:'thing')}
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
        mock.update(_,_,_) >> {new Thing(code:'aa', description:'thing')}
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
        mock.update(_,_,_) >> {['dummy':'foo']}
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
        mock.update(_,_,_) >> {['dummy':'foo']}
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
        mock.update(_,_,_) >> {['dummy':'foo']}
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
        mock.update(_,_,_) >> {['dummy':'foo']}
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
        mock.update(_,_,_) >> {new Thing(code:'aa', description:'thing')}
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
        mock.update(_,_,_) >> {new Thing(code:'aa', description:'thing')}
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
        updateCount * mock.update(_,_,_)
        deleteCount * mock.delete(_,_,_)

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
        mock.update(_,_,_) >> {[:]}
        mock.delete(_,_,_) >> {}
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
        mock.update(_,_,_) >> {[:]}
        mock.delete(_,_,_) >> {}
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
}
