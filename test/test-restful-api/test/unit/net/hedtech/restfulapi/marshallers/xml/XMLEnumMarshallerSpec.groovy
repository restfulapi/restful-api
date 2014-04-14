/* ***************************************************************************
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
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
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException

import org.junit.Rule
import org.junit.rules.TestName

import org.springframework.beans.BeanWrapper
import org.springframework.web.context.WebApplicationContext

import spock.lang.*


@TestMixin([GrailsUnitTestMixin,ControllerUnitTestMixin])
class XMLEnumMarshallerSpec extends Specification {

    @Rule TestName testName = new TestName()

    def "Test marshalling enum"() {
        setup:
        def marshaller = new DeclarativeBeanMarshaller(
            app:grailsApplication
        )

        register([marshaller, new EnumMarshaller()])
        def bean = new SimpleBeanWithEnum(name:'foo', enumValue:SimpleEnum.VALUE2)

        when:
        def content = render(bean)
        def xml = XML.parse content

        then:
        'foo'    == xml.name.text()
        'VALUE2' == xml.enumValue[0].text()
        0        == xml.enumValue[0].attributes().size()
    }

    private void register( String name, def marshallers ) {
        XML.createNamedConfig("XMLEnumMarshallerSpec:" + testName.getMethodName() + ":$name") { xml ->
            marshallers.each { marshaller ->
                xml.registerObjectMarshaller( marshaller, 100 )
            }
        }
    }

    private void register( def marshallers ) {
        register( "default", marshallers )
    }

    private String render( String name, def obj ) {
        XML.use( "XMLEnumMarshallerSpec:" + testName.getMethodName() + ":$name" ) {
            //for some reason (obj as JSON) doesn't work in parameterized spock tests
            def converter = new XML(obj)
            converter.toString()
        }
    }

    private String render( def obj ) {
        render( "default", obj )
    }

}
