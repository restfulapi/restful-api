/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.marshallers.xml

import grails.test.mixin.*
import grails.test.mixin.web.*
import spock.lang.*
import net.hedtech.restfulapi.*
import grails.converters.XML
import grails.test.mixin.support.*
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.springframework.web.context.WebApplicationContext
import org.springframework.beans.BeanWrapper
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import grails.test.mixin.domain.DomainClassUnitTestMixin

import net.hedtech.restfulapi.beans.*

import org.junit.Rule
import org.junit.rules.TestName

@TestMixin([GrailsUnitTestMixin,ControllerUnitTestMixin])
class XMLDeclarativeGroovyBeanMarshallerSpec extends Specification {

    @Rule TestName testName = new TestName()

    def "Test excluding fields and properties"() {
        setup:
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication,
            excludedFields:['property','publicField']
        )
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render(bean)
        def xml = XML.parse content

        then:
        0 == xml.property.size()
        0 == xml.publicField.size()
        1 == xml.property2.size()
        1 == xml.publicField2.size()
    }

    def "Test default exclusions"() {
        setup:
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render(bean)
        def xml = XML.parse content

        then:
        0 == xml.password.size()
        0 == xml.'class'.size()
        0 == xml.'metaClass'.size()
    }


    def "Test including fields"() {
        setup:
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication,
            includedFields:['property','publicField']
        )
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render(bean)
        def xml = XML.parse content

        then:
        2     == xml.children().size()
        'foo' == xml.property.text()
        'bar' == xml.publicField.text()
    }

    def "Test that included fields overrides excluded fields"() {
        setup:
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication,
            includedFields:['property','publicField', 'publicField2'],
            excludedFields:['property','publicField']
        )
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar', publicField2:'foos')

        when:
        def content = render(bean)
        def xml = XML.parse content

        then:
        3      == xml.children().size()
        'foo'  == xml.property.text()
        'bar'  == xml.publicField.text()
        'foos' == xml.publicField2.text()
    }

    def "Test alternative names of fields"() {
        setup:
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication,
            fieldNames:['property':'modProperty', 'publicField':'modPublicField']
        )
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render(bean)
        def xml = XML.parse content

        then:
        0 == xml.property.size()
        0 == xml.publicField.size()
        'foo' == xml.modProperty.text()
        'bar' == xml.modPublicField.text()
    }

    def "Test limiting marshalling to a class"() {
        setup:
        def marshaller = new DeclarativeGroovyBeanMarshaller(
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

    def "Test additional field closures"() {
        setup:
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication,
            supportClass:SimpleBean,
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
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render(bean)
        def xml = XML.parse content

        then:
        'foo-bar' == xml.title.text()
        'not it'  == xml.tag.text()
    }

    def "Test additional fields map"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication,
            supportClass:SimpleBean,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ],
            additionalFieldsMap:['a1':'b1','a2':'b2','xml':null,'beanWrapper':null,'grailsApplication':null]
        )
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render(bean)

        then:
        bean              == passedMap['beanWrapper'].getWrappedInstance()
        grailsApplication == passedMap['grailsApplication']
        null              != passedMap['xml']
        'b1'              == passedMap['a1']
        'b2'              == passedMap['a2']
    }

    def "Test map passed to additional fields closures has the derived resource name added"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ]
        )
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render(bean)

        then:
        'simple-beans' == passedMap['resourceName']
    }

    def "Test resourceName is not overridden if specified"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ],
            additionalFieldsMap:['resourceName':'my-beans']
        )
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render(bean)

        then:
        'my-beans' == passedMap['resourceName']
    }

    def "Test metaprogrammed getId() is passed as resourceId to field closures"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ]
        )
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')
        bean.metaClass.getId << {-> 50 }

        when:
        def content = render(bean)

        then:
        50 == passedMap['resourceId']
    }

    def "Test metaprogrammed id property is passed as resourceId to field closures"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ]
        )
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')
        bean.metaClass.id = 51

        when:
        def content = render(bean)

        then:
        51 == passedMap['resourceId']
    }

    def "Test bean with public id field has value passed to field closures"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ]
        )
        register( marshaller )
        SimpleBean bean = new SimpleBeanWithIdField(property:'foo', publicField:'bar')
        bean.id = 52

        when:
        def content = render(bean)

        then:
        52 == passedMap['resourceId']
    }

    def "Test bean with id property has value passed to field closures"() {
        setup:
        def passedMap = [:]
        def marshaller = new DeclarativeGroovyBeanMarshaller(
            app:grailsApplication,
            additionalFieldClosures:[
                {map ->
                    passedMap.putAll( map )
                }
            ]
        )
        register( marshaller )
        SimpleBean bean = new SimpleBeanWithIdProperty(property:'foo', publicField:'bar')
        bean.id = 53

        when:
        def content = render(bean)

        then:
        53 == passedMap['resourceId']
    }

    private void register( String name, def marshaller ) {
        XML.createNamedConfig( "DeclarativeGroovyBeanMarshallerSpec:" + testName + ":$name" ) { xml ->
            xml.registerObjectMarshaller( marshaller, 100 )
        }
    }

    private void register( def marshaller ) {
        register( "default", marshaller )
    }

    private String render( String name, def obj ) {
        XML.use( "DeclarativeGroovyBeanMarshallerSpec:" + testName + ":$name" ) {
            return (obj as XML) as String
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