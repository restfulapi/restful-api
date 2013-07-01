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
import org.codehaus.groovy.grails.web.json.JSONObject
import grails.test.mixin.domain.DomainClassUnitTestMixin
import java.beans.PropertyDescriptor
import java.lang.reflect.Field
import net.hedtech.restfulapi.beans.*

import org.junit.Rule
import org.junit.rules.TestName

@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin])
class XMLGroovyBeanMarshallerSpec extends Specification {

    @Rule TestName testName = new TestName()

    void setup() {
    }

    def "Test field access"() {
        setup:
        def marshaller = new GroovyBeanMarshaller(
            app:grailsApplication
        )
        register(marshaller)
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')


        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'foo' == xml.property.text()
        'bar' == xml.publicField.text()
        0     == xml.protectedField.size()
        0     == xml.privateField.size()
        0     == xml.transientField.size()
        0     == xml.staticField.size()
    }

    def "Test excluding fields"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.excludesClosure = {Object value-> ['property','publicField'] }
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0 == xml.property.size()
        0 == xml.publicField.size()
    }

    def "Test default exclusions"() {
        setup:
        def marshaller = new GroovyBeanMarshaller(
            app:grailsApplication
        )
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0 == xml.password.size()
        0 == xml.'class'.size()
        0 == xml.'metaClass'.size()
    }


    def "Test including fields"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object value-> [ 'property', 'publicField'] }
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'foo' == xml.property.text()
        'bar' == xml.publicField.text()
        0 == xml.property2.size()
        0 == xml.publicField2.size()
    }

    def "Test that included fields overrides excluded fields"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        def exclusionCalled
        marshaller.includesClosure = {Object value-> [ 'property', 'property2', 'publicField','publicField2'] }
        marshaller.excludesClosure = {Object value->
            exclusionCalled = true
            [ 'property', 'publicField']
        }
        register( marshaller )
        SimpleBean bean = new SimpleBean(
            property:'foo', publicField:'bar',
            property2:'prop2', publicField2:'field2')

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'foo'    == xml.property.text()
        'bar'    == xml.publicField.text()
        'prop2'  == xml.property2.text()
        'field2' == xml.publicField2.text()
        4        == xml.children().size()
    }

    def "Test special processing of properties"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.processPropertyClosure = {
            BeanWrapper beanWrapper, PropertyDescriptor property, XML xml ->
            if (property.getName() == 'property') {
                xml.startNode('modProperty')
                xml.convertAnother(beanWrapper.getPropertyValue(property.getName()))
                xml.end()
                return false
            } else {
                return true
            }
        }
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0     == xml.property.size()
        'foo' == xml.modProperty.text()
    }

    def "Test special processing of fields"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.processFieldClosure = {
            Object obj, Field field, XML xml ->
            if (field.getName() == 'publicField') {
                xml.startNode('modPublicField')
                xml.convertAnother(field.get(obj))
                xml.end()
                return false
            } else {
                return true
            }
        }
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0     == xml.publicField.size()
        'bar' == xml.modPublicField.text()
    }

    def "Test alternative names"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.substitutionNames = ['property':'modProperty','publicField':'modPublicField']
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0     == xml.property.size()
        0     == xml.publicField.size()
        'foo' == xml.modProperty.text()
        'bar' == xml.modPublicField.text()
    }

    def "Test additional fields"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.additionalFieldsClosure = {BeanWrapper wrapper, XML xml ->
            xml.startNode('additionalProp')
            xml.convertAnother("some value")
            xml.end()
        }
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'some value' == xml.additionalProp.text()
    }

    def "Test overriding available properties"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.availablePropertiesClosure = {BeanWrapper wrapper ->
            [wrapper.getPropertyDescriptor('property2')]
        }
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0 == xml.property.size()
        1 == xml.property2.size()
    }


    def "Test overriding available fields"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.availableFieldsClosure = {Object value ->
            [value.getClass().getDeclaredField('publicField2')]
        }
        register( marshaller )
        SimpleBean bean = new SimpleBean(property:'foo', publicField:'bar')

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0 == xml.publicField.size()
        1 == xml.publicField2.size()
    }

    private void register( String name, def marshaller ) {
        XML.createNamedConfig( "GroovyBeanMarshallerSpec:" + testName + ":$name" ) { xml ->
            xml.registerObjectMarshaller( marshaller, 100 )
        }
    }

    private void register( def marshaller ) {
        register( "default", marshaller )
    }

    private String render( String name, def obj ) {
        XML.use( "GroovyBeanMarshallerSpec:" + testName + ":$name" ) {
            return (obj as XML) as String
        }
    }

    private String render( def obj ) {
        render( "default", obj )
    }

    class TestMarshaller extends GroovyBeanMarshaller {
        def availablePropertiesClosure
        def availableFieldsClosure
        def excludesClosure
        def includesClosure
        def processPropertyClosure
        def processFieldClosure
        def substitutionNames = [:]
        def additionalFieldsClosure

        @Override
        protected List<PropertyDescriptor> getAvailableProperties(BeanWrapper beanWrapper) {
            if (availablePropertiesClosure != null) {
                availablePropertiesClosure.call(beanWrapper)
            } else {
                super.getAvailableProperties(beanWrapper)
            }
        }

        @Override
        protected List<Field> getAvailableFields(Object value) {
            if (availableFieldsClosure != null) {
                availableFieldsClosure.call(value)
            } else {
                super.getAvailableFields(value)
            }
        }

        @Override
        protected List<String> getExcludedFields(Object value) {
            if (excludesClosure != null) {
                return excludesClosure.call(value)
            } else {
                return super.getExcludedFields(value)
            }
        }

        @Override
        protected List<String> getIncludedFields(Object value) {
            if (includesClosure != null) {
                return includesClosure.call(value)
            } else {
                return super.getExcludedFields(value)
            }
        }

        protected boolean processProperty(BeanWrapper beanWrapper,
                                          PropertyDescriptor property,
                                          XML xml) {
            if (processPropertyClosure != null) {
                processPropertyClosure.call(beanWrapper, property, xml)
            } else {
                super.processProperty(beanWrapper, property, xml)
            }
        }

        protected boolean processField(Object obj,
                                       Field field,
                                       XML xml) {
            if (processFieldClosure != null) {
                processFieldClosure.call(obj, field, xml)
            } else {
                super.processField(obj, field, xml)
            }
        }

        protected String getSubstitutionName(BeanWrapper beanWrapper, PropertyDescriptor property) {
            def name = substitutionNames[property.getName()]
            if (name) {
                name
            } else {
                null
            }
        }

        /**
         * Return the name to use when marshalling the field, or
         * null if the field name should be used as-is.
         * @return the name to use when marshalling the field,
         *         or null if the domain field name should be used
         */
        protected String getSubstitutionName(Object value, Field field) {
            def name = substitutionNames[field.getName()]
            if (name) {
                name
            } else {
                null
            }
        }

        protected void processAdditionalFields(BeanWrapper beanWrapper, XML xml) {
            if (additionalFieldsClosure != null) {
                additionalFieldsClosure.call(beanWrapper, xml)
            } else {
                super.processAdditionalFields(beanWrapper, xml)
            }
        }
    }
}