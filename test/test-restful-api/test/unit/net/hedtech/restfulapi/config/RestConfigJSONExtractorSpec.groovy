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
class RestConfigJSONExtractorSpec extends Specification {

    def "Test json extractor template parsing"() {
        setup:
        def src =
        {
            jsonExtractorTemplates {
                template 'one' config {
                }

                template 'two' config {
                    inherits = ['one']
                    property 'person.name' name 'bar'
                    property 'person.address' flatObject true
                    property 'person.customer' shortObject true
                    property 'lastName' defaultValue 'Smith'
                    shortObject { def v -> 'short' }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def mConfig = config.jsonExtractor.configs['two']
        def shortObject = mConfig.shortObjectClosure.call([:])

        then:
         2                     == config.jsonExtractor.configs.size()
         ['person.name':'bar'] == mConfig.dottedRenamedPaths
         ['person.address']    == mConfig.dottedFlattenedPaths
         ['person.customer']   == mConfig.dottedShortObjectPaths
         ['lastName':'Smith']  == mConfig.dottedValuePaths
         'short'               == shortObject
    }

    def "Test json extractor creation"() {
        setup:
        def src =
        {
            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    jsonExtractor {
                        property 'person.name' name 'bar'
                        property 'person.address' flatObject true
                        property 'person.customer' shortObject true
                        property 'lastName' defaultValue 'Smith'
                        shortObject { def v -> 'short' }
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def extractor = config.getRepresentation( 'things', 'application/json' ).extractor
        def shortObject = extractor.shortObjectClosure.call([:])

        then:
         ['person.name':'bar'] == extractor.dottedRenamedPaths
         ['person.address']    == extractor.dottedFlattenedPaths
         ['person.customer']   == extractor.dottedShortObjectPaths
         ['lastName':'Smith']  == extractor.dottedValuePaths
         'short'               == shortObject
    }

    def "Test json extractor creation from merged configuration"() {
        setup:
        def src =
        {
            jsonExtractorTemplates {
                template 'one' config {
                    property 'one' name 'modOne'
                }

                template 'two' config {
                    property 'two' name 'modTwo'
                }
            }

            resource 'things' config {
                representation {
                    mediaTypes = ['application/json']
                    jsonExtractor {
                        inherits = ['one','two']
                        property 'three' name 'modThree'
                    }
                }
            }
        }

        when:
        def config = RestConfig.parse( grailsApplication, src )
        config.validate()
        def extractor = config.getRepresentation( 'things', 'application/json' ).extractor

        then:
        ['one':'modOne','two':'modTwo','three':'modThree'] == extractor.dottedRenamedPaths
    }

}