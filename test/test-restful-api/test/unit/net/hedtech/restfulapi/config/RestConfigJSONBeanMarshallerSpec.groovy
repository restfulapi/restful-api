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
import net.hedtech.restfulapi.beans.*
import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*
import net.hedtech.restfulapi.marshallers.json.*

import spock.lang.*


@TestMixin(GrailsUnitTestMixin)
class RestConfigJSONBeanMarshallerSpec extends Specification {

    def "Test json bean marshaller in marshaller group"() {
        setup:
        def src =
        {
            marshallerGroups {
                group 'jsonBean' marshallers {
                    jsonBeanMarshaller {
                        supports SimpleBean
                        marshallsNullFields false
                        field 'foo' name 'bar' marshallsNull false
                        includesFields {
                            field 'bar' name 'customBar' marshallsNull true
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
        def marshaller = config.marshallerGroups['jsonBean'].marshallers[0].instance

        then:
        SimpleBean                                == marshaller.supportClass
        ['foo':'bar','bar':'customBar']           == marshaller.fieldNames
        ['bar']                                   == marshaller.includedFields
        ['foobar']                                == marshaller.excludedFields
        false                                     == marshaller.marshallNullFields
        [foo:false,bar:true]                      == marshaller.marshalledNullFields
    }

    def "Test json bean marshaller template parsing"() {
        setup:
        def src =
        {
            jsonBeanMarshallerTemplates {
                template 'one' config {
                }

                template 'two' config {
                    inherits = ['one']
                    priority = 5
                    supports SimpleBean
                    requiresIncludedFields true
                    marshallsNullFields false
                    field 'foo' name 'bar'
                    field 'f1'
                    field 'f2' marshallsNull true
                    includesFields {
                        field 'foo' name 'foobar' marshallsNull false
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
        def mConfig = config.jsonBean.configs['two']

        then:
         2                      == config.jsonBean.configs.size()
         ['one']                == mConfig.inherits
         5                      == mConfig.priority
         SimpleBean             == mConfig.supportClass
         true                   == mConfig.requireIncludedFields
         ['foo':'foobar']       == mConfig.fieldNames
         ['foo']                == mConfig.includedFields
         true                   == mConfig.useIncludedFields
         ['bar']                == mConfig.excludedFields
         1                      == mConfig.additionalFieldClosures.size()
         ['a':'b','c':'d']      == mConfig.additionalFieldsMap
         false                  == mConfig.marshallNullFields
         ['f2':true,'foo':false] == mConfig.marshalledNullFields
    }

    def "Test json bean marshaller creation"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    marshallers {
                        jsonBeanMarshaller {
                            supports SimpleBean
                            marshallsNullFields false
                            field 'owner' name 'myOwner' marshallsNull false
                            requiresIncludedFields true
                            includesFields {
                                field 'code' name 'productCode'
                                field 'parts' marshallsNull true
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
        def marshaller = config.getRepresentation( 'things', 'application/json' ).marshallers[0].instance

        then:
        SimpleBean                               == marshaller.supportClass
        ['owner':'myOwner','code':'productCode'] == marshaller.fieldNames
        true                                     == marshaller.requireIncludedFields
        ['code','parts']                         == marshaller.includedFields
        ['description']                          == marshaller.excludedFields
        1                                        == marshaller.additionalFieldClosures.size()
        ['foo':'bar']                            == marshaller.additionalFieldsMap
        false                                    == marshaller.marshallNullFields
        ['owner':false, 'parts':true]            == marshaller.marshalledNullFields
    }

    def "Test json bean marshaller creation from merged configuration"() {
        setup:
        def src =
        {
            jsonBeanMarshallerTemplates {
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
                        jsonBeanMarshaller {
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
