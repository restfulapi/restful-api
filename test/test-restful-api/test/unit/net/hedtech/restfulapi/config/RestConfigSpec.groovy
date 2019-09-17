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

package net.hedtech.restfulapi.config

import grails.test.mixin.*
import grails.test.mixin.support.*

import net.hedtech.restfulapi.*
import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*
import net.hedtech.restfulapi.marshallers.json.*

import spock.lang.*


@TestMixin(GrailsUnitTestMixin)
class RestConfigSpec extends Specification {

    def "Test simple configuration for one media type"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                        marshaller {
                            instance = 'b'
                            priority = 6
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )

        then:
        1 == config.resources.size()
        1 == config.resources['things'].representations.size()
    }

    def "Test that attempt to rename resource fails"() {
        setup:
        def src =
        {
            resource 'things' config {
                name = 'changed'
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )

        then:
        def e = thrown(RuntimeException)
        "Name of resource illegally changed" == e.message

    }

    def "Test a resource with 2 representations defined"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'v0'
                            priority = 5
                        }
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaTypes = ['application/vnd.hedtech.v1+json']
                    marshallers {
                        marshaller {
                            instance = 'v1'
                            priority = 5
                        }
                    }
                    extractor = 'v1'
                }
            }
        }

        when:
        def conf = RestConfig.parse( grailsApplication, src )

        then:
        1 == conf.resources.size()
        2 == conf.resources['things'].representations.size()
    }

    def "Test resources missing extractors and marshallers"() {
        setup:
        def src = {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        marshaller {
                            instance = 'foo'
                            priority = 5
                        }
                    }
                }
                representation {
                    mediaTypes = ['application/xml']
                    extractor = 'foo'
                }
                representation {
                    mediaTypes = ['application/custom-json']
                }
            }
        }

        when:
        def conf = RestConfig.parse( grailsApplication, src )

        then:
        1     == conf.resources.size()
        3     == conf.resources['things'].representations.size()
        conf.resources['things'].representations.values().each {
            null != it.mediaType
        }

    }

    def "Test content negotiation for representation"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'v0'
                            priority = 5
                        }
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaTypes = ['application/vnd.hedtech.v1+json']
                    marshallers {
                        marshaller {
                            instance = 'v1'
                            priority = 5
                        }
                    }
                    extractor = 'v1'
                }
            }
        }
        def types = ['application/vnd.hedtech.v1+json',
                     'application/vnd.hedtech.v0+json']

        when:
        def config = RestConfig.parse( grailsApplication, src )
        RepresentationConfig conf = config.getRepresentation('things', types )

        then:
        null                              != conf
        'application/vnd.hedtech.v1+json' == conf.mediaType
    }

    def "Test multiple media types assigned to representation"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json','application/json']
                    marshallers {
                        marshaller {
                            instance = 'v0'
                            priority = 5
                        }
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaTypes = ['application/vnd.hedtech.v1+json']
                    marshallers {
                        marshaller {
                            instance = 'v1'
                            priority = 5
                        }
                    }
                    extractor = 'v1'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        RepresentationConfig defaultConfig = config.getRepresentation( 'things', 'application/json' )
        RepresentationConfig version0 = config.getRepresentation( 'things', 'application/vnd.hedtech.v0+json' )
        RepresentationConfig version1 = config.getRepresentation( 'things', 'application/vnd.hedtech.v1+json' )

        then:
        'application/json'                == defaultConfig.mediaType
        'v0'                              == defaultConfig.marshallers[0].instance
        'v0'                              == defaultConfig.extractor
        'application/vnd.hedtech.v0+json' == version0.mediaType
        'v0'                              == version0.marshallers[0].instance
        'v0'                              == version0.extractor
        'application/vnd.hedtech.v1+json' == version1.mediaType
        'v1'                              == version1.marshallers[0].instance
        'v1'                              == version1.extractor
    }

    def "Test detection when the same media type is assigned to two different representations"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        marshaller {
                            instance = 'v0'
                            priority = 5
                        }
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        marshaller {
                            instance = 'v1'
                            priority = 5
                        }
                    }
                    extractor = 'v1'
                }
            }
        }

        when:
        RestConfig.parse( grailsApplication, src )

        then:
        AmbiguousRepresentationException e = thrown()
        'things'           == e.resourceName
        'application/json' == e.mediaType
    }

    def "Test marshaller groups"() {
        setup:
        def src =
        {
            marshallerGroups {
                group 'defaultJSON' marshallers {
                    marshaller {
                        instance = 'defaultJsonDate'
                        priority = 5
                    }
                    marshaller {
                        instance = 'foo'
                        priority = 10
                    }
                }
                group 'thingDefaults' marshallers {
                    marshaller {
                        instance = 'defaultThing'
                        priority = 5
                    }
                }
            }

            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        marshaller {
                            instance = 'customThing'
                            priority = 15
                        }
                        marshallerGroup 'defaultJSON'
                        marshaller {
                            instance = 'customDate'
                            priority = 15
                        }
                        marshallerGroup 'thingDefaults'
                    }
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        RepresentationConfig rep = config.getRepresentation( 'things', 'application/json' )

        then:
        ['customThing','defaultJsonDate','foo','customDate','defaultThing'] == rep.marshallers.instance
        [15,5,10,15,5] == rep.marshallers.priority
    }

    def "Test default marshaller groups"() {
        setup:
        def src =
        {
            marshallerGroups {
                group 'json' marshallers {
                    marshaller {
                        instance = 'defaultJsonDate'
                        priority = 5
                    }
                    marshaller {
                        instance = 'foo'
                        priority = 10
                    }
                }
                group 'xml' marshallers {
                    marshaller {
                        instance = 'defaultXmlDate'
                        priority = 5
                    }

                    marshaller {
                        instance = 'bar'
                        priority = 5
                    }
                }
            }

            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        marshaller {
                            instance = 'customJsonThing'
                            priority = 15
                        }
                    }
                }
                representation {
                    mediaTypes = ['application/xml']
                    marshallers {
                        marshaller {
                            instance = 'customXmlThing'
                            priority = 15
                        }
                    }
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        RepresentationConfig jsonRep = config.getRepresentation( 'things', 'application/json' )
        RepresentationConfig xmlRep = config.getRepresentation( 'things', 'application/xml' )

        then:
        ['defaultJsonDate','foo','customJsonThing'] == jsonRep.marshallers.instance
        [5,10,15]                                   == jsonRep.marshallers.priority
        ['defaultXmlDate','bar','customXmlThing']   == xmlRep.marshallers.instance
        [5,5,15]                                    == xmlRep.marshallers.priority
    }

    def "Test default marshaller groups with anyResource"() {
        setup:
        def src =
        {
            marshallerGroups {
                group 'json' marshallers {
                    marshaller {
                        instance = 'defaultJsonDate'
                        priority = 5
                    }
                    marshaller {
                        instance = 'foo'
                        priority = 10
                    }
                }
                group 'xml' marshallers {
                    marshaller {
                        instance = 'defaultXmlDate'
                        priority = 5
                    }

                    marshaller {
                        instance = 'bar'
                        priority = 5
                    }
                }
            }

            anyResource {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        marshaller {
                            instance = 'customJson'
                            priority = 15
                        }
                    }
                }
                representation {
                    mediaTypes = ['application/xml']
                    marshallers {
                        marshaller {
                            instance = 'customXml'
                            priority = 15
                        }
                    }
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        RepresentationConfig jsonRep = config.getRepresentation( '', 'application/json' )
        RepresentationConfig xmlRep = config.getRepresentation( '', 'application/xml' )

        then:
        ['defaultJsonDate','foo','customJson'] == jsonRep.marshallers.instance
        [5,10,15]                              == jsonRep.marshallers.priority
        ['defaultXmlDate','bar','customXml']   == xmlRep.marshallers.instance
        [5,5,15]                               == xmlRep.marshallers.priority
    }

    def "Test missing marshaller group"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        marshallerGroup 'defaultJSON'
                    }
                }
            }
        }

        when:
        RestConfig.parse( grailsApplication, src )

        then:
        def e = thrown(MissingMarshallerGroupException)
        'defaultJSON' == e.name
    }

    def "Test service name override"() {
        setup:
        def src =
        {
            resource 'things' config {
                serviceName = 'TheThingService'
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                        marshaller {
                            instance = 'b'
                            priority = 6
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )

        then:
        1 == config.resources.size()
        'TheThingService' == config.resources['things'].serviceName
    }

    def "Test service adapter name override"() {
        setup:
        def src =
        {
            resource 'things' config {
                serviceAdapterName = 'thingServiceAdapter'
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                        marshaller {
                            instance = 'b'
                            priority = 6
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )

        then:
        1 == config.resources.size()
        'thingServiceAdapter' == config.resources['things'].serviceAdapterName
    }

    def "Test method overrides"() {
        setup:
        def src =
        {
            resource 'things' config {
                methods = ['list','show']
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                        marshaller {
                            instance = 'b'
                            priority = 6
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        ['list','show'] == config.getResource( 'things' ).getMethods()
    }

    def "Test invalid method name"() {
        setup:
        def src =
        {
            resource 'things' config {
                methods = ['list','show','foo']
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                        marshaller {
                            instance = 'b'
                            priority = 6
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        def e = thrown(UnknownMethodException)
        'things' == e.resourceName
        'foo' == e.methodName
    }

    def "Test exception when methods on resource is not a collection"() {
        setup:
        def src =
        {
            resource 'things' config {
                methods = "list,show"
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                        marshaller {
                            instance = 'b'
                            priority = 6
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        def e = thrown(MethodsNotCollectionException)
        'things' == e.resourceName
    }

    def "Test anyResource"() {
        setup:
        def src =
        {
            anyResource {
                methods = ['list','show']
                anyMediaType = 'application/custom'
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                        marshaller {
                            instance = 'b'
                            priority = 6
                        }
                    }
                    extractor = 'DynamicJsonExtractor'
                }

                representation {
                    mediaTypes = ['application/custom']
                    marshallers {
                    }
                    extractor = 'custom'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def resource = config.getResource("")
        def rep = resource.getRepresentation('application/json')
        def wildcardRep = resource.getRepresentation('*/*')

        then:
        ['list','show']        == resource.getMethods()
        ['a','b']              == rep.marshallers*.instance
        [5,6]                  == rep.marshallers*.priority
        'DynamicJsonExtractor' == rep.extractor
        'custom'               == wildcardRep.extractor
    }

    def "Test getting a resource with a default resource config"() {
        setup:
        def src =
        {
            anyResource {
                representation {
                    mediaTypes = ['application/json']
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def resourceConfig = config.getResource( 'things' )

        then:
        null != resourceConfig
        resourceConfig instanceof ResourceConfig
    }

    def "Test getting a representation with a default resource config"() {
        setup:
        def src =
        {
            anyResource {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                        marshaller {
                            instance = 'b'
                            priority = 6
                        }
                    }
                    extractor = 'DynamicJsonExtractor'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def rep = config.getRepresentation( 'things', ['application/json'] )

        then:
        null      != rep
        ['a','b'] == rep.marshallers*.instance
    }

    def "Test that whitelisting overrides default resource config"() {
        setup:
        def src =
        {
            anyResource {
                representation {
                    mediaTypes = ['application/xml']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                    }
                    extractor = 'DynamicJsonExtractor'
                }
            }
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    serviceName = 'foo'
                    marshallers {
                        marshaller {
                            instance = 'local'
                        }
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def resource = config.getResource('things')
        def rep = config.getRepresentation( 'things', ['application/json'] )

        then:
        'foo'     == resource.serviceName
        ['local'] == rep.marshallers*.instance
    }

    def "Test that if a resource is whitelisted there is no fallback to default for representations"() {
        setup:
        def src =
        {
            anyResource {
                representation {
                    mediaTypes = ['application/xml']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                    }
                    extractor = 'DynamicJsonExtractor'
                }
            }
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        marshaller {
                            instance = 'local'
                        }
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def thingRep = config.getRepresentation( 'things', ['application/xml'] )
        def otherThingRep = config.getRepresentation( 'otherThing', ['application/xml'] )

        then:
        null == thingRep
        null != otherThingRep
    }

    def "Test getting config for a non-whitelisted resource without default"() {
        setup:
        def src = {}

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        null == config.getResource( 'things' )
        null == config.getRepresentation('things', ['application/json'])
    }

    def "Test overriding marshaller framework for representation"() {
        setup:
        def src =
        {
            anyResource {
                representation {
                    mediaTypes = ['application/json']
                    marshallerFramework = 'custom'
                }
            }
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallerFramework = 'custom'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        'custom' == config.getRepresentation("dummy",'application/json').marshallerFramework
        'custom' == config.getRepresentation("things",'application/json').marshallerFramework
    }

    def "Test overriding content type for representation"() {
        setup:
        def src =
        {
            anyResource {
                representation {
                    mediaTypes = ['application/json']
                    contentType = 'application/custom'
                }
            }
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    contentType = 'application/custom'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        'application/custom' == config.getRepresentation("dummy",'application/json').contentType
        'application/custom' == config.getRepresentation("things",'application/json').contentType
    }

    def "Test grailsApplication is available in resource marshallers"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        marshaller {
                            instance = new net.hedtech.restfulapi.marshallers.json.ThingClassMarshaller(grailsApplication)
                        }
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )

        then:
        config.validate()
    }

    def "Test grailsApplication is available in marshaller groups"() {
        setup:
        def src =
        {
            marshallerGroups {
                group 'test' marshallers {
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.json.ThingClassMarshaller(grailsApplication)
                        priority = 101
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )

        then:
        config.validate()
    }

    def "Test */* media type without explicit assignment returns first representation"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json','application/json']
                    marshallers {
                        marshaller {
                            instance = 'v0'
                            priority = 5
                        }
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaTypes = ['application/vnd.hedtech.v1+json']
                    marshallers {
                        marshaller {
                            instance = 'v1'
                            priority = 5
                        }
                    }
                    extractor = 'v1'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        RepresentationConfig conf = config.getRepresentation('things', '*/*' )

        then:
        null                              != conf
        'application/vnd.hedtech.v0+json' == conf.mediaType
        'v0'                              == conf.extractor
    }

    def "Test */* media type with explicit assignment returns correct representation"() {
        setup:
        def src =
        {
            resource 'things' config {
                anyMediaType = 'application/json'
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'v0'
                            priority = 5
                        }
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaTypes = ['application/vnd.hedtech.v1+json','application/json']
                    marshallers {
                        marshaller {
                            instance = 'v1'
                            priority = 5
                        }
                    }
                    extractor = 'v1'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        RepresentationConfig conf = config.getRepresentation('things', '*/*' )

        then:
        null               != conf
        'application/json' == conf.mediaType
        'v1'               == conf.extractor
    }

    def "Test exception when a resource declares a non-existent anyMediaType"() {
        setup:
        def src =
        {
            resource 'things' config {
                anyMediaType = 'application/json'
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'v0'
                            priority = 5
                        }
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaTypes = ['application/vnd.hedtech.v1+json']
                    marshallers {
                        marshaller {
                            instance = 'v1'
                            priority = 5
                        }
                    }
                    extractor = 'v1'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        MissingAnyMediaType e = thrown()
        'things'           == e.resourceName
        'application/json' == e.mediaType
    }

    def "Test jsonArrayPrefix prefix"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    jsonArrayPrefix = 'abc'
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )

        then:
        'abc' == config.getRepresentation('things', 'application/vnd.hedtech.v0+json').jsonArrayPrefix
    }

    def "Test jsonArrayPrefix on anyResource"() {
        setup:
        def src =
        {
            anyResource {
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    jsonArrayPrefix = 'abc'
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )

        then:
        'abc' == config.getRepresentation('', 'application/vnd.hedtech.v0+json').jsonArrayPrefix
    }

    def "Test exception handler configuration"() {
        setup:
        def src =
        {
            exceptionHandlers {
                handler {
                    instance = 'foo'
                }
                handler {
                    instance = 'bar'
                    priority = 2
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        ['foo', 'bar'] == config.exceptionHandlers*.instance
        [0,2]          == config.exceptionHandlers*.priority
    }

    def "Test id match configuration"() {
        setup:
        def src =
        {
            resource 'things' config {
                idMatchEnforced = false
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

            resource 'foos' config {
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )

        then:
        false == config.resources['things'].idMatchEnforced
        true  == config.resources['foos'].idMatchEnforced
    }

    def "Test body extraction on delete"() {
        setup:
        def src =
        {
            resource 'things' config {
                bodyExtractedOnDelete = true
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

            resource 'foos' config {
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+json']
                    marshallers {
                        marshaller {
                            instance = 'a'
                            priority = 5
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )

        then:
        true == config.resources['things'].bodyExtractedOnDelete
        false  == config.resources['foos'].bodyExtractedOnDelete
    }

    def "Test unsupported media type methods"() {
        setup:
        def src =
                {
                    resource 'things' config {
                        methods = ['list','show','create','update','delete']
                        unsupportedMediaTypeMethods = ['application/vnd.hedtech.v0+json': ['create','update','delete'],
                                                       'application/vnd.hedtech.v1+json': ['delete']]
                        representation {
                            mediaTypes = ['application/vnd.hedtech.v0+json',
                                          'application/vnd.hedtech.v1+json',
                                          'application/vnd.hedtech.v2+json']
                            marshallers {
                                marshaller {
                                    instance = 'a'
                                    priority = 5
                                }
                                marshaller {
                                    instance = 'b'
                                    priority = 6
                                }
                            }
                            extractor = 'net.hedtech.DynamicJsonExtractor'
                        }
                    }

                }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        ['list','show','create','update','delete'] == config.getResource( 'things' ).getMethods()
        ['create','update','delete'] == config.getResource( 'things' ).getUnsupportedMediaTypeMethods().get( 'application/vnd.hedtech.v0+json' )
        ['delete'] == config.getResource( 'things' ).getUnsupportedMediaTypeMethods().get( 'application/vnd.hedtech.v1+json' )
        null == config.getResource( 'things' ).getUnsupportedMediaTypeMethods().get( 'application/vnd.hedtech.v2+json' )
    }

    def "Test unsupported media type methods invalid method name"() {
        setup:
        def src =
                {
                    resource 'things' config {
                        methods = ['list','show','create','update','delete']
                        unsupportedMediaTypeMethods = ['application/vnd.hedtech.v0+json': ['create','update','delete'],
                                                       'application/vnd.hedtech.v1+json': ['foo']]
                        representation {
                            mediaTypes = ['application/vnd.hedtech.v0+json',
                                          'application/vnd.hedtech.v1+json',
                                          'application/vnd.hedtech.v2+json']
                            marshallers {
                                marshaller {
                                    instance = 'a'
                                    priority = 5
                                }
                                marshaller {
                                    instance = 'b'
                                    priority = 6
                                }
                            }
                            extractor = 'net.hedtech.DynamicJsonExtractor'
                        }
                    }

                }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        def e = thrown(UnknownMediaTypeMethodException)
        'things' == e.resourceName
        'application/vnd.hedtech.v1+json' == e.mediaType
        'foo' == e.methodName
    }

    def "Test exception when unsupported media type methods on resource is not a map"() {
        setup:
        def src =
                {
                    resource 'things' config {
                        methods = ['list','show','create','update','delete']
                        unsupportedMediaTypeMethods = 'application/vnd.hedtech.v1+json'
                        representation {
                            mediaTypes = ['application/vnd.hedtech.v0+json',
                                          'application/vnd.hedtech.v1+json',
                                          'application/vnd.hedtech.v2+json']
                            marshallers {
                                marshaller {
                                    instance = 'a'
                                    priority = 5
                                }
                                marshaller {
                                    instance = 'b'
                                    priority = 6
                                }
                            }
                            extractor = 'net.hedtech.DynamicJsonExtractor'
                        }
                    }

                }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        def e = thrown(UnsupportedMediaTypeMethodsNotMapException)
        'things' == e.resourceName
    }

    def "Test exception when unsupported media type methods on resource is not a map of collection"() {
        setup:
        def src =
                {
                    resource 'things' config {
                        methods = ['list','show','create','update','delete']
                        unsupportedMediaTypeMethods = ['application/vnd.hedtech.v1+json': 'foo']
                        representation {
                            mediaTypes = ['application/vnd.hedtech.v0+json',
                                          'application/vnd.hedtech.v1+json',
                                          'application/vnd.hedtech.v2+json']
                            marshallers {
                                marshaller {
                                    instance = 'a'
                                    priority = 5
                                }
                                marshaller {
                                    instance = 'b'
                                    priority = 6
                                }
                            }
                            extractor = 'net.hedtech.DynamicJsonExtractor'
                        }
                    }

                }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        def e = thrown(UnsupportedMediaTypeMethodsMapValueNotCollectionException)
        'things' == e.resourceName
        'application/vnd.hedtech.v1+json' == e.mediaType
    }

    def "Test unsupported media type methods unknown media type"() {
        setup:
        def src =
                {
                    resource 'things' config {
                        methods = ['list','show','create','update','delete']
                        unsupportedMediaTypeMethods = ['application/vnd.hedtech.v0+json': ['create','update','delete'],
                                                       'application/vnd.hedtech.v0.5+json': ['create','update','delete'],
                                                       'application/vnd.hedtech.v1+json': ['delete']]
                        representation {
                            mediaTypes = ['application/vnd.hedtech.v0+json',
                                          'application/vnd.hedtech.v1+json',
                                          'application/vnd.hedtech.v2+json']
                            marshallers {
                                marshaller {
                                    instance = 'a'
                                    priority = 5
                                }
                                marshaller {
                                    instance = 'b'
                                    priority = 6
                                }
                            }
                            extractor = 'net.hedtech.DynamicJsonExtractor'
                        }
                    }

                }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        def e = thrown(UnsupportedMediaTypeMethodsUnknownMediaTypeException)
        'things' == e.resourceName
        'application/vnd.hedtech.v0.5+json' == e.mediaType
    }

    def "Test resource metadata"() {
        setup:
        def src =
                {
                    resource 'things' config {
                        resourceMetadata = [title: "My Things", authoritative: true]
                        methods = ['list','show','create','update','delete']
                        representation {
                            mediaTypes = ['application/vnd.hedtech.v0+json',
                                          'application/vnd.hedtech.v1+json',
                                          'application/vnd.hedtech.v2+json']
                            marshallers {
                                marshaller {
                                    instance = 'a'
                                    priority = 5
                                }
                                marshaller {
                                    instance = 'b'
                                    priority = 6
                                }
                            }
                            extractor = 'net.hedtech.DynamicJsonExtractor'
                        }
                    }

                }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        "My Things" == config.getResource( 'things' ).getResourceMetadata().get( 'title' )
        true == config.getResource( 'things' ).getResourceMetadata().get( 'authoritative' )
    }

    def "Test exception when resource metadata is not a map"() {
        setup:
        def src =
                {
                    resource 'things' config {
                        resourceMetadata = "My Things"
                        methods = ['list','show','create','update','delete']
                        representation {
                            mediaTypes = ['application/vnd.hedtech.v0+json',
                                          'application/vnd.hedtech.v1+json',
                                          'application/vnd.hedtech.v2+json']
                            marshallers {
                                marshaller {
                                    instance = 'a'
                                    priority = 5
                                }
                                marshaller {
                                    instance = 'b'
                                    priority = 6
                                }
                            }
                            extractor = 'net.hedtech.DynamicJsonExtractor'
                        }
                    }

                }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        def e = thrown(ResourceMetadataNotMapException)
        'things' == e.resourceName
    }

    def "Test representation metadata"() {
        setup:
        def src =
                {
                    resource 'things' config {
                        methods = ['list','show','create','update','delete']
                        representation {
                            representationMetadata = [filters: ["filter1", "filter2"]]
                            mediaTypes = ['application/vnd.hedtech.v0+json',
                                          'application/vnd.hedtech.v1+json',
                                          'application/vnd.hedtech.v2+json']
                            marshallers {
                                marshaller {
                                    instance = 'a'
                                    priority = 5
                                }
                                marshaller {
                                    instance = 'b'
                                    priority = 6
                                }
                            }
                            extractor = 'net.hedtech.DynamicJsonExtractor'
                        }
                    }

                }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        def representations = config.getResource( 'things' ).representations
        3 == representations.size()
        [filters: ["filter1", "filter2"]] == representations.get('application/vnd.hedtech.v0+json').getRepresentationMetadata()
        [filters: ["filter1", "filter2"]] == representations.get('application/vnd.hedtech.v1+json').getRepresentationMetadata()
        [filters: ["filter1", "filter2"]] == representations.get('application/vnd.hedtech.v2+json').getRepresentationMetadata()
    }

    def "Test exception when representation metadata is not a map"() {
        setup:
        def src =
                {
                    resource 'things' config {
                        methods = ['list','show','create','update','delete']
                        representation {
                            representationMetadata = "My Stuff"
                            mediaTypes = ['application/vnd.hedtech.v0+json',
                                          'application/vnd.hedtech.v1+json',
                                          'application/vnd.hedtech.v2+json']
                            marshallers {
                                marshaller {
                                    instance = 'a'
                                    priority = 5
                                }
                                marshaller {
                                    instance = 'b'
                                    priority = 6
                                }
                            }
                            extractor = 'net.hedtech.DynamicJsonExtractor'
                        }
                    }

                }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        def e = thrown(RepresentationMetadataNotMapException)
        'things' == e.resourceName
        'application/vnd.hedtech.v0+json' == e.mediaType
    }

    def "Test service name override by representation service name"() {
        setup:
        def src =
                {
                    resource 'things' config {
                        serviceName = 'TheThingService'
                        representation {
                            representationServiceName = 'TheThingRepresentationService'
                            mediaTypes = ['application/vnd.hedtech.v0+json']
                            marshallers {

                            }
                            extractor = null
                        }
                    }

                }

        when:
        def config = RestConfig.parse( grailsApplication, src )

        then:
        1 == config.resources.size()
        'TheThingRepresentationService' == config.resources['things'].representations.get('application/vnd.hedtech.v0+json').representationServiceName
    }
}
