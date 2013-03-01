/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import grails.test.mixin.*
import spock.lang.*

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*
import net.hedtech.restfulapi.*

@TestFor(RestConfigService)
class RESTConfigServiceSpec extends Specification {


    def setup() {
        config.restfulApiConfig = null
    }

    def cleanup() {
        config.restfulApiConfig = null
    }

    def "Test simple configuration for one media type"() {
        setup:
        def src =
        {
            resource {
                name = 'things'
                representation {
                    mediaType = 'application/vnd.hedtech.v0+json'
                    marshaller {
                        marshaller = 'a'
                        priority = 5
                    }
                    marshaller {
                        marshaller = 'b'
                        priority = 6
                    }
                    extractor = 'net.hedtech.DynamicJsonExtractor'
                }
            }

        }

        when:
        def config = RESTConfig.parse( grailsApplication, src )

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
                    marshaller {
                        marshaller = 'v0'
                        priority = 5
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaType = 'application/vnd.hedtech.v1+json'
                    marshaller {
                        marshaller = 'v1'
                        priority = 5
                    }
                    extractor = 'v1'
                }
            }
        }

        when:
        def conf = RESTConfig.parse( grailsApplication, src )

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
                    marshaller {
                        marshaller = 'v1'
                        priority = 5
                    }
                    extractor = 'v1'
                }
                representation {
                    mediaType = 'application/vnd.hedtech.v2+xml'
                    marshaller {
                        marshaller = 'v2'
                        priority = 5
                    }
                    extractor = 'v2'
                }

            }
        }

        when:
        def conf = RESTConfig.parse( grailsApplication, src )

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
                    marshaller {
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
        def conf = RESTConfig.parse( grailsApplication, src )

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
                marshaller {
                    marshaller = "xml1"
                    priority 5
                }
                marshaller {
                    marshaller = "xml2"
                    priority = 6
                }
                extractor = "extractor"
            }
        }

        when:
        def conf = RESTConfig.parse( grailsApplication, src )

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
                    marshaller {
                        marshaller = 'v0'
                        priority = 5
                    }
                    extractor = 'v0'
                }
                representation {
                    mediaType = 'application/vnd.hedtech.v1+json'
                    marshaller {
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
        def config = RESTConfig.parse( grailsApplication, src )
        RepresentationConfig conf = config.getRepresentation('things', types )

        then:
        null != conf
        'application/vnd.hedtech.v1+json' == conf.mediaType
    }
}