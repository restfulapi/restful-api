/* ***************************************************************************
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
package net.hedtech.restfulapi.marshallers.xml

import grails.converters.XML
import grails.test.mixin.*
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.test.mixin.support.*
import grails.test.mixin.web.*

import net.hedtech.restfulapi.*

import org.apache.commons.lang.UnhandledException

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer

import org.junit.Rule
import org.junit.rules.TestName

import org.springframework.beans.BeanWrapper
import org.springframework.web.context.WebApplicationContext

import spock.lang.*


@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin,DomainClassUnitTestMixin])
@Mock([MarshalledThing,MarshalledPartOfThing,
       MarshalledSubPartOfThing,MarshalledThingContributor,
       MarshalledOwnerOfThing,MarshalledThingEmbeddedPart])
class XMLBasicDomainClassMarshallerSpec extends Specification {

    @Rule TestName testName = new TestName()

    void setup() {
    }

    def "Test with Id"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.includeIdFor << {Object o -> true }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.save()
        thing.id = 1

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        '1'  == xml.id.text()
        'AA' == xml.code.text()
        1    == xml.description.size()
    }

    def "Test without Id"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.includeIdFor << {Object o -> false }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0    == xml.id.size()
        'AA' == xml.code.text()
    }

    def "Test with version"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.includeVersionFor << {Object o -> true }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1
        thing.version = 1

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        '1'  == xml.version.text()
        'AA' == xml.code.text()
    }

    def "Test without version"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.includeVersionFor << {Object o -> false }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1
        thing.version = 1

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0    == xml.version.size()
        'AA' == xml.code.text()
    }

    def "Test excluding fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.getExcludedFields << {Object value-> ['description','isLarge'] }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0    == xml.description.size()
        0    == xml.isLarge.size()
        'AA' == xml.code.text()
    }

    def "Test default exclusions"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", lastModifiedBy:'John Smith' )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0    == xml.lastModifiedBy.size()
        null != thing.lastModifiedBy

    }

    def "Test including fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.getIncludedFields << {Object value-> [ 'code', 'description','parts'] }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'AA'       == xml.code.text()
        'aa thing' == xml.description.text()
        0 == xml.contributors.size()
        0 == xml.embeddedPart.size()
        0 == xml.owner.size()
        0 == xml.subPart.size()
        0 == xml.isLarge.size()
        0 == xml.lastModified.size()
        0 == xml.lastModifiedBy.size()
        0 == xml.dataOrigin.size()
    }

    def "Test including no fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.getIncludedFields << {Object value -> []}
        marshaller.metaClass.includeVersionFor << {Object value -> false}
        marshaller.metaClass.includeIdFor << {Object value -> false}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0 == xml.children().size()
    }

    def "Test require included fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.getIncludedFields << {Object value-> [ 'code','aFieldThatDoesNotExist', 'anotherFieldThatDoesNotExist'] }
        marshaller.metaClass.requireIncludedFields << { true }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'aa thing' )

        when:
        render( thing )

        then:
        Exception e = thrown()
        ['aFieldThatDoesNotExist', 'anotherFieldThatDoesNotExist'] == e.getCause().missingNames
    }


    def "Test that included fields overrides excluded fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.getIncludedFields << {Object value-> [ 'code', 'description','lastModifiedBy'] }
        marshaller.metaClass.getExcludedFields << {Object value->
            exclusionCalled = true
            [ 'code', 'description']
        }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", lastModifiedBy:'John Smith' )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'AA'         == xml.code.text()
        'aa thing'   == xml.description.text()
        'John Smith' == xml.lastModifiedBy.text()

    }

    def "Test special processing of simple fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.processField << {
            BeanWrapper beanWrapper, GrailsDomainClassProperty property, XML xml ->
            if (property.getName() == 'description') {
                xml.startNode('modDescription')
                xml.convertAnother(beanWrapper.getPropertyValue(property.getName()))
                xml.end()
                return false
            } else {
                return true
            }
        }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0          == xml.description.size()
        'aa thing' == xml.modDescription.text()
        'AA'       == xml.code.text()
    }

    def "Test alternative name of simple field"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.getSubstitutionName << {
            BeanWrapper beanWrapper, GrailsDomainClassProperty property ->
            if (property.getName() == 'description') {
                return 'modDescription'
            } else {
                return null
            }
        }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0          == xml.description.size()
        'aa thing' == xml.modDescription.text()
        'AA'       == xml.code.text()
    }

    def "Test that null Collection association field is marshalled with null attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.parts = null

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null   == thing.parts
        1      == xml.parts.size()
        0      == xml.parts.children().size()
        "true" == xml.parts.'@null'.text()
    }

    def "Test that null Collection association field is deep marshalled with null attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.deepMarshallAssociation << {BeanWrapper wrapper, GrailsDomainClassProperty property -> true}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.parts = null

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null   == thing.parts
        1      == xml.parts.size()
        0      == xml.parts.children().size()
        "true" == xml.parts.'@null'.text()
    }

    def "Test that empty Collection association field is marshalled as an empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.parts = []

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        []     == thing.parts
        1      == xml.parts.size()
        0      == xml.parts.children().size()
        "true" == xml.parts.'@array'.text()
    }

    def "Test that null Map association field is marshalled with null attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", contributors:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null   == thing.contributors
        1      == xml.contributors.size()
        0      == xml.contributors.children().size()
        "true" == xml.contributors.'@null'.text()
    }

    def "Test that null Map association field is deep marshalled with null attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.deepMarshallAssociation << {BeanWrapper wrapper, GrailsDomainClassProperty property -> true}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", contributors:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null   == thing.contributors
        1      == xml.contributors.size()
        0      == xml.contributors.children().size()
        "true" == xml.contributors.'@null'.text()
    }

    def "Test that empty Map association field is marshalled as an empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", contributors:[:] )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        [:]    == thing.contributors
        1      == xml.contributors.size()
        0      == xml.contributors.children().size()
        "true" == xml.contributors.'@map'.text()
    }

    def "Test that null one-to-one association field is marshalled as an empty node with null attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", subPart:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null   == thing.subPart
        1      == xml.subPart.size()
        0      == xml.subPart.children().size()
        'true' == xml.subPart.@null.text()
    }

    def "Test that null one-to-one association field is deep marshalled as an empty node with null attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.deepMarshallAssociation << {BeanWrapper wrapper, GrailsDomainClassProperty property -> true}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", subPart:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null   == thing.subPart
        1      == xml.subPart.size()
        0      == xml.subPart.children().size()
        'true' == xml.subPart.@null.text()
    }

    def "Test that null many-to-one association field is marshalled as an empty node with null attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", owner:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null   == thing.owner
        1      == xml.owner.size()
        0      == xml.owner.children().size()
        'true' == xml.owner.@null.text()
    }

    def "Test that null many-to-one association field is deep marshalled as an empty node with null attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.deepMarshallAssociation << {BeanWrapper wrapper, GrailsDomainClassProperty property -> true}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", owner:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null   == thing.owner
        1      == xml.owner.size()
        0      == xml.owner.children().size()
        'true' == xml.owner.@null.text()
    }

    def "Test that null embedded association field is marshalled as an empty node with null attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", embeddedPart:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        null   == thing.embeddedPart
        1      == xml.embeddedPart.size()
        0      == xml.embeddedPart.children().size()
        'true' == xml.embeddedPart.@null.text()
    }

    def "Test that associated collection carries the array attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        def parts = []
        def part = new MarshalledPartOfThing(code:'partA',description:'part A')
        part.setId(1)
        parts.add part
        thing.parts = parts

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.parts.'@array'.text()
    }

    def "Test that associated map carries the map attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledThingContributor contrib = new MarshalledThingContributor( firstName:'John', lastName:'Smith' )
        contrib.id = 5
        thing.contributors=['smith':contrib]


        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.contributors.'@map'.text()
    }

    def "Test that non-association collection carries the array attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.simpleArray.'@array'.text()
    }

    def "Test that null non-association map marshalls with null attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.simpleMap = null

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.simpleMap.'@null'.text()
        0      == xml.simpleMap.children().size()
    }

    def "Test that empty non-association map marshalls as empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.simpleMap = [:]

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.simpleMap.'@map'.text()
        0      == xml.simpleMap.children().size()
    }

    def "Test non-association map"() {
        setup:
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.simpleMap = ['key 1':'value 1']

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true"    == xml.simpleMap.'@map'.text()
        1         == xml.simpleMap.children().size()
        'key 1'   == xml.simpleMap.entry[0].@key.text()
        'value 1' == xml.simpleMap.entry[0].text()
    }


    def "Test that null non-association collection marshalls with null attribute"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.simpleArray = null

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.simpleArray.'@null'.text()
        0      == xml.simpleArray.children().size()
    }

    def "Test that empty non-association collection marshalls as empty node"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.simpleArray = []

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        "true" == xml.simpleArray.'@array'.text()
        0      == xml.simpleArray.children().size()
    }

    def "Test non-association collection"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.simpleArray = ['abc','123']

        when:
        def content = render( thing )
        def xml = XML.parse content
        def values = []
        xml.simpleArray.children().each() {
            values.add it.text()
        }

        then:
        "true"        == xml.simpleArray.'@array'.text()
        2             == xml.simpleArray.children().size()
        ['abc','123'] == values
    }

    def "Test null simple property"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication,

        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:null )
        thing.simpleArray = ['abc','123']

        when:
        def content = render( thing )
        def xml = XML.parse content
        def values = []
        xml.simpleArray.children().each() {
            values.add it.text()
        }

        then:
        "true"        == xml.description.'@null'.text()
        0             == xml.description.children().size()
        ""            == xml.description.text()
    }


    def "Test special processing of association field"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        boolean invoked = false
        marshaller.metaClass.processField << {
            BeanWrapper beanWrapper, GrailsDomainClassProperty property, XML xml ->
            invoked = true
            if (property.getName() == 'parts') {
                xml.startNode('theParts')
                beanWrapper.getPropertyValue(property.getName()).each {
                    xml.startNode('part')

                    xml.startNode('theId')
                    xml.convertAnother(it.getId())
                    xml.end()
                    xml.startNode('theCode')
                    xml.convertAnother(it.code)
                    xml.end()

                    xml.end()
                }
                xml.end()
                return false
            } else {
                return true
            }
        }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        def parts = []
        def part = new MarshalledPartOfThing(code:'partA',description:'part A')
        part.setId(1)
        parts.add part
        thing.parts = parts


        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0 == xml.parts.size()
        true    == invoked
        1       == xml.theParts.size()
        1       == xml.theParts.children().size()
        1       == xml.theParts.part[0].theId.size()
        'partA' == xml.theParts.part[0].theCode.text()
    }

    def "Test Collection based association field marshalled as short object"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        def parts = []
        def part = new MarshalledPartOfThing(code:'partA',description:'part A')
        part.setId(1)
        parts.add part
        part = new MarshalledPartOfThing(code:'partB',description:'part B')
        part.setId(2)
        parts.add part
        thing.parts = parts

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        '/marshalled-part-of-things/1' == xml.parts.shortObject[0]._link.text()
        '/marshalled-part-of-things/2' == xml.parts.shortObject[1]._link.text()
        0 == xml.parts.code.size()
        0 == xml.parts.description.size()
    }

    def "Test Collection based association field deep marshalled"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.deepMarshallAssociation << {BeanWrapper wrapper, GrailsDomainClassProperty property -> true}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        def parts = []
        def part = new MarshalledPartOfThing(code:'partA',description:'part A')
        part.setId(1)
        parts.add part
        part = new MarshalledPartOfThing(code:'partB',description:'part B')
        part.setId(2)
        parts.add part
        thing.parts = parts

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        '1'      == xml.parts.marshalledPartOfThing[0].id.text()
        'partA'  == xml.parts.marshalledPartOfThing[0].code.text()
        'part A' == xml.parts.marshalledPartOfThing[0].description.text()

        '2'      == xml.parts.marshalledPartOfThing[1].id.text()
        'partB'  == xml.parts.marshalledPartOfThing[1].code.text()
        'part B' == xml.parts.marshalledPartOfThing[1].description.text()
    }

    def "Test Map based association field marshalled as short object"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledThingContributor contrib = new MarshalledThingContributor( firstName:'John', lastName:'Smith' )
        contrib.id = 5
        thing.contributors.put('smith',contrib)
        contrib = new MarshalledThingContributor( firstName:'John', lastName:'Anderson' )
        contrib.id = 6
        thing.contributors.put('anderson',contrib)

        when:
        def content = render( thing )
        def xml = XML.parse content
        def smith = xml.contributors.children().find { it.@key.text() == 'smith' }
        def anderson = xml.contributors.children().find { it.@key.text() == 'anderson' }

        then:
        2                                  == xml.contributors.children().size()
        'smith'                            == xml.contributors.entry[0].@key.text()
        '/marshalled-thing-contributors/5' == smith._link.text()
        '/marshalled-thing-contributors/6' == anderson._link.text()
        1                                  == xml.contributors.entry[0].children().size()
        1                                  == xml.contributors.entry[1].children().size()
    }

    def "Test Map based association field deep marshalled"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.deepMarshallAssociation << {BeanWrapper wrapper, GrailsDomainClassProperty property -> true}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledThingContributor contrib = new MarshalledThingContributor( firstName:'John', lastName:'Smith' )
        contrib.id = 5
        thing.contributors.put('smith',contrib)
        contrib = new MarshalledThingContributor( firstName:'John', lastName:'Anderson' )
        contrib.id = 6
        thing.contributors.put('anderson',contrib)

        when:
        def content = render( thing )
        def xml = XML.parse content
        def smith = xml.contributors.children().find { it.@key.text() == 'smith' }
        def anderson = xml.contributors.children().find { it.@key.text() == 'anderson' }

        then:
        2          == xml.contributors.children().size()
        '5'        == smith.id.text()
        'John'     == smith.firstName.text()
        'Smith'    == smith.lastName.text()
        '6'        == anderson.id.text()
        'John'     == anderson.firstName.text()
        'Anderson' == anderson.lastName.text()
    }

    def "Test many-to-one association field marshalled as short object"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledOwnerOfThing owner = new MarshalledOwnerOfThing( firstName:'John', lastName:'Smith' )
        owner.id = 5
        thing.owner = owner


        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0 == xml.owner.firstName.size()
        '/marshalled-owner-of-things/5' == xml.owner._link.text()
    }

    def "Test many-to-one association field deep marshalled"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.deepMarshallAssociation << {BeanWrapper wrapper, GrailsDomainClassProperty property -> true}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledOwnerOfThing owner = new MarshalledOwnerOfThing( firstName:'John', lastName:'Smith' )
        owner.id = 5
        thing.owner = owner


        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        '5'     == xml.owner.id.text()
        'John'  == xml.owner.firstName.text()
        'Smith' == xml.owner.lastName.text()
    }

    def "Test one-to-one association field marshalled as short object"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledSubPartOfThing part = new MarshalledSubPartOfThing( code:'zz' )
        part.id = 5
        thing.subPart = part


        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        0 == xml.subPart.code.size()
        '/marshalled-sub-part-of-things/5' == xml.subPart._link.text()
    }

    def "Test one-to-one association field deep marshalled"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.deepMarshallAssociation << {BeanWrapper wrapper, GrailsDomainClassProperty property -> true}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledSubPartOfThing part = new MarshalledSubPartOfThing( code:'zz' )
        part.id = 5
        thing.subPart = part


        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        '5' == xml.subPart.id.text()
        'zz' == xml.subPart.code.text()
    }

    def "Test embedded association field marshalled as full object"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledThingEmbeddedPart part = new MarshalledThingEmbeddedPart( serialNumber:'ad34fa', description:'foo' )
        thing.embeddedPart = part

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'ad34fa' == xml.embeddedPart.serialNumber.text()
        'foo'    == xml.embeddedPart.description.text()
    }

    def "Test additional fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.processAdditionalFields << {BeanWrapper wrapper, XML xml ->
            xml.startNode('additionalProp')
            xml.convertAnother(wrapper.getPropertyValue('code') + ':' + wrapper.getPropertyValue('description'))
            xml.end()
        }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'aa thing' )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'AA:aa thing' == xml.additionalProp.text()
    }

    private void register( String name, def marshaller ) {
        XML.createNamedConfig( "BasicDomainClassMarshaller:" + testName + ":$name" ) { json ->
            json.registerObjectMarshaller( marshaller, 100 )
        }
    }

    private void register( def marshaller ) {
        register( "default", marshaller )
    }

    private String render( String name, def obj ) {
        XML.use( "BasicDomainClassMarshaller:" + testName + ":$name" ) {
            return (obj as XML) as String
        }
    }

    private String render( def obj ) {
        render( "default", obj )
    }
}
