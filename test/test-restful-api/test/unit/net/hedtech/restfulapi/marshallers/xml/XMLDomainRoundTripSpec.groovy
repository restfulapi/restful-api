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

import org.junit.Rule
import org.junit.rules.TestName

import org.springframework.beans.BeanWrapper
import org.springframework.web.context.WebApplicationContext

import spock.lang.*


/**
 * Sanity check that the MapExtractor can parse content generated
 * by the declarative marshaller.
 */
@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin,DomainClassUnitTestMixin])
@Mock([MarshalledThing,MarshalledPartOfThing,
       MarshalledSubPartOfThing,MarshalledThingContributor,
       MarshalledOwnerOfThing,MarshalledThingEmbeddedPart])
class XMLDomainRoundTripSpec extends Specification {

    @Rule TestName testName = new TestName()

    def "Test simple properties"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includeVersion:false,
            includedFields:['code','description']
        )
        register(marshaller)
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1

        def expected = [id:'1', code:'AA',description:'AA thing']

        when:
        def content = render(thing)
        def map = new DeclarativeXMLExtractor().extract(XML.parse(content))

        then:
        expected == map
    }

    def "Test collection of associations"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includeVersion:false,
            includedFields:['code','description', 'parts']
        )
        register(marshaller)
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1
        def part = new MarshalledPartOfThing(id:5, code:'a1',description:'part 1')
        part.id = 5
        thing.parts.add part
        part = new MarshalledPartOfThing(id:6, code:'a2',description:'part 2')
        part.id = 6
        thing.parts.add part

        def extractor = new DeclarativeXMLExtractor(dottedShortObjectPaths:['parts'])

        def expected = [id:'1', code:'AA', description:'AA thing',
            parts:['5', '6'] ]

        when:
        def content = render(thing)
        def map = extractor.extract(XML.parse(content))

        then:
        expected == map
    }

    def "Test simple collection"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includeVersion:false,
            includedFields:['code','description', 'simpleArray']
        )
        register(marshaller)
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1
        thing.simpleArray = ['a','b']

        def extractor = new DeclarativeXMLExtractor()

        def expected = [id:'1', code:'AA', description:'AA thing',
            simpleArray:['a','b'] ]

        when:
        def content = render(thing)
        def map = extractor.extract(XML.parse(content))

        then:
        expected == map
    }

    def "Test map of associations"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includeVersion:false,
            includedFields:['code','description', 'contributors']
        )
        register(marshaller)
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1
        def part = new MarshalledThingContributor(firstName:'John',lastName:'Smith')
        part.id = 5
        thing.contributors.put('smith',part)
        part = new MarshalledThingContributor(firstName:'John',lastName:'Adams')
        part.id = 6
        thing.contributors.put('adams',part)

        def extractor = new DeclarativeXMLExtractor(dottedShortObjectPaths:['contributors'])

        def expected = [id:'1', code:'AA', description:'AA thing',
            contributors:['smith':['id':'5'], 'adams':['id':'6']] ]

        when:
        def content = render(thing)
        def map = extractor.extract(XML.parse(content))

        then:
        expected == map
    }


    def "Test simple map"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includeVersion:false,
            includedFields:['code','description', 'simpleMap']
        )
        register(marshaller)
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1
        thing.simpleMap = ['a':1,'b':2]

        def extractor = new DeclarativeXMLExtractor()

        def expected = [id:'1', code:'AA', description:'AA thing',
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
        marshallers.add new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            includeVersion:false,
            includedFields:['code','description', 'embeddedPart']
        )
        marshallers.add new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThingEmbeddedPart,
            includeVersion:false,
            includeId:false,
            includedFields:['serialNumber','description']
        )
        register(marshallers)
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.id = 1
        thing.embeddedPart = new MarshalledThingEmbeddedPart(serialNumber:'1234', description:'embedded')

        def extractor = new DeclarativeXMLExtractor()

        def expected = [id:'1', code:'AA', description:'AA thing',
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
