/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import grails.test.mixin.*
import spock.lang.*

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.xml.*
import net.hedtech.restfulapi.*
import grails.test.mixin.support.*
import net.hedtech.restfulapi.marshallers.xml.*
import net.hedtech.restfulapi.beans.*


@TestMixin(GrailsUnitTestMixin)
class RestConfigXMLGroovyBeanMarshallerSpec extends Specification {

    def "Test xml groovy bean marshaller in marshaller group"() {
        setup:
        def src =
        {
            marshallerGroups {
                group 'groovyBean' marshallers {
                    xmlGroovyBeanMarshaller {
                        supports SimpleBean
                        field 'foo' name 'bar'
                        includesFields {
                            field 'bar' name 'customBar'
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
        def marshaller = config.marshallerGroups['groovyBean'].marshallers[0].instance

        then:
        SimpleBean                                == marshaller.supportClass
        ['foo':'bar','bar':'customBar']           == marshaller.fieldNames
        ['bar']                                   == marshaller.includedFields
        ['foobar']                                == marshaller.excludedFields
    }

    def "Test xml groovy bean marshaller template parsing"() {
        setup:
        def src =
        {
            xmlGroovyBeanMarshallerTemplates {
                template 'one' config {
                }

                template 'two' config {
                    inherits = ['one']
                    priority = 5
                    supports SimpleBean
                    field 'foo' name 'bar'
                    field 'f1'
                    field 'f2'
                    includesFields {
                        field 'foo' name 'foobar'
                    }
                    excludesFields {
                        field 'bar'
                    }
                    additionalFields {->}
                    additionalFieldsMap = ['a':'b','c':'d']
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def mConfig = config.xmlGroovyBean.configs['two']

        then:
         2                     == config.xmlGroovyBean.configs.size()
         ['one']               == mConfig.inherits
         5                     == mConfig.priority
         SimpleBean            == mConfig.supportClass
         ['foo':'foobar']      == mConfig.fieldNames
         ['foo']               == mConfig.includedFields
         ['bar']               == mConfig.excludedFields
         1                     == mConfig.additionalFieldClosures.size()
         ['a':'b','c':'d']     == mConfig.additionalFieldsMap
    }

    def "Test xml groovy bean marshaller creation"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/xml']
                    marshallers {
                        xmlGroovyBeanMarshaller {
                            supports SimpleBean
                            field 'owner' name 'myOwner'
                            includesFields {
                                field 'code' name 'productCode'
                                field 'parts'
                            }
                            excludesFields {
                                field 'description'
                            }
                            additionalFields {Map m ->}
                            additionalFieldsMap = ['foo':'bar']
                        }
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def marshaller = config.getRepresentation( 'things', 'application/xml' ).marshallers[0].instance

        then:
        SimpleBean                               == marshaller.supportClass
        ['owner':'myOwner','code':'productCode'] == marshaller.fieldNames
        ['code','parts']                         == marshaller.includedFields
        ['description']                          == marshaller.excludedFields
        1                                        == marshaller.additionalFieldClosures.size()
        ['foo':'bar']                            == marshaller.additionalFieldsMap
    }

    def "Test xml groovy bean marshaller creation from merged configuration"() {
        setup:
        def src =
        {
            xmlGroovyBeanMarshallerTemplates {
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
                    mediaTypes = ['application/xml']
                    marshallers {
                        xmlGroovyBeanMarshaller {
                            inherits = ['one','two']
                            supports Thing
                            includesFields {
                                field 'code'
                                field 'description'
                            }
                        }
                    }
                    extractor = 'extractor'
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def marshaller = config.getRepresentation( 'things', 'application/xml' ).marshallers[0].instance

        then:
        ['field1','field2','code','description'] == marshaller.includedFields
    }

}