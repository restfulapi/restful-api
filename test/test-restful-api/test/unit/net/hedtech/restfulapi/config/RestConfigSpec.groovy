/* ****************************************************************************
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

    def "Test jsonAsXml settings and defaults"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/vnd.hedtech.v0+xml']
                    jsonAsXml = true
                }
                representation {
                    mediaTypes = ['application/vnd.hedtech.v1+xml']
                    jsonAsXml = false
                    marshallers {
                        marshaller {
                            instance = 'v1'
                            priority = 5
                        }
                    }
                    extractor = 'v1'
                }
                representation {
                    mediaTypes = ['application/vnd.hedtech.v2+xml']
                    marshallers {
                        marshaller {
                            instance = 'v2'
                            priority = 5
                        }
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
                    jsonAsXml = true
                    extractor = new net.hedtech.restfulapi.extractors.xml.JSONObjectExtractor()
                }
                representation {
                    mediaTypes = ['application/custom-xml']
                }
                representation {
                    mediaTypes = ['application/custom-json-as-xml']
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
        null != conf
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
        'application/json' == defaultConfig.mediaType
        'v0' == defaultConfig.marshallers[0].instance
        'v0' == defaultConfig.extractor
        'application/vnd.hedtech.v0+json' == version0.mediaType
        'v0' == version0.marshallers[0].instance
        'v0' == version0.extractor
        'application/vnd.hedtech.v1+json' == version1.mediaType
        'v1' == version1.marshallers[0].instance
        'v1' == version1.extractor
    }

    def "Test detection when the same media type is assigned to two different representation"() {
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

    def "Test json domain marshaller in marshaller group"() {
        setup:
        def src =
        {
            marshallerGroups {
                group 'domain' marshallers {
                    jsonDomainMarshaller {
                        supports Thing
                        field 'foo' name 'bar' resource 'custom-foos'
                        includesFields {
                            field 'bar' name 'customBar' resource 'custom-bars'
                        }
                        excludesFields {
                            field 'foobar'
                        }
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        def marshaller = config.marshallerGroups['domain'].marshallers[0].instance

        then:
        Thing == marshaller.supportClass
        ['foo':'bar','bar':'customBar'] == marshaller.fieldNames
        ['foo':'custom-foos','bar':'custom-bars'] == marshaller.fieldResourceNames
        ['bar'] == marshaller.includedFields
        ['foobar'] == marshaller.excludedFields
    }

    def "Test xmlAsJSON missing equivalent json representation for marshaller"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/xml']
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


    def "Test domain marshaller support class"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonDomainMarshaller {
                            supports Thing
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

        then:
        DeclarativeDomainClassMarshaller.getName() == representation.marshallers[0].instance.getClass().getName()
        1     == representation.marshallers.size()
        100   == representation.marshallers[0].priority
        Thing == representation.marshallers[0].instance.supportClass
    }

    def "Test domain marshaller priority"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonDomainMarshaller {
                            supports Thing
                            priority = 90
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

        then:
        1  == representation.marshallers.size()
        90 == representation.marshallers[0].priority
    }

    def "Test domain marshaller fieldname substitutions"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonDomainMarshaller {
                            supports Thing
                            field 'code' name 'serialNumber'
                            field 'description' name 'desc'
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
        def marshaller = representation.marshallers[0].instance

        then:
        1                                            == representation.marshallers.size()
        ['code':'serialNumber','description':'desc'] == marshaller.fieldNames

    }

    def "Test domain marshaller includes"() {
       setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonDomainMarshaller {
                            supports Thing
                            includesFields {
                                field 'code'
                                field 'description'
                            }
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
        def marshaller = representation.marshallers[0].instance

        then:
        1                      == representation.marshallers.size()
        ['code','description'] == marshaller.includedFields
        [:]                    == marshaller.fieldNames
    }

    def "Test domain marshaller includes with substitutions"() {
       setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonDomainMarshaller {
                            supports Thing
                            includesFields {
                                field 'code'
                                field 'description' name 'desc'
                            }
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
        def marshaller = representation.marshallers[0].instance

        then:
        1                      == representation.marshallers.size()
        ['code','description'] == marshaller.includedFields
        ['description':'desc'] == marshaller.fieldNames
    }

    def "Test domain marshaller excludes"() {
       setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonDomainMarshaller {
                            supports Thing
                            excludesFields {
                                field 'dateManufactured'
                                field 'isGood'
                            }
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
        def marshaller = representation.marshallers[0].instance

        then:
        1                             == representation.marshallers.size()
        ['dateManufactured','isGood'] == marshaller.excludedFields
    }

    def "Test domain marshaller exclude id and version"() {
       setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonDomainMarshaller {
                            includesId false
                            includesVersion false
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
        def marshaller = representation.marshallers[0].instance

        then:
        false == marshaller.includeId
        false == marshaller.includeVersion
    }

    def "Test additional field closures"() {
       setup:
       int counter = 0
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonDomainMarshaller {
                            additionalFields { Map m ->
                                counter++
                            }
                            additionalFields { Map m ->
                                counter++
                            }
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
        def marshaller = representation.marshallers[0].instance
        marshaller.additionalFieldClosures.each { it.call( [:] ) }

        then:
        2 == marshaller.additionalFieldClosures.size()
        2 == counter
    }

    def "Test json domain marshaller template parsing"() {
        setup:
        def src =
        {
            jsonDomainMarshallerTemplates {
                template 'one' config {
                }

                template 'two' config {
                    inherits = ['one']
                    priority = 5
                    supports Thing
                    field 'foo' name 'bar'
                    field 'f1' resource 'r1'
                    field 'f2' resource 'r2'
                    includesFields {
                        field 'foo' name 'foobar'
                    }
                    excludesFields {
                        field 'bar'
                    }
                    additionalFields {->}
                    additionalFieldsMap = ['a':'b','c':'d']
                    shortObject {Map m -> return 'foo'}
                    includesId false
                    includesVersion false
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def mConfig = config.jsonDomain.configs['two']
        def shortObject = mConfig.shortObjectClosure.call([:])

        then:
         2                     == config.jsonDomain.configs.size()
         ['one']               == mConfig.inherits
         5                     == mConfig.priority
         Thing                 == mConfig.supportClass
         ['foo':'foobar']      == mConfig.fieldNames
         ['foo']               == mConfig.includedFields
         ['bar']               == mConfig.excludedFields
         1                     == mConfig.additionalFieldClosures.size()
         ['a':'b','c':'d']     == mConfig.additionalFieldsMap
         ['f1':'r1','f2':'r2'] == mConfig.fieldResourceNames
         'foo'                 == shortObject
         false                 == mConfig.includeId
         false                 == mConfig.includeVersion
    }

    def "Test json domain marshaller creation"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonDomainMarshaller {
                            supports Thing
                            field 'owner' resource 't-owners'
                            includesFields {
                                field 'code' name 'productCode'
                                field 'parts' resource 't-parts'
                            }
                            excludesFields {
                                field 'description'
                            }
                            includesId      false
                            includesVersion false
                            additionalFields {Map m ->}
                            shortObject {Map m -> return 'foo'}
                            additionalFieldsMap = ['foo':'bar']
                        }
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def marshaller = config.getRepresentation( 'things', 'application/json' ).marshallers[0].instance
        def shortObject = marshaller.shortObjectClosure.call([:])

        then:
        Thing                                  == marshaller.supportClass
        ['code':'productCode']                 == marshaller.fieldNames
        ['code','parts']                       == marshaller.includedFields
        ['description']                        == marshaller.excludedFields
        false                                  == marshaller.includeId
        false                                  == marshaller.includeVersion
        1                                      == marshaller.additionalFieldClosures.size()
        ['foo':'bar']                          == marshaller.additionalFieldsMap
        ['owner':'t-owners','parts':'t-parts'] == marshaller.fieldResourceNames
        'foo'                                  == shortObject
    }

    def "Test json domain marshaller creation from merged configuration"() {
        setup:
        def src =
        {
            jsonDomainMarshallerTemplates {
                template 'one' config {
                    includesFields {
                        field 'field1'
                    }
                }

                template 'two' config {
                    includesFields {
                        field 'field2'
                    }
                }
            }

            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonDomainMarshaller {
                            inherits = ['one','two']
                            supports Thing
                            includesFields {
                                field 'code'
                                field 'description'
                            }
                        }
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def marshaller = config.getRepresentation( 'things', 'application/json' ).marshallers[0].instance

        then:
        ['field1','field2','code','description'] == marshaller.includedFields
    }

}