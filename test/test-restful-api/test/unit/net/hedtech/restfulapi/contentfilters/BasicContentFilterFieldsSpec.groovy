/* ****************************************************************************
 * Copyright 2016 Ellucian Company L.P. and its affiliates.
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

package net.hedtech.restfulapi.contentfilters

import grails.test.mixin.*
import grails.test.mixin.support.*

import net.hedtech.restfulapi.*
import spock.lang.*


@TestMixin(GrailsUnitTestMixin)
class BasicContentFilterFieldsSpec extends Specification {

    ContentFilterFields restContentFilterFields

    void setup() {
        cleanup()
        restContentFilterFields = new BasicContentFilterFields()
        restContentFilterFields.grailsApplication = grailsApplication
    }

    void cleanup() {
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap = null
    }

    def "Test missing fieldPatternsMap config property"() {
        setup:

        when:
        List fieldPatterns = restContentFilterFields.retrieveFieldPatterns("my-resource")

        then:
        null != fieldPatterns
        true == fieldPatterns instanceof List
        0 == fieldPatterns.size()
    }

    def "Test fieldPatternsMap is not a Map"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap = "Not-A-Map"

        when:
        List fieldPatterns = restContentFilterFields.retrieveFieldPatterns("my-resource")

        then:
        null != fieldPatterns
        true == fieldPatterns instanceof List
        0 == fieldPatterns.size()
    }

    def "Test missing resource name"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
            [
                    "another-resource": []
            ]

        when:
        List fieldPatterns = restContentFilterFields.retrieveFieldPatterns("my-resource")

        then:
        null != fieldPatterns
        true == fieldPatterns instanceof List
        0 == fieldPatterns.size()
    }

    def "Test resource name with no field patterns"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": []
                ]

        when:
        List fieldPatterns = restContentFilterFields.retrieveFieldPatterns("my-resource")

        then:
        null != fieldPatterns
        true == fieldPatterns instanceof List
        0 == fieldPatterns.size()
    }

    def "Test resource name with one field pattern"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["name"]
                ]

        when:
        List fieldPatterns = restContentFilterFields.retrieveFieldPatterns("my-resource")

        then:
        null != fieldPatterns
        true == fieldPatterns instanceof List
        1 == fieldPatterns.size()
        "name" == fieldPatterns.get(0)
    }

    def "Test resource name with multiple field patterns sorted"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["name", "code", "desc"]
                ]

        when:
        List fieldPatterns = restContentFilterFields.retrieveFieldPatterns("my-resource")

        then:
        null != fieldPatterns
        true == fieldPatterns instanceof List
        3 == fieldPatterns.size()
        // field patterns are returned in sorted order
        "code" == fieldPatterns.get(0)
        "desc" == fieldPatterns.get(1)
        "name" == fieldPatterns.get(2)
    }

    def "Test resource name with duplicate field patterns removed"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["name", "code", "name", "desc", "code"]
                ]

        when:
        List fieldPatterns = restContentFilterFields.retrieveFieldPatterns("my-resource")

        then:
        null != fieldPatterns
        true == fieldPatterns instanceof List
        3 == fieldPatterns.size()
        // field patterns are returned in sorted order
        "code" == fieldPatterns.get(0)
        "desc" == fieldPatterns.get(1)
        "name" == fieldPatterns.get(2)
    }

    def "Test resource name with field pattern is not a List"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": "name"
                ]

        when:
        List fieldPatterns = restContentFilterFields.retrieveFieldPatterns("my-resource")

        then:
        null != fieldPatterns
        true == fieldPatterns instanceof List
        1 == fieldPatterns.size()
        "name" == fieldPatterns.get(0)
    }
}
