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

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer

import org.junit.Rule
import org.junit.rules.TestName

import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import org.springframework.web.context.WebApplicationContext

import spock.lang.*


@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin,DomainClassUnitTestMixin])
@Mock([MarshalledThing,MarshalledPartOfThing,
       MarshalledSubPartOfThing,MarshalledThingContributor,
       MarshalledOwnerOfThing,MarshalledThingEmbeddedPart])
class XMLDeclarativeDomainClassMarshallerSpec extends Specification {

    @Rule TestName testName = new TestName()

    void setup() {
    }

    def "Test default element name"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication
        )

        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.save()
        thing.id = 1

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'marshalledThing'  == xml.name()
    }

    def "Test overidden element name"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            elementName:'Thing'
        )

        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:'AA thing' )
        thing.save()
        thing.id = 1

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'Thing'  == xml.name()
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
        def xml = XML.parse content

        then:
        '1'  == xml.id.text()
        'AA' == xml.code.text()
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
        def xml = XML.parse content

        then:
        0    == xml.id.size()
        'AA' == xml.code.text()
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
        def xml = XML.parse content

        then:
        '1'  == xml.version.text()
        'AA' == xml.code.text()
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
        def xml = XML.parse content

        then:
        0 == xml.version.size()
        'AA' == xml.code.text()
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
        def xml = XML.parse content

        then:
        0    == xml.description.size()
        0    == xml.isLarge.size()
        'AA' == xml.code.text()
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
        def xml = XML.parse content

        then:
        0    == xml.lastModifiedBy.size()
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
        def xml = XML.parse content

        then:
        'AA'       == xml.code.text()
        'aa thing' == xml.description.text()
        0          == xml.contributors.size()
        0          == xml.embeddedPart.size()
        0          == xml.owner.size()
        0          == xml.subPart.size()
        0          == xml.isLarge.size()
        0          == xml.lastModified.size()
        0          == xml.lastModifiedBy.size()
        0          == xml.dataOrigin.size()
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
        def xml = XML.parse content

        then:
        0 == xml.children.size()
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
        Exception e = thrown()
        ['aFieldThatDoesNotExist', 'anotherFieldThatDoesNotExist'] == e.getCause().missingNames
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
        def xml = XML.parse content

        then:
        'AA'         == xml.code.text()
        'aa thing'   == xml.description.text()
        'John Smith' == xml.lastModifiedBy.text()
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
        def xml = XML.parse content

        then:
        0          == xml.description.size()
        0          == xml.parts.size()
        'aa thing' == xml.modDescription.text()
        'AA'       == xml.code.text()
        1          == xml.modParts.size()
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
                    def xml = map['xml']
                    def beanWrapper = map['beanWrapper']
                    xml.startNode('title')
                    xml.convertAnother(beanWrapper.getPropertyValue('code') + "-" + beanWrapper.getPropertyValue('description'))
                    xml.end()
                },
                {map ->
                    def xml = map['xml']
                    xml.startNode('tag')
                    xml.convertAnother('not it')
                    xml.end()
                }]
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'AA-aa thing' == xml.title.text()
        'not it'      == xml.tag.text()
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
            additionalFieldsMap:['a1':'b1','a2':'b2','xml':null,'beanWrapper':null,'grailsApplication':null]
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )

        when:
        def content = render( thing )

        then:
        thing             == passedMap['beanWrapper'].getWrappedInstance()
        grailsApplication == passedMap['grailsApplication']
        null              != passedMap['xml']
        'b1'              == passedMap['a1']
        'b2'              == passedMap['a2']
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
        def xml = XML.parse content

        then:
        '/customized-parts/1' == xml.parts[0].shortObject._link.text()
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
        def xml = XML.parse content

        then:
        1                            == xml.contributors.entry.size()
        'smith'                      == xml.contributors.entry[0].'@key'.text()
        '/customized-contributors/5' == xml.contributors.entry[0]._link.text()
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
        def xml = XML.parse content

        then:
        0                      == xml.owner.firstName.size()
        '/customized-owners/5' == xml.owner._link.text()
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
        def xml = XML.parse content

        then:
        0                        == xml.subPart.code.size()
        '/customized-subparts/5' == xml.subPart._link.text()
    }

    def "Test short object closure"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            supportClass:MarshalledThing,
            shortObjectClosure:{ def map ->
                def xml = map['xml']
                xml.startNode('resource')
                xml.convertAnother(map['resourceName'])
                xml.end()
                xml.startNode('id')
                xml.convertAnother(map['resourceId'])
                xml.end()
            }
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:"aa thing" )
        def parts = [new PartOfThing(code:'BB',description:'bb part')]
        parts[0].id = 15
        thing.parts = parts

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        '15' == xml.parts[0].shortObject.id.text()
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
        null                        != passedMap['xml']
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

    def "Test including only non-null fields"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includeId:false,
            includeVersion:false,
            marshallNullFields:false
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', description:null, embeddedPart:null, owner:null )

        when:
        def content = render(thing)
        def xml = XML.parse content

        then:
        'AA' == xml.code.text()
        0    == xml.description.size()
        0    == xml.embeddedPart.size()
        0    == xml.owner.size()
        1    == xml.contributors.size()
        1    == xml.simpleMap.size()
        1    == xml.simpleArray.size()
        1    == xml.parts.size()
    }

    def "Test including non-null fields on a per-field basis"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includeId:false,
            includeVersion:false,
            marshalledNullFields:['embeddedPart':false, 'owner':false]
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', embeddedPart:null, owner:null, description:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'AA'   == xml.code.text()
        1      == xml.description.size()
        'true' == xml.description.'@null'.text()
        0      == xml.embeddedPart.size()
        0      == xml.owner.size()
        1      == xml.contributors.size()
        1      == xml.simpleMap.size()
        1      == xml.simpleArray.size()
        1      == xml.parts.size()
    }

    def "Test including non-null fields on a per-field basis overrides default"() {
        setup:
        def marshaller = new DeclarativeDomainClassMarshaller(
            app:grailsApplication,
            includeId:false,
            includeVersion:false,
            marshallNullFields:false,
            marshalledNullFields:['embeddedPart':true, 'owner':true]
        )
        register( marshaller )
        MarshalledThing thing = new MarshalledThing( code:'AA', embededPart:null, owner:null, description:null )

        when:
        def content = render( thing )
        def xml = XML.parse content

        then:
        'AA'   == xml.code.text()
        0      == xml.description.size()
        1      == xml.embeddedPart.size()
        1      == xml.owner.size()
        'true' == xml.embeddedPart.'@null'.text()
        'true' == xml.embeddedPart.'@null'.text()
        1      == xml.contributors.size()
        1      == xml.simpleMap.size()
        1      == xml.simpleArray.size()
        1      == xml.parts.size()
    }

    private void register( String name, def marshaller ) {
        XML.createNamedConfig( "DeclarativeDomainClassMarshaller:" + testName + ":$name" ) { xml ->
            xml.registerObjectMarshaller( marshaller, 100 )
        }
    }

    private void register( def marshaller ) {
        register( "default", marshaller )
    }

    private String render( String name, def obj ) {
        XML.use( "DeclarativeDomainClassMarshaller:" + testName + ":$name" ) {
            return (obj as XML) as String
        }
    }

    private String render( def obj ) {
        render( "default", obj )
    }
}
