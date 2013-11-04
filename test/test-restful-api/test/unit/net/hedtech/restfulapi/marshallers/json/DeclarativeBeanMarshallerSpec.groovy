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
import grails.test.mixin.support.*
import grails.test.mixin.web.*

import net.hedtech.restfulapi.*
import net.hedtech.restfulapi.beans.*

import org.apache.commons.lang.UnhandledException

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.codehaus.groovy.grails.web.json.JSONObject

import org.junit.Rule
import org.junit.rules.TestName

import org.springframework.beans.BeanWrapper
import org.springframework.web.context.WebApplicationContext

import spock.lang.*


@TestMixin([GrailsUnitTestMixin,ControllerUnitTestMixin])
class DeclarativeBeanMarshallerSpec extends Specification {

    @Rule TestName testName = new TestName()

    @Unroll
    def "Test excluding fields and properties"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            excludedFields:['property','publicField']
        )
        register( marshaller )

        when:
        def content = render(bean)
        def json = JSON.parse content

        then:
        !json.containsKey('property')
        !json.containsKey('publicField')
        json.containsKey('property2')
        json.containsKey('publicField2')

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test default exclusions"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication
        )
        register( marshaller )

        when:
        def content = render(bean)
        def json = JSON.parse content

        then:
        !json.containsKey('password')
        !json.containsKey('class')
        !json.containsKey('metaClass')

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test including fields"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            includedFields:['property','publicField']
        )
        register( marshaller )

        when:
        def content = render(bean)
        def json = JSON.parse content

        then:
        2     == json.keySet().size()
        'foo' == json.property
        'bar' == json.publicField

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test including no fields"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            includedFields:[]
        )
        register( marshaller )

        when:
        def content = render(bean)
        def json = JSON.parse content

        then:
        0 == json.keySet().size()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test require included fields"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            includedFields:[ 'property', 'publicField', 'missingField1', 'missingField2'],
            requireIncludedFields:true
        )
        register( marshaller )

        when:
        render( bean )

        then:
        UnhandledException e = thrown()
        ['missingField1', 'missingField2'] == e.getCause().missingNames

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test that included fields overrides excluded fields"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            includedFields:['property','publicField', 'publicField2'],
            excludedFields:['property','publicField']
        )
        register( marshaller )

        when:
        def content = render(bean)
        def json = JSON.parse content

        then:
        3      == json.keySet().size()
        'foo'  == json.property
        'bar'  == json.publicField
        'foos' == json.publicField2

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar', publicField2:'foos'),
                 new SimpleJavaBean(property:'foo', publicField:'bar', publicField2:'foos')]
    }

    @Unroll
    def "Test alternative names of fields"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            fieldNames:['property':'modProperty', 'publicField':'modPublicField']
        )
        register( marshaller )

        when:
        def content = render(bean)
        def json = JSON.parse content

        then:
        !json.containsKey('property')
        !json.containsKey('publicField')
        'foo' == json.modProperty
        'bar' == json.modPublicField

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    def "Test limiting marshalling to a class"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            supportClass:SimpleBean
        )
        int counter = 0
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')
        Thing thing = new Thing( code:'BB', description:'bb thing' )

        expect:
        thing instanceof GroovyObject
        marshaller.supports( bean )
        !marshaller.supports( thing )
    }

    @Unroll
    def "Test additional field closures"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            supportClass:bean.class,
            additionalFieldClosures:[
                {map ->
                    def json = map['json']
                    def beanWrapper = map['beanWrapper']
                    Object val = beanWrapper.getWrappedInstance()
                    json.property('title',beanWrapper.getPropertyValue('property') + "-" + val.publicField)
                },
                {map ->
                    map['json'].property('tag',"not it")
                }]
        )
        register( marshaller )

        when:
        def content = render(bean)
        def json = JSON.parse content

        then:
        'foo-bar' == json.title
        'not it'  == json.tag

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test additional fields map"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            supportClass:bean.class,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ],
            additionalFieldsMap:['a1':'b1','a2':'b2','json':null,'beanWrapper':null,'grailsApplication':null]
        )
        register( marshaller )

        when:
        def content = render(bean)

        then:
        null              != passedMap['beanWrapper']
        bean              == passedMap['beanWrapper'].getWrappedInstance()
        grailsApplication == passedMap['grailsApplication']
        null              != passedMap['json']
        'b1'              == passedMap['a1']
        'b2'              == passedMap['a2']

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test map passed to additional fields closures has the derived resource name added"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ]
        )
        register( marshaller )

        when:
        def content = render(bean)

        then:
        resourceName == passedMap['resourceName']

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
        resourceName << ['simple-beans', 'simple-java-beans']
    }

    @Unroll
    def "Test resourceName is not overridden if specified"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ],
            additionalFieldsMap:['resourceName':'my-beans']
        )
        register( marshaller )

        when:
        def content = render(bean)

        then:
        'my-beans' == passedMap['resourceName']

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test metaprogrammed getId() is passed as resourceId to field closures"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ]
        )
        register( marshaller )
        bean.metaClass.getId << {-> 50 }

        when:
        def content = render(bean)

        then:
        50 == passedMap['resourceId']

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test metaprogrammed id property is passed as resourceId to field closures"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ]
        )
        register( marshaller )
        bean.metaClass.id = 51

        when:
        def content = render(bean)

        then:
        51 == passedMap['resourceId']

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test bean with public id field has value passed to field closures"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ]
        )
        register( marshaller )
        bean.id = 52

        when:
        def content = render(bean)

        then:
        52 == passedMap['resourceId']

        where:
        bean << [new SimpleBeanWithIdField(property:'foo', publicField:'bar'),
                 new SimpleJavaBeanWithIdField(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test bean with id property has value passed to field closures"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ]
        )
        register( marshaller )
        bean.id = 53

        when:
        def content = render(bean)

        then:
        53 == passedMap['resourceId']

        where:
        bean << [new SimpleBeanWithIdProperty(property:'foo', publicField:'bar'),
                 new SimpleJavaBeanWithIdProperty(property:'foo', publicField:'bar')]
    }


    private void register( String name, def marshaller ) {
        JSON.createNamedConfig( "BeanMarshallerSpec:" + testName.getMethodName() + ":$name" ) { json ->
            json.registerObjectMarshaller( marshaller, 100 )
        }
    }

    private void register( def marshaller ) {
        register( "default", marshaller )
    }

    private String render( String name, def obj ) {
        JSON.use( "BeanMarshallerSpec:" + testName.getMethodName() + ":$name" ) {
            //for some reason (obj as JSON) doesn't work in parameterized spock tests
            def converter = new JSON(obj)
            converter.toString()
        }
    }

     private String render( def obj ) {
         render( "default", obj )
     }
}
