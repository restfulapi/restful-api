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
package net.hedtech.restfulapi.marshallers.json

import grails.converters.JSON
import grails.test.mixin.*
import grails.test.mixin.support.*
import grails.test.mixin.web.*
import grails.test.mixin.domain.DomainClassUnitTestMixin

import net.hedtech.restfulapi.*

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.json.JSONObject

import org.junit.Rule
import org.junit.rules.TestName

import org.springframework.beans.BeanWrapper
import org.springframework.web.context.WebApplicationContext

import spock.lang.*


@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin,DomainClassUnitTestMixin])
@Mock([MarshalledThing,MarshalledPartOfThing,
       MarshalledSubPartOfThing,MarshalledThingContributor,
       MarshalledOwnerOfThing,MarshalledThingEmbeddedPart])
class BasicDomainClassMarshallerSpec extends Specification {

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
        def json = JSON.parse content

        then:
        1    == json.id
        'AA' == json.code
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
        def json = JSON.parse content

        then:
        !json.containsKey('id')
        'AA' == json.code
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
        def json = JSON.parse content

        then:
        1    == json.version
        'AA' == json.code
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
        def json = JSON.parse content

        then:
        !json.containsKey('version')
        'AA' == json.code
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
        def json = JSON.parse content

        then:
        !json.containsKey('description')
        !json.containsKey('isLarge')
        'AA' == json.code
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
        def json = JSON.parse content

        then:
        !json.containsKey('lastModifiedBy')
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
        def json = JSON.parse content

        then:
        'AA'       == json.code
        'aa thing' == json.description
        !json.containsKey('contributors')
        !json.containsKey('embeddedPart')
        !json.containsKey('owner')
        !json.containsKey('subPart')
        !json.containsKey('isLarge')
        !json.containsKey('lastModified')
        !json.containsKey('lastModifiedBy')
        !json.containsKey('dataOrigin')
    }

    def "Test including no fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.getIncludedFields << {Object value-> [] }
        marshaller.metaClass.includeVersionFor << {Object value-> false}
        marshaller.metaClass.includeIdFor << {Object value-> false}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        0 == json.keySet().size()
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
        def json = JSON.parse content

        then:
        'AA'         == json.code
        'aa thing'   == json.description
        'John Smith' == json.lastModifiedBy

    }

    def "Test special processing of simple fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.processField << {
            BeanWrapper beanWrapper, GrailsDomainClassProperty property, JSON json ->
            if (property.getName() == 'description') {
                json.getWriter().key('modDescription')
                json.convertAnother(beanWrapper.getPropertyValue(property.getName()))
                return false
            } else {
                return true
            }
        }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        !json.containsKey('description')
        'aa thing' == json.modDescription
        'AA'       == json.code
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
        def json = JSON.parse content

        then:
        !json.containsKey('description')
        'aa thing' == json.modDescription
        'AA'       == json.code
    }

    def "Test that null Collection association field is marshalled as null"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.parts = null

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        null            == thing.parts
        JSONObject.NULL == json.parts
    }

    def "Test that null Map association field is marshalled as null"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", contributors:null )

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        null            == thing.contributors
        JSONObject.NULL == json.contributors
    }

    def "Test that null one-to-one association field is marshalled as null"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", subPart:null )

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        null            == thing.subPart
        JSONObject.NULL == json.subPart
    }

    def "Test that null many-to-one association field is marshalled as null"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", owner:null )

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        null            == thing.owner
        JSONObject.NULL == json.owner
    }

    def "Test that null embedded association field is marshalled as null"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", embeddedPart:null )

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        null            == thing.embeddedPart
        JSONObject.NULL == json.embeddedPart
    }

   def "Test that null Collection association field is deep marshalled as null"() {
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
        def json = JSON.parse content

        then:
        null            == thing.parts
        JSONObject.NULL == json.parts
    }

    def "Test that null Map association field is deep marshalled as null"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.deepMarshallAssociation << {BeanWrapper wrapper, GrailsDomainClassProperty property -> true}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", contributors:null )

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        null            == thing.contributors
        JSONObject.NULL == json.contributors
    }

    def "Test that null one-to-one association field is deep marshalled as null"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.deepMarshallAssociation << {BeanWrapper wrapper, GrailsDomainClassProperty property -> true}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", subPart:null )

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        null            == thing.subPart
        JSONObject.NULL == json.subPart
    }

    def "Test that null many-to-one association field is deep marshalled as null"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.deepMarshallAssociation << {BeanWrapper wrapper, GrailsDomainClassProperty property -> true}
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing", owner:null )

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        null            == thing.owner
        JSONObject.NULL == json.owner
    }


    def "Test special processing of association field"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        boolean invoked = false
        marshaller.metaClass.processField << {
            BeanWrapper beanWrapper, GrailsDomainClassProperty property, JSON json ->
            invoked = true
            if (property.getName() == 'parts') {
                def writer = json.getWriter()
                writer.key('theParts')
                writer.array()
                beanWrapper.getPropertyValue(property.getName()).each {
                    writer.object()
                    writer.key('theId').value(it.getId())
                    writer.key('theCode').value(it.code)
                    writer.endObject()
                }
                writer.endArray()
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
        def json = JSON.parse content

        then:
        !json.containsKey('parts')
        true    == invoked
        1       == json.theParts.size()
        1       == json.theParts[0].theId
        'partA' == json.theParts[0].theCode

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
        def json = JSON.parse content

        then:
        '/marshalled-part-of-things/1' == json.parts[0]._link
        '/marshalled-part-of-things/2' == json.parts[1]._link
        !json.parts[0].containsKey('code')
        !json.parts[0].containsKey('description')
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
        def json = JSON.parse content

        then:
        1        == json.parts[0].id
        'partA'  == json.parts[0].code
        'part A' == json.parts[0].description
        2        == json.parts[1].id
        'partB'  == json.parts[1].code
        'part B' == json.parts[1].description
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
        def json = JSON.parse content

        then:
        2    == json.contributors.size()
        '/marshalled-thing-contributors/5' == json.contributors['smith']._link
        '/marshalled-thing-contributors/6' == json.contributors['anderson']._link
        !json.contributors['smith'].containsKey('lastName')
        !json.contributors['smith'].containsKey('firstName')
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
        def json = JSON.parse content

        then:
        2          == json.contributors.size()
        5          == json.contributors['smith'].id
        'John'     == json.contributors['smith'].firstName
        'Smith'    == json.contributors['smith'].lastName
        6          == json.contributors['anderson'].id
        'John'     == json.contributors['anderson'].firstName
        'Anderson' == json.contributors['anderson'].lastName
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
        def json = JSON.parse content

        then:
        !json.owner.containsKey('firstName')
        '/marshalled-owner-of-things/5' == json.owner._link
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
        def json = JSON.parse content

        then:
        5       == json.owner.id
        'John'  == json.owner.firstName
        'Smith' == json.owner.lastName
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
        def json = JSON.parse content

        then:
        !json.subPart.containsKey('code')
        '/marshalled-sub-part-of-things/5' == json.subPart._link
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
        def json = JSON.parse content

        then:
        5 == json.subPart.id
        'zz' == json.subPart.code
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
        def json = JSON.parse content

        then:
        'ad34fa' == json.embeddedPart.serialNumber
        'foo'    == json.embeddedPart.description
    }

    def "Test additional fields"() {
        setup:
        def marshaller = new BasicDomainClassMarshaller(
            app:grailsApplication
        )
        marshaller.metaClass.processAdditionalFields << {BeanWrapper wrapper, JSON json ->
            json.property('additionalProp',wrapper.getPropertyValue('code') + ':' + wrapper.getPropertyValue('description'))
        }
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'aa thing' )

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        'AA:aa thing' == json.additionalProp
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

    private void register( String name, def marshaller ) {
        JSON.createNamedConfig( "BasicDomainClassMarshaller:" + testName + ":$name" ) { json ->
            json.registerObjectMarshaller( marshaller, 100 )
        }
    }

    private void register( def marshaller ) {
        register( "default", marshaller )
    }

    private String render( String name, def obj ) {
        JSON.use( "BasicDomainClassMarshaller:" + testName + ":$name" ) {
            return (obj as JSON) as String
        }
    }

    private String render( def obj ) {
        render( "default", obj )
    }
}
