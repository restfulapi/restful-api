/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.marshallers.json

import grails.test.mixin.*
import grails.test.mixin.web.*
import spock.lang.*
import net.hedtech.restfulapi.*
import grails.converters.JSON
import grails.test.mixin.support.*
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.springframework.web.context.WebApplicationContext
import org.springframework.beans.BeanWrapper
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.web.json.JSONObject
import grails.test.mixin.domain.DomainClassUnitTestMixin

import org.junit.Rule
import org.junit.rules.TestName

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
            substitutions:['description':'modDescription', 'parts':'modParts']
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