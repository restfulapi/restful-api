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
import net.hedtech.restfulapi.beans.*

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer

import org.junit.Rule
import org.junit.rules.TestName

import org.springframework.beans.BeanWrapper
import org.springframework.web.context.WebApplicationContext

import spock.lang.*


@TestMixin([GrailsUnitTestMixin,ControllerUnitTestMixin])
class XMLDeclarativeBeanMarshallerSpec extends Specification {

    @Rule TestName testName = new TestName()

    @Unroll
    def "Test default element name"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication
        )

        register( marshaller )

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        elementName  == xml.name()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
        elementName << ['simpleBean','simpleJavaBean']
    }

    @Unroll
    def "Test overidden element name"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            elementName:'Bean'
        )

        register( marshaller )

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'Bean'  == xml.name()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

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
        def xml = XML.parse content

        then:
        0 == xml.property.size()
        0 == xml.publicField.size()
        1 == xml.property2.size()
        1 == xml.publicField2.size()

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
        def xml = XML.parse content

        then:
        0 == xml.password.size()
        0 == xml.'class'.size()
        0 == xml.'metaClass'.size()

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
        def xml = XML.parse content

        then:
        2     == xml.children().size()
        'foo' == xml.property.text()
        'bar' == xml.publicField.text()

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
        def xml = XML.parse content

        then:
        0 == xml.children().size()

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
        Exception e = thrown()
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
        def xml = XML.parse content

        then:
        3      == xml.children().size()
        'foo'  == xml.property.text()
        'bar'  == xml.publicField.text()
        'foos' == xml.publicField2.text()

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
        def xml = XML.parse content

        then:
        0 == xml.property.size()
        0 == xml.publicField.size()
        'foo' == xml.modProperty.text()
        'bar' == xml.modPublicField.text()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test limiting marshalling to a class"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            supportClass:bean.class
        )
        int counter = 0
        register( marshaller )
        Thing thing = new Thing( code:'BB', description:'bb thing' )

        expect:
        marshaller.supports( bean )
        !marshaller.supports( thing )

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test additional field closures"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            supportClass:bean.class,
            additionalFieldClosures:[
                {map ->
                    def xml = map['xml']
                    def beanWrapper = map['beanWrapper']
                    Object val = beanWrapper.getWrappedInstance()
                    xml.startNode('title')
                    xml.convertAnother(beanWrapper.getPropertyValue('property') + "-" + val.publicField)
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

        when:
        def content = render(bean)
        def xml = XML.parse content

        then:
        'foo-bar' == xml.title.text()
        'not it'  == xml.tag.text()

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
            additionalFieldsMap:['a1':'b1','a2':'b2','xml':null,'beanWrapper':null,'grailsApplication':null]
        )
        register( marshaller )

        when:
        def content = render(bean)

        then:
        bean              == passedMap['beanWrapper'].getWrappedInstance()
        grailsApplication == passedMap['grailsApplication']
        null              != passedMap['xml']
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

        resourceName << ['simple-beans','simple-java-beans']
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

    @Unroll
    def "Test including only non-null fields"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            marshallNullFields:false
        )
        register(marshaller)

        when:
        def content = render(bean)
        def xml = XML.parse content

        then:
        'foo' == xml.property.text()
        'bar' == xml.publicField.text()
        0     == xml.property2.size()
        0     == xml.publicField2.size()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar', property2:null, publicField2:null),
                 new SimpleJavaBean(property:'foo', publicField:'bar', property2:null, publicField2:null)]
    }
    @Unroll

    def "Test including non-null fields on a per-field basis"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            marshalledNullFields:['property2':false, 'publicField2':false]
        )
        register( marshaller )

        when:
        def content = render(bean)
        def xml = XML.parse content

        then:
        'foo'  == xml.property.text()
        'bar'  == xml.publicField.text()
        'true' == xml.listProperty.'@null'.text()
        'true' == xml.listField.'@null'.text()
        0      == xml.property2.size()
        0      == xml.publicField2.size()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar', property2:null, publicField2:null, listProperty:null, listField: null),
                 new SimpleJavaBean(property:'foo', publicField:'bar', property2:null, publicField2:null, listProperty:null, listField: null)]
    }

    def "Test including non-null fields on a per-field basis overrides default"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication,
            marshallNullFields:false,
            marshalledNullFields:['property2':true, 'publicField2':true]
        )
        register(marshaller)
        when:
        def content = render(bean)
        def xml = XML.parse content

        then:
        'foo'  == xml.property.text()
        'bar'  == xml.publicField.text()
        'true' == xml.property2.'@null'.text()
        'true' == xml.publicField2.'@null'.text()
        0      == xml.listProperty.size()
        0      == xml.listField.size()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar', listProperty:null, listField: null),
                 new SimpleJavaBean(property:'foo', publicField:'bar', listProperty:null, listField: null)]
    }


    private void register( String name, def marshaller ) {
        XML.createNamedConfig( "XMLDeclarativeBeanMarshallerSpec:" + testName.getMethodName() + ":$name" ) { xml ->
            xml.registerObjectMarshaller( marshaller, 100 )
        }
    }

    private void register( def marshaller ) {
        register( "default", marshaller )
    }

    private String render( String name, def obj ) {
        XML.use( "XMLDeclarativeBeanMarshallerSpec:" + testName.getMethodName() + ":$name" ) {
            //for some reason (obj as JSON) doesn't work in parameterized spock tests
            def converter = new XML(obj)
            converter.toString()
        }
    }

    private String render( def obj ) {
        render( "default", obj )
    }

    static class SimpleBeanWithIdField extends SimpleBean {
        public long id
    }

    static class SimpleBeanWithIdProperty extends SimpleBean {
        long id
    }
}
