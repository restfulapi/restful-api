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
import org.codehaus.groovy.grails.web.json.JSONObject

import java.beans.PropertyDescriptor
import java.lang.reflect.Field

import org.junit.Rule
import org.junit.rules.TestName

import org.springframework.beans.BeanWrapper
import org.springframework.web.context.WebApplicationContext

import spock.lang.*


@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin])
class XMLBeanMarshallerSpec extends Specification {

    @Rule TestName testName = new TestName()

    void setup() {
    }

    def "Test that collections and maps are not supported by default"() {
        setup:
        def marshaller = new BeanMarshaller(
            app:grailsApplication
        )

        expect:
        false == marshaller.supports(new DummyCollection())
        false == marshaller.supports(new DummyMap())
    }

    @Unroll
    def "Test field access"() {
        setup:
        def marshaller = new BeanMarshaller(
            app:grailsApplication
        )
        register(marshaller)

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

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test excluding fields"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.excludesClosure = {Object value-> ['property','publicField'] }
        register( marshaller )

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0 == xml.property.size()
        0 == xml.publicField.size()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test default exclusions"() {
        setup:
        def marshaller = new BeanMarshaller(
            app:grailsApplication
        )
        register( marshaller )

        when:
        def content = render( bean )
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
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object value-> [ 'property', 'publicField'] }
        register( marshaller )

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'foo' == xml.property.text()
        'bar' == xml.publicField.text()
        0 == xml.property2.size()
        0 == xml.publicField2.size()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test including no fields"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object value-> []}
        register( marshaller )

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0 == xml.children.size()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test require included fields"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object value-> [ 'property', 'publicField', 'missingField1', 'missingField2'] }
        marshaller.requireIncludedFieldsClosure = {Object o -> true}
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

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'foo'    == xml.property.text()
        'bar'    == xml.publicField.text()
        'prop2'  == xml.property2.text()
        'field2' == xml.publicField2.text()
        4        == xml.children().size()

        where:
        bean << [new SimpleBean( property:'foo', publicField:'bar', property2:'prop2', publicField2:'field2'),
                 new SimpleJavaBean( property:'foo', publicField:'bar', property2:'prop2', publicField2:'field2')]
    }

    @Unroll
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

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0     == xml.property.size()
        'foo' == xml.modProperty.text()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
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

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0     == xml.publicField.size()
        'bar' == xml.modPublicField.text()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test alternative names"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.substitutionNames = ['property':'modProperty','publicField':'modPublicField']
        register( marshaller )

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0     == xml.property.size()
        0     == xml.publicField.size()
        'foo' == xml.modProperty.text()
        'bar' == xml.modPublicField.text()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
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

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'some value' == xml.additionalProp.text()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test overriding available properties"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.availablePropertiesClosure = {BeanWrapper wrapper ->
            [wrapper.getPropertyDescriptor('property2')]
        }
        register( marshaller )

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0 == xml.property.size()
        1 == xml.property2.size()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test overriding available fields"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.availableFieldsClosure = {Object value ->
            [value.getClass().getDeclaredField('publicField2')]
        }
        register( marshaller )

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        0 == xml.publicField.size()
        1 == xml.publicField2.size()

        where:
        bean << [new SimpleBean(property:'foo', publicField:'bar'),
                 new SimpleJavaBean(property:'foo', publicField:'bar')]
    }

    @Unroll
    def "Test collection property has array attribute"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object value-> [ 'listProperty' ] }
        register( marshaller )
        bean.listProperty = ['abc','123']

        when:
        def content = render( bean )
        def xml = XML.parse content
        def values = []
        xml.listProperty.children().each {
            values.add it.text()
        }

        then:
        'true'        == xml.listProperty.@array.text()
        2             == xml.listProperty.children().size()
        ['abc','123'] == values

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test collection field has array attribute"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object value-> [ 'listField' ] }
        register( marshaller )
        bean.listField = ['abc','123']

        when:
        def content = render( bean )
        def xml = XML.parse content
        def values = []
        xml.listField.children().each {
            values.add it.text()
        }

        then:
        'true'        == xml.listField.@array.text()
        2             == xml.listField.children().size()
        ['abc','123'] == values

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test map property has map attribute"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object value-> [ 'mapProperty' ] }
        register( marshaller )
        bean.mapProperty = ['abc':'123']

        when:
        def content = render( bean )
        def xml = XML.parse content
        def values = [:]
        xml.mapProperty.children().each {
            values.put(it.@key.text(), it.text())
        }

        then:
        'true'        == xml.mapProperty.@map.text()
        1             == xml.mapProperty.children().size()
        ['abc':'123'] == values

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test map field has map attribute"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object value-> [ 'mapField' ] }
        register( marshaller )
        bean.mapField = ['abc':'123']

        when:
        def content = render( bean )
        def xml = XML.parse content
        def values = [:]
        xml.mapField.children().each {
            values.put(it.@key.text(), it.text())
        }

        then:
        'true'        == xml.mapField.@map.text()
        1             == xml.mapField.children().size()
        ['abc':'123'] == values

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test collection property with complex objects"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object o -> ['listProperty','lastName','firstName']}
        register( marshaller )
        bean.listProperty = [new MarshalledThingContributor(lastName:'Smith',firstName:"John")]

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'true'  == xml.listProperty.@array.text()
        1       == xml.listProperty.children().size()
        'Smith' == xml.listProperty.children()[0].lastName.text()
        'John'  == xml.listProperty.children()[0].firstName.text()

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test collection field with complex objects"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object o -> ['listField','lastName','firstName']}
        register( marshaller )
        bean.listField = [new MarshalledThingContributor(lastName:'Smith',firstName:"John")]

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'true'  == xml.listField.@array.text()
        1       == xml.listField.children().size()
        'Smith' == xml.listField.children()[0].lastName.text()
        'John'  == xml.listField.children()[0].firstName.text()

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test map property"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object value-> [ 'mapProperty' ] }
        register( marshaller )
        bean.mapProperty = ['a':'1','b':'2']

        when:
        def content = render( bean )
        def xml = XML.parse content
        def map = [:]
        xml.mapProperty.children().each() {
            map.put(it.@key.text(), it.text())
        }

        then:
        'true'            == xml.mapProperty.@map.text()
        2                 == xml.mapProperty.children().size()
        ['a':'1','b':'2'] == map

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test map field"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object value-> [ 'mapField' ] }
        register( marshaller )
        bean.mapField = ['a':'1','b':'2']

        when:
        def content = render( bean )
        def xml = XML.parse content
        def map = [:]
        xml.mapField.children().each() {
            map.put(it.@key.text(), it.text())
        }

        then:
        'true'            == xml.mapField.@map.text()
        2                 == xml.mapField.children().size()
        ['a':'1','b':'2'] == map

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test map property with complex objects"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object o -> ['mapProperty','lastName','firstName']}
        register( marshaller )
        bean.mapProperty = ['a':new MarshalledThingContributor(lastName:'Smith',firstName:"John")]

        when:
        def content = render( bean )
        def xml = XML.parse content
        def value = xml.mapProperty.children()[0]

        then:
        'true'  == xml.mapProperty.@map.text()
        1       == xml.mapProperty.children().size()
        'Smith' == value.lastName.text()
        'John'  == value.firstName.text()

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test map field with complex objects"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object o -> ['mapField','lastName','firstName']}
        register( marshaller )
        bean.mapField = ['a':new MarshalledThingContributor(lastName:'Smith',firstName:"John")]

        when:
        def content = render( bean )
        def xml = XML.parse content
        def value = xml.mapField.children()[0]

        then:
        'true'  == xml.mapField.@map.text()
        1       == xml.mapField.children().size()
        'Smith' == value.lastName.text()
        'John'  == value.firstName.text()

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test null collection property has null attribute"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object o -> ['listProperty']}
        register( marshaller )

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'true'  == xml.listProperty.@null.text()
        0       == xml.listProperty.children().size()

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test null collection field has null attribute"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object o -> ['listField']}
        register( marshaller )

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'true'  == xml.listField.@null.text()
        0       == xml.listField.children().size()

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test null map property has null attribute"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object o -> ['mapProperty']}
        register( marshaller )

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'true'  == xml.mapProperty.@null.text()
        0       == xml.mapProperty.children().size()

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test null map field has null attribute"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = {Object o -> ['mapField']}
        register( marshaller )

        when:
        def content = render( bean )
        def xml = XML.parse content

        then:
        'true'  == xml.mapField.@null.text()
        0       == xml.mapField.children().size()

        where:
        bean << [new SimpleBean(),
                 new SimpleJavaBean()]
    }

    @Unroll
    def "Test a field and property with the same name are marshalled once"() {
        setup:
        def marshaller = new TestMarshaller(
            app:grailsApplication
        )
        marshaller.includesClosure = { Object value -> ['propertyAndField']}
        register( marshaller )

        when:
        def content = render( bean )

        then:
        1 == content.count( "<propertyAndField>" )

        where:
        bean << [new SimpleBean(propertyAndField:'foo'),
                 new SimpleJavaBean(propertyAndField:'foo')]
    }

    private void register( String name, def marshaller ) {
        XML.createNamedConfig( "XMLBeanMarshallerSpec:" + testName.getMethodName() + ":$name" ) { xml ->
            xml.registerObjectMarshaller( marshaller, 100 )
        }
    }

    private void register( def marshaller ) {
        register( "default", marshaller )
    }

    private String render( String name, def obj ) {
        XML.use( "XMLBeanMarshallerSpec:" + testName.getMethodName() + ":$name" ) {
            //for some reason (obj as JSON) doesn't work in parameterized spock tests
            def converter = new XML(obj)
            converter.toString()
        }
    }

    private String render( def obj ) {
        render( "default", obj )
    }

    class TestMarshaller extends BeanMarshaller {
        def availablePropertiesClosure
        def availableFieldsClosure
        def excludesClosure
        def includesClosure
        def requireIncludedFieldsClosure
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
                return super.getIncludedFields(value)
            }
        }

        @Override
        protected boolean requireIncludedFields(Object value) {
            if (requireIncludedFieldsClosure != null) {
                return requireIncludedFieldsClosure.call(value)
            } else {
                return super.requireIncludedFields(value)
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
