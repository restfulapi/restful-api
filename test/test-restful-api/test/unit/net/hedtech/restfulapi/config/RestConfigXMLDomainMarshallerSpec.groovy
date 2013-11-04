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

package net.hedtech.restfulapi.config

import grails.test.mixin.*
import grails.test.mixin.support.*

import net.hedtech.restfulapi.*
import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.xml.*
import net.hedtech.restfulapi.marshallers.xml.*

import spock.lang.*


@TestMixin(GrailsUnitTestMixin)
class RestConfigXMLDomainMarshallerSpec extends Specification {

    def "Test xml domain marshaller in marshaller group"() {
        setup:
        def src =
        {
            marshallerGroups {
                group 'domain' marshallers {
                    xmlDomainMarshaller {
                        supports Thing
                        elementName 'Thing'
                        deepMarshallsAssociations true
                        field 'foo' name 'bar' resource 'custom-foos' deep false
                        includesFields {
                            field 'bar' name 'customBar' resource 'custom-bars' deep true
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
        Thing                                     == marshaller.supportClass
        'Thing'                                   == marshaller.elementName
        ['foo':'bar','bar':'customBar']           == marshaller.fieldNames
        ['foo':'custom-foos','bar':'custom-bars'] == marshaller.fieldResourceNames
        ['bar']                                   == marshaller.includedFields
        ['foobar']                                == marshaller.excludedFields
        true                                      == marshaller.deepMarshallAssociations
        [foo:false,bar:true]                      == marshaller.deepMarshalledFields

    }

    def "Test xml domain marshaller template parsing"() {
        setup:
        def src =
        {
            xmlDomainMarshallerTemplates {
                template 'one' config {
                }

                template 'two' config {
                    inherits = ['one']
                    priority = 5
                    supports Thing
                    elementName 'Thing'
                    deepMarshallsAssociations true
                    requiresIncludedFields true
                    field 'foo' name 'bar'
                    field 'f1' resource 'r1'
                    field 'f2' resource 'r2'
                    field 'f3' deep true
                    includesFields {
                        field 'foo' name 'foobar'
                        field 'f4' deep false
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
        def mConfig = config.xmlDomain.configs['two']
        def shortObject = mConfig.shortObjectClosure.call([:])

        then:
        2                      == config.xmlDomain.configs.size()
        ['one']                == mConfig.inherits
        5                      == mConfig.priority
        Thing                  == mConfig.supportClass
        'Thing'                == mConfig.elementName
        true                   == mConfig.requireIncludedFields
        ['foo':'foobar']       == mConfig.fieldNames
        ['foo','f4']           == mConfig.includedFields
        true                   == mConfig.useIncludedFields
        ['bar']                == mConfig.excludedFields
        1                      == mConfig.additionalFieldClosures.size()
        ['a':'b','c':'d']      == mConfig.additionalFieldsMap
        ['f1':'r1','f2':'r2']  == mConfig.fieldResourceNames
        'foo'                  == shortObject
        false                  == mConfig.includeId
        false                  == mConfig.includeVersion
        true                   == mConfig.deepMarshallAssociations
        ['f3':true,'f4':false] == mConfig.deepMarshalledFields

    }

    def "Test xml domain marshaller creation"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/xml']
                    marshallers {
                        xmlDomainMarshaller {
                            supports Thing
                            elementName 'Thing'
                            requiresIncludedFields true
                            deepMarshallsAssociations true
                            field 'owner' resource 't-owners' deep false
                            includesFields {
                                field 'code' name 'productCode'
                                field 'parts' resource 't-parts' deep true
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
        def marshaller = config.getRepresentation( 'things', 'application/xml' ).marshallers[0].instance
        def shortObject = marshaller.shortObjectClosure.call([:])

        then:
        Thing                                  == marshaller.supportClass
        'Thing'                                == marshaller.elementName
        true                                   == marshaller.requireIncludedFields
        ['code':'productCode']                 == marshaller.fieldNames
        ['code','parts']                       == marshaller.includedFields
        ['description']                        == marshaller.excludedFields
        false                                  == marshaller.includeId
        false                                  == marshaller.includeVersion
        1                                      == marshaller.additionalFieldClosures.size()
        ['foo':'bar']                          == marshaller.additionalFieldsMap
        ['owner':'t-owners','parts':'t-parts'] == marshaller.fieldResourceNames
        'foo'                                  == shortObject
        true                                   == marshaller.deepMarshallAssociations
        ['owner':false, 'parts':true]          == marshaller.deepMarshalledFields

    }

    def "Test xml domain marshaller creation from merged configuration"() {
        setup:
        def src =
        {
            xmlDomainMarshallerTemplates {
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
                        xmlDomainMarshaller {
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
