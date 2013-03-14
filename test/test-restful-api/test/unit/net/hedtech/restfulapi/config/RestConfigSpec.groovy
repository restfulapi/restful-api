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
        foo | bar
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

}