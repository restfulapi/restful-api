/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import grails.test.mixin.*
import spock.lang.*

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*
import net.hedtech.restfulapi.*
import grails.test.mixin.support.*
import net.hedtech.restfulapi.marshallers.json.*

@TestMixin(GrailsUnitTestMixin)
class RestConfigSpec extends Specification {

    def "Test simple configuration for one media type"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/vnd.hedtech.v0+json'
                    addMarshaller {
                        marshaller = 'a'
                        priority = 5
                    }
                    addMarshaller {
                        marshaller = 'b'
                        priority = 6
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

    def "Test a resource with 2 representations defined"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/vnd.hedtech.v0+json'
                    addMarshaller {
                        marshaller = 'v0'
                        priority = 5
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaType = 'application/vnd.hedtech.v1+json'
                    addMarshaller {
                        marshaller = 'v1'
                        priority = 5
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

    def "Test jsonAsXml settings and defaults"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/vnd.hedtech.v0+xml'
                    jsonAsXml = true
                }
                representation {
                    mediaType = 'application/vnd.hedtech.v1+xml'
                    jsonAsXml = false
                    addMarshaller {
                        marshaller = 'v1'
                        priority = 5
                    }
                    extractor = 'v1'
                }
                representation {
                    mediaType = 'application/vnd.hedtech.v2+xml'
                    addMarshaller {
                        marshaller = 'v2'
                        priority = 5
                    }
                    extractor = 'v2'
                }

            }
        }

        when:
        def conf = RestConfig.parse( grailsApplication, src )

        then:
        1     == conf.resources.size()
        3     == conf.resources['things'].representations.size()
        true  == conf.resources['things'].representations['application/vnd.hedtech.v0+xml'].jsonAsXml
        false == conf.resources['things'].representations['application/vnd.hedtech.v1+xml'].jsonAsXml
        false == conf.resources['things'].representations['application/vnd.hedtech.v2+xml'].jsonAsXml
    }

    @Unroll
    def "Test resources missing extractors and marshallers"(String foo,String bar) {
        setup:
        def src = {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addMarshaller {
                        marshaller = 'foo'
                        priority = 5
                    }
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
                    mediaType = 'application/custom-json-as-xml'
                    jsonAsXml = true
                }
            }
        }

        when:
        def conf = RestConfig.parse( grailsApplication, src )

        then:
        1     == conf.resources.size()
        4     == conf.resources['things'].representations.size()
        conf.resources['things'].representations.values().each {
            null != it.mediaType
        }
        where:
        foo   | bar
        'foo' | 'bar'
    }

    def "Test global json as xml settings"() {
        setup:
        def src =
        {
            jsonAsXml {
                enableDefault = true
                addMarshaller {
                    marshaller = "xml1"
                    priority 5
                }
                addMarshaller {
                    marshaller = "xml2"
                    priority = 6
                }
                extractor = "extractor"
            }
        }

        when:
        def conf = RestConfig.parse( grailsApplication, src )

        then:
        null  != conf.jsonAsXml
        true  == conf.jsonAsXml.enableDefault
    }

    def "Test content negotiation for representation"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/vnd.hedtech.v0+json'
                    addMarshaller {
                        marshaller = 'v0'
                        priority = 5
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaType = 'application/vnd.hedtech.v1+json'
                    addMarshaller {
                        marshaller = 'v1'
                        priority = 5
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
        null != conf
        'application/vnd.hedtech.v1+json' == conf.mediaType
    }

    def "Test multiple media types assigned to representation"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/vnd.hedtech.v0+json'
                    mediaType = 'application/json'
                    addMarshaller {
                        marshaller = 'v0'
                        priority = 5
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaType = 'application/vnd.hedtech.v1+json'
                    addMarshaller {
                        marshaller = 'v1'
                        priority = 5
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
        'application/json' == defaultConfig.mediaType
        'v0' == defaultConfig.marshallers[0].marshaller
        'v0' == defaultConfig.extractor
        'application/vnd.hedtech.v0+json' == version0.mediaType
        'v0' == version0.marshallers[0].marshaller
        'v0' == version0.extractor
        'application/vnd.hedtech.v1+json' == version1.mediaType
        'v1' == version1.marshallers[0].marshaller
        'v1' == version1.extractor
    }

    def "Test detection when the same media type is assigned to two different representation"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addMarshaller {
                        marshaller = 'v0'
                        priority = 5
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaType = 'application/json'
                    addMarshaller {
                        marshaller = 'v1'
                        priority = 5
                    }
                    extractor = 'v1'
                }
            }
        }

        when:
        RestConfig.parse( grailsApplication, src )

        then:
        def e = thrown(AmbiguousRepresentationException)
        'things' == e.resourceName
        'application/json' == e.mediaType
    }

    def "Test marshaller groups"() {
        setup:
        def src =
        {
            marshallerGroup {
                name = 'defaultJSON'
                addMarshaller {
                    marshaller = 'defaultJsonDate'
                    priority = 5
                }
                addMarshaller {
                    marshaller = 'foo'
                    priority = 10
                }
            }
            marshallerGroup {
                name = 'thingDefaults'
                addMarshaller {
                    marshaller = 'defaultThing'
                    priority = 5
                }
            }
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addMarshaller {
                        marshaller = 'customThing'
                        priority = 15
                    }
                    addMarshaller getMarshallerGroup('defaultJSON')
                    addMarshaller {
                        marshaller = 'customDate'
                        priority = 15
                    }
                    addMarshaller getMarshallerGroup('thingDefaults')
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        RepresentationConfig rep = config.getRepresentation( 'things', 'application/json' )

        then:
        ['customThing','defaultJsonDate','foo','customDate','defaultThing'] == rep.marshallers.marshaller
        [15,5,10,15,5] == rep.marshallers.priority
    }

    def "Test missing marshaller group"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addMarshaller getMarshallerGroup('defaultJSON')
                }
            }
        }

        when:
        RestConfig.parse( grailsApplication, src )

        then:
        def e = thrown(MissingMarshallerGroupException)
        'defaultJSON' == e.name
    }

    def "Test xmlAsJSON missing equivalent json representation for marshaller"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/xml'
                    jsonAsXml = true
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()

        then:
        def e = thrown(MissingJSONEquivalent)
        'things' == e.resourceName
        'application/xml' == e.mediaType
    }

    def "Test service name override"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                serviceName = 'TheThingService'
                representation {
                    mediaType = 'application/vnd.hedtech.v0+json'
                    addMarshaller {
                        marshaller = 'a'
                        priority = 5
                    }
                    addMarshaller {
                        marshaller = 'b'
                        priority = 6
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

    def "Test method overrides"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                methods = ['list','show']
                representation {
                    mediaType = 'application/vnd.hedtech.v0+json'
                    addMarshaller {
                        marshaller = 'a'
                        priority = 5
                    }
                    addMarshaller {
                        marshaller = 'b'
                        priority = 6
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
            resource {
                name = 'things'
                methods = ['list','show','foo']
                representation {
                    mediaType = 'application/vnd.hedtech.v0+json'
                    addMarshaller {
                        marshaller = 'a'
                        priority = 5
                    }
                    addMarshaller {
                        marshaller = 'b'
                        priority = 6
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
            resource {
                name = 'things'
                methods = "list,show"
                representation {
                    mediaType = 'application/vnd.hedtech.v0+json'
                    addMarshaller {
                        marshaller = 'a'
                        priority = 5
                    }
                    addMarshaller {
                        marshaller = 'b'
                        priority = 6
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


    def "Test domain marshaller support class"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addJSONDomainMarshaller {
                        supportClass = Thing
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def representation = config.getResource('things').getRepresentation('application/json')

        then:
        DeclarativeDomainClassMarshaller.getName() == representation.marshallers[0].marshaller.getClass().getName()
        1     == representation.marshallers.size()
        100   == representation.marshallers[0].priority
        Thing == representation.marshallers[0].marshaller.supportClass
    }

    def "Test domain marshaller priority"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addJSONDomainMarshaller {
                        supportClass = Thing
                        priority = 90
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def representation = config.getResource('things').getRepresentation('application/json')

        then:
        1  == representation.marshallers.size()
        90 == representation.marshallers[0].priority
    }

    def "Test domain marshaller fieldname substitutions"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addJSONDomainMarshaller {
                        supportClass = Thing
                        field 'code' substitute 'serialNumber'
                        field 'description' substitute 'desc'
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def representation = config.getResource('things').getRepresentation('application/json')
        def marshaller = representation.marshallers[0].marshaller

        then:
        1                                            == representation.marshallers.size()
        ['code':'serialNumber','description':'desc'] == marshaller.substitutions

    }

    def "Test domain marshaller includes"() {
       setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addJSONDomainMarshaller {
                        supportClass = Thing
                        include {
                            field 'code'
                            field 'description'
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def representation = config.getResource('things').getRepresentation('application/json')
        def marshaller = representation.marshallers[0].marshaller

        then:
        1                      == representation.marshallers.size()
        ['code','description'] == marshaller.includedFields
        [:]                    == marshaller.substitutions
    }

    def "Test domain marshaller includes with substitutions"() {
       setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addJSONDomainMarshaller {
                        supportClass = Thing
                        include {
                            field 'code'
                            field 'description' substitute 'desc'
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def representation = config.getResource('things').getRepresentation('application/json')
        def marshaller = representation.marshallers[0].marshaller

        then:
        1                      == representation.marshallers.size()
        ['code','description'] == marshaller.includedFields
        ['description':'desc'] == marshaller.substitutions
    }

    def "Test domain marshaller excludes"() {
       setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addJSONDomainMarshaller {
                        supportClass = Thing
                        exclude {
                            field 'dateManufactured'
                            field 'isGood'
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def representation = config.getResource('things').getRepresentation('application/json')
        def marshaller = representation.marshallers[0].marshaller

        then:
        1                             == representation.marshallers.size()
        ['dateManufactured','isGood'] == marshaller.excludedFields
    }

    def "Test domain marshaller exclude id and version"() {
       setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addJSONDomainMarshaller {
                        includeId = false
                        includeVersion = false
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def representation = config.getResource('things').getRepresentation('application/json')
        def marshaller = representation.marshallers[0].marshaller

        then:
        false == marshaller.includeId
        false == marshaller.includeVersion
    }

    def "Test additional field closures"() {
       setup:
       int counter = 0
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addJSONDomainMarshaller {
                        additionalFields { app, bean, json ->
                            counter++
                        }
                        additionalFields { app, bean, json ->
                            counter++
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def representation = config.getResource('things').getRepresentation('application/json')
        def marshaller = representation.marshallers[0].marshaller
        marshaller.additionalFieldClosures.each { it.call( null, null, null ) }

        then:
        2 == marshaller.additionalFieldClosures.size()
        2 == counter
    }

    def "Test merging domain marshaller configurations"() {
        setup:
        DomainMarshallerConfig one = new DomainMarshallerConfig(
            supportClass:Thing,
            substitutions:['foo':'foo1','bar':'bar1'],
            includedFields:['foo','bar'],
            excludedFields:['e1','e2'],
            additionalFieldClosures:[{app,bean,json ->}],
            includeId:true,
            includeVersion:true
        )
        DomainMarshallerConfig two = new DomainMarshallerConfig(
            supportClass:PartOfThing,
            substitutions:['foo':'foo2','baz':'baz1'],
            includedFields:['baz'],
            excludedFields:['e3'],
            additionalFieldClosures:[{app,bean,json ->}],
            includeId:false,
            includeVersion:false
        )

        when:
        def config = one.merge(two)

        then:
        true                                     == one.isSupportClassSet
        true                                     == two.isSupportClassSet
        PartOfThing                              == two.supportClass
        ['foo':'foo2','bar':'bar1','baz':'baz1'] == config.substitutions
        ['foo','bar','baz']                      == config.includedFields
        ['e1','e2','e3']                         == config.excludedFields
        2                                        == config.additionalFieldClosures.size()
        false                                    == config.includeId
        false                                    == config.includeVersion
    }

    def "Test merging domain marshaller configurations does not alter either object"() {
        setup:
        DomainMarshallerConfig one = new DomainMarshallerConfig(
            supportClass:Thing,
            substitutions:['foo':'foo1','bar':'bar1'],
            includedFields:['foo','bar'],
            excludedFields:['e1','e2'],
            additionalFieldClosures:[{app,bean,json ->}],
            includeId:true,
            includeVersion:true
        )
        DomainMarshallerConfig two = new DomainMarshallerConfig(
            supportClass:PartOfThing,
            substitutions:['foo':'foo2','baz':'baz1'],
            includedFields:['baz'],
            excludedFields:['e3'],
            additionalFieldClosures:[{app,bean,json ->}],
            includeId:false,
            includeVersion:false
        )

        when:
        one.merge(two)

        then:
        true                        == one.isSupportClassSet
        ['foo':'foo1','bar':'bar1'] == one.substitutions
        ['foo','bar']               == one.includedFields
        ['e1','e2']                 == one.excludedFields
        1                           == one.additionalFieldClosures.size()
        true                        == one.includeId
        true                        == one.includeVersion

        true                        == two.isSupportClassSet
        ['foo':'foo2','baz':'baz1'] == two.substitutions
        ['baz']                     == two.includedFields
        ['e3']                      == two.excludedFields
        1                           == two.additionalFieldClosures.size()
        false                       == two.includeId
        false                       == two.includeVersion

    }

    def "Test resolution of domain marshaller configuration includes"() {
        setup:
        DomainMarshallerConfig part1 = new DomainMarshallerConfig(
        )
        DomainMarshallerConfig part2 = new DomainMarshallerConfig(
        )
        DomainMarshallerConfig part3 = new DomainMarshallerConfig(
        )
        DomainMarshallerConfig combined = new DomainMarshallerConfig(
            includes:['part1','part2']
        )
        DomainMarshallerConfig actual = new DomainMarshallerConfig(
            includes:['combined','part3']
        )
        ConfigGroup group = new ConfigGroup()
        group.configs = ['part1':part1,'part2':part2,'part3':part3,'combined':combined]

        when:
        def resolvedList = group.getIncludesChain( actual )

        then:
        [part1,part2,combined,part3,actual] == resolvedList
    }

    def "Test merge order of domain marshaller configuration includes"() {
        setup:
        DomainMarshallerConfig part1 = new DomainMarshallerConfig(
            substitutions:['1':'part1','2':'part1','3':'part1']
        )
        DomainMarshallerConfig part2 = new DomainMarshallerConfig(
            substitutions:['2':'part2','3':'part2']

        )
        DomainMarshallerConfig actual = new DomainMarshallerConfig(
            includes:['part1','part2'],
            substitutions:['3':'actual']
        )
        ConfigGroup group = new ConfigGroup()
        group.configs = ['part1':part1,'part2':part2]

        when:
        def config = group.getMergedConfig( actual )

        then:
        ['1':'part1','2':'part2','3':'actual'] == config.substitutions
    }

    def "Test json domain marshaller template parsing"() {
        setup:
        def src =
        {
            jsonDomainMarshaller 'one' params {
            }

            jsonDomainMarshaller 'two' params {
                includes = ['one']
                priority = 5
                supportClass = Thing
                substitutions = ['foo':'bar']
                includedFields = ['foo']
                excludedFields = ['bar']
                additionalFieldClosures = [{->}]
                includeId = false
                includeVersion = false
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def mConfig = config.jsonDomain.configs['two']

        then:
        2             == config.jsonDomain.configs.size()
        ['one']       == mConfig.includes
        5             == mConfig.priority
        Thing         == mConfig.supportClass
        ['foo':'bar'] == mConfig.substitutions
        ['foo']       == mConfig.includedFields
        ['bar']       == mConfig.excludedFields
        1             == mConfig.additionalFieldClosures.size()
        false         == mConfig.includeId
        false         == mConfig.includeVersion
    }

    def "Test json domain marshaller creation from merged configuration"() {
        setup:
        def src =
        {
            jsonDomainMarshaller 'one' params {
                includedFields = ['field1']
            }

            jsonDomainMarshaller 'two' params {
                includedFields = ['field2']
            }

            resource {
                name = 'things'
                representation {
                    mediaType = 'application/json'
                    addJSONDomainMarshaller {
                        includes = ['one','two']
                        supportClass = Thing
                        include {
                            field 'code'
                            field 'description'
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def marshaller = config.getRepresentation( 'things', 'application/json' ).marshallers[0].marshaller

        then:
        ['field1','field2','code','description'] == marshaller.includedFields
    }

}