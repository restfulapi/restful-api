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
import net.hedtech.restfulapi.extractors.xml.*

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer

import org.springframework.beans.BeanWrapper
import org.springframework.web.context.WebApplicationContext

import org.junit.Rule
import org.junit.rules.TestName

import spock.lang.*


/**
 * Sanity check that the MapExtractor can parse content generated
 * by the declarative marshaller.
 */
@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin,DomainClassUnitTestMixin])
@Mock([MarshalledThing,MarshalledPartOfThing,
       MarshalledSubPartOfThing,MarshalledThingContributor,
       MarshalledOwnerOfThing,MarshalledThingEmbeddedPart])
class XMLBeanRoundTripSpec extends Specification {

    @Rule TestName testName = new TestName()

    def "Test simple properties"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            includedFields:['code','description']
        )
        register(marshaller)
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1

        def expected = [code:'AA',description:'AA thing']

        when:
        def content = render(thing)
        def map = new DeclarativeXMLExtractor().extract(XML.parse(content))

        then:
        expected == map
    }

    def "Test collection of objects"() {
        setup:
        def marshallers = []
        marshallers.add new DeclarativeBeanMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            includedFields:['code','description', 'parts']
        )
        marshallers.add new DeclarativeBeanMarshaller(
            app:grailsApplication,
            supportClass:MarshalledPartOfThing,
            includedFields:['code','description']
        )
        register(marshallers)
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        def part = new MarshalledPartOfThing(code:'a1',description:'part 1')
        thing.parts.add part
        part = new MarshalledPartOfThing(code:'a2',description:'part 2')
        thing.parts.add part

        def extractor = new DeclarativeXMLExtractor()

        def expected = [code:'AA', description:'AA thing',
            parts:[ [code:'a1',description:'part 1'], [code:'a2', description:'part 2'] ] ]

        when:
        def content = render(thing)
        def map = extractor.extract(XML.parse(content))

        then:
        expected == map
    }

    def "Test simple collection"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            includedFields:['code','description', 'simpleArray']
        )
        register(marshaller)
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1
        thing.simpleArray = ['a','b']

        def extractor = new DeclarativeXMLExtractor()

        def expected = [code:'AA', description:'AA thing',
            simpleArray:['a','b'] ]

        when:
        def content = render(thing)
        def map = extractor.extract(XML.parse(content))

        then:
        expected == map
    }

    def "Test map of objects"() {
        setup:
        def marshallers = []
        marshallers.add new DeclarativeBeanMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            includedFields:['code','description', 'contributors']
        )
        marshallers.add new DeclarativeBeanMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThingContributor,
            includedFields:['firstName','lastName']
        )
        register(marshallers)
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        def part = new MarshalledThingContributor(firstName:'John',lastName:'Smith')
        thing.contributors.put('smith',part)
        part = new MarshalledThingContributor(firstName:'John',lastName:'Adams')
        thing.contributors.put('adams',part)

        def extractor = new DeclarativeXMLExtractor()

        def expected = [code:'AA', description:'AA thing',
            contributors:['smith':[firstName:'John',lastName:'Smith'],
                          'adams':[firstName:'John',lastName:'Adams']] ]

        when:
        def content = render(thing)
        def map = extractor.extract(XML.parse(content))

        then:
        expected == map
    }


    def "Test simple map"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            includedFields:['code','description', 'simpleMap']
        )
        register(marshaller)
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.simpleMap = ['a':1,'b':2]

        def extractor = new DeclarativeXMLExtractor()

        def expected = [code:'AA', description:'AA thing',
            simpleMap:['a':'1','b':'2'] ]

        when:
        def content = render(thing)
        def map = extractor.extract(XML.parse(content))

        then:
        expected == map
    }

    def "Test embedded"() {
        setup:
        def marshallers = []
        marshallers.add new DeclarativeBeanMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            includedFields:['code','description', 'embeddedPart']
        )
        marshallers.add new DeclarativeBeanMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThingEmbeddedPart,
            includedFields:['serialNumber','description']
        )
        register(marshallers)
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.embeddedPart = new MarshalledThingEmbeddedPart(serialNumber:'1234', description:'embedded')

        def extractor = new DeclarativeXMLExtractor()

        def expected = [code:'AA', description:'AA thing',
            embeddedPart:[serialNumber:'1234',description:'embedded'] ]

        when:
        def content = render(thing)
        def map = extractor.extract(XML.parse(content))

        then:
        expected == map
    }

    private void register( String name, List marshaller ) {
        XML.createNamedConfig( "XMLDomainRoundTripSpec:" + testName + ":$name" ) { xml ->
            marshaller.each() {
                xml.registerObjectMarshaller( it, 100 )
            }
        }
    }

    private void register( List marshaller ) {
        register( "default", marshaller )
    }

    private void register( def marshaller ) {
        register("default",[marshaller])
    }

    private String render( String name, def obj ) {
        XML.use( "XMLDomainRoundTripSpec:" + testName + ":$name" ) {
            return (obj as XML) as String
        }
    }

    private String render( def obj ) {
        render( "default", obj )
    }
}
