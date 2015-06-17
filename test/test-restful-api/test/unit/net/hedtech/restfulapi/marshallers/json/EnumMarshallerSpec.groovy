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
package net.hedtech.restfulapi.marshallers.json

import grails.converters.JSON
import grails.test.mixin.*
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.test.mixin.support.*
import grails.test.mixin.web.*

import java.beans.PropertyDescriptor
import java.lang.reflect.Field

import net.hedtech.restfulapi.*

import net.hedtech.restfulapi.beans.*

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer
import org.codehaus.groovy.grails.web.json.JSONObject

import org.junit.Rule
import org.junit.rules.TestName

import spock.lang.*

@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin])
class EnumMarshallerSpec extends Specification {

    @Rule TestName testName = new TestName()

    void setup() {
    }

    def "Test marshalling enum"() {
        setup:
        def marshaller = new BeanMarshaller(
            app:grailsApplication
        )
        register([marshaller,new EnumMarshaller()])
        def bean = new SimpleBeanWithEnum(name:'foo', enumValue:SimpleEnum.VALUE2)

        when:
        def content = render(bean)
        def json = JSON.parse content

        then:
        'foo'    == json.name
        "VALUE2" == json.enumValue
    }

    private void register( String name, def marshallers ) {
        JSON.createNamedConfig("EnumMarshallerSpec:" + testName.getMethodName() + ":$name") { xml ->
            marshallers.each { marshaller ->
                xml.registerObjectMarshaller( marshaller, 100 )
            }
        }
    }

    private void register( def marshaller ) {
        register( "default", marshaller )
    }

    private String render( String name, def obj ) {
        JSON.use( "EnumMarshallerSpec:" + testName.getMethodName() + ":$name" ) {
            //for some reason (obj as JSON) doesn't work in parameterized spock tests
            def converter = new JSON(obj)
            converter.toString()
        }
    }

    private String render( def obj ) {
        render( "default", obj )
    }
}
