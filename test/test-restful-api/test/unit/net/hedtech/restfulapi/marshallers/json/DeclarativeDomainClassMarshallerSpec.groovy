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
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.test.mixin.web.*
import grails.test.mixin.support.*

import net.hedtech.restfulapi.*

import org.apache.commons.lang.UnhandledException

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.codehaus.groovy.grails.web.json.JSONObject

import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import org.springframework.web.context.WebApplicationContext

import org.junit.Rule
import org.junit.rules.TestName

import spock.lang.*


@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin,DomainClassUnitTestMixin])
@Mock([MarshalledThing,MarshalledPartOfThing,
       MarshalledSubPartOfThing,MarshalledThingContributor,
       MarshalledOwnerOfThing,MarshalledThingEmbeddedPart])
class DeclarativeDomainClassMarshallerSpec extends Specification {

    @Rule TestName testName = new TestName()

    void setup() {
    }

    def "Test with Id"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includeId:true
        )
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
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includeId:false
        )
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
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includeVersion:true
        )
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
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includeVersion:false
        )
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
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            excludedFields:['description','isLarge']
        )
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
        def marshaller = new DeclarativeDomainClassMarshaller(
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
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includedFields:['code','description','parts']
        )
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
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includedFields:[],
            includeId:false,
            includeVersion:false
        )
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
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includedFields:['code','description','lastModifiedBy'],
            excludedFields:['code','description']
        )
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

    def "Test alternative names of fields"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            fieldNames:['description':'modDescription', 'parts':'modParts']
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.parts = [new PartOfThing(code:'BB',description:'bb part')]

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        !json.containsKey('description')
        !json.containsKey('parts')
        'aa thing' == json.modDescription
        'AA'       == json.code
        1          == json.modParts.size()
    }

    def "Test limiting marshalling to a class"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing
        )
        int counter = 0
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        Thing otherThing = new Thing( code:'BB', description:'bb thing' )

        expect:
        marshaller.supports( thing )
        !marshaller.supports( otherThing )
    }

    def "Test additional field closures"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            additionalFieldClosures:[
                {map ->
                    def json = map['json']
                    def beanWrapper = map['beanWrapper']
                    json.property('title',beanWrapper.getPropertyValue('code') + "-" + beanWrapper.getPropertyValue('description'))
                },
                {map ->
                    map['json'].property('tag',"not it")
                }]
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        'AA-aa thing' == json.title
        'not it'      == json.tag
    }

    def "Test additional fields map"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ],
            additionalFieldsMap:['a1':'b1','a2':'b2','json':null,'beanWrapper':null,'grailsApplication':null]
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )

        then:
        thing == passedMap['beanWrapper'].getWrappedInstance()
        grailsApplication == passedMap['grailsApplication']
        null != passedMap['json']
        'b1' == passedMap['a1']
        'b2' == passedMap['a2']
    }

    def "Test map passed to additional fields closures has the derived resource name added"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ]
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )

        then:
        'marshalled-things' == passedMap['resourceName']
    }

    def "Test resourceName is not overridden if specified"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ],
            additionalFieldsMap:['resourceName':'my-things']
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )

        then:
        'my-things' == passedMap['resourceName']
    }

    def "Test map passed to additional fields closures has the resource id added"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ]
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        thing.id = 50

        when:
        def content = render( thing )

        then:
        50 == passedMap['resourceId']
    }

    def "Test Collection based association field resource name"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            fieldResourceNames:['parts':'customized-parts']
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        def parts = []
        def part = new MarshalledPartOfThing(code:'partA',desc:'part A')
        part.setId(1)
        parts.add part
        thing.parts = parts

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        '/customized-parts/1' == json.parts[0]._link
    }

    def "Test Map based association field resource name"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            fieldResourceNames:['contributors':'customized-contributors']
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        MarshalledThingContributor contrib = new MarshalledThingContributor( firstName:'John', lastName:'Smith' )
        contrib.id = 5
        thing.contributors=['smith':contrib]


        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        1    == json.contributors.size()
        '/customized-contributors/5' == json.contributors['smith']._link
    }

    def "Test many-to-one association field resource name"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            fieldResourceNames:['owner':'customized-owners']
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
        '/customized-owners/5' == json.owner._link
    }

    def "Test one-to-one association field resource name"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            fieldResourceNames:['subPart':'customized-subparts']
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
        '/customized-subparts/5' == json.subPart._link
    }

    def "Test short object closure"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            shortObjectClosure:{ def map ->
                def json = map['json']
                def writer = json.getWriter()
                writer.object()
                writer.key("resource").value(map['resourceName'])
                writer.key("id").value(map['resourceId'])
                writer.endObject()
            }
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        def parts = [new PartOfThing(code:'BB',description:'bb part')]
        parts[0].id = 15
        thing.parts = parts

        when:
        def content = render( thing )
        def json = JSON.parse content

        then:
        15 == json.parts[0].id
    }

    def "Test map passed to short object closure"() {
        setup:
        def passedMap
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            shortObjectClosure:{ def map ->
                passedMap = map.clone()
            }
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        def parts = [new PartOfThing(code:'BB',description:'bb part')]
        parts[0].id = 15
        thing.parts = parts

        when:
        render( thing )

        then:
        grailsApplication           == passedMap['grailsApplication']
        'parts'                     == passedMap['property'].getName()
        parts[0]                    == passedMap['refObject']
        null                        != passedMap['json']
        15                          == passedMap['resourceId']
        'marshalled-part-of-things' == passedMap['resourceName']
    }

    def "Test map passed to short object closure has the overridden resource name"() {
        setup:
        def passedMap
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            fieldResourceNames:['parts':'custom-parts'],
            shortObjectClosure:{ def map ->
                passedMap = map.clone()
            }
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        def parts = [new PartOfThing(code:'BB',description:'bb part')]
        parts[0].id = 15
        thing.parts = parts

        when:
        render( thing )

        then:
        'custom-parts' == passedMap['resourceName']
    }

    def "Test require included fields"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            requireIncludedFields:true,
            includedFields:['code','aFieldThatDoesNotExist', 'anotherFieldThatDoesNotExist']
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'aa thing' )

        when:
        render( thing )

        then:
        UnhandledException e = thrown()
        ['aFieldThatDoesNotExist', 'anotherFieldThatDoesNotExist'] == e.getCause().missingNames
    }

    @Unroll
    def "Test default deep marshalling"(def fieldName) {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            deepMarshallAssociations:true
        )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'aa thing' )
        def bean = new BeanWrapperImpl(thing)
        def domainClass = grailsApplication.getDomainClass(thing.getClass().getName())
        def property = domainClass.getPersistentProperty(fieldName)

        when:
        def deep = marshaller.deepMarshallAssociation(bean,property)

        then:
        true == deep

        where:
        fieldName << ['parts','subPart','owner','contributors']
    }

    @Unroll
    def "Test field-level deep marshalling"(def fieldName) {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            deepMarshallAssociations:false,
            deepMarshalledFields:[("$fieldName".toString()):true]
        )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'aa thing' )
        def bean = new BeanWrapperImpl(thing)
        def domainClass = grailsApplication.getDomainClass(thing.getClass().getName())
        def property = domainClass.getPersistentProperty(fieldName)

        when:
        def deep = marshaller.deepMarshallAssociation(bean,property)

        then:
        true == deep

        where:
        fieldName << ['parts','subPart','owner','contributors']
    }

    private void register( String name, def marshaller ) {
        JSON.createNamedConfig( "DeclarativeDomainClassMarshaller:" + testName + ":$name" ) { json ->
            json.registerObjectMarshaller( marshaller, 100 )
        }
    }

    private void register( def marshaller ) {
        register( "default", marshaller )
    }

    private String render( String name, def obj ) {
        JSON.use( "DeclarativeDomainClassMarshaller:" + testName + ":$name" ) {
            return (obj as JSON) as String
        }
    }

    private String render( def obj ) {
        render( "default", obj )
    }
}
