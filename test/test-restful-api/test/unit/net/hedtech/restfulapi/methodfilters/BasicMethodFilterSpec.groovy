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

package net.hedtech.restfulapi.methodfilters

import grails.test.mixin.*
import grails.test.mixin.support.*

import net.hedtech.restfulapi.*
import spock.lang.*


@TestMixin(GrailsUnitTestMixin)
class BasicMethodFilterSpec extends Specification {

    MethodFilter restMethodFilter

    void setup() {
        cleanup()
        restMethodFilter = new BasicMethodFilter()
        restMethodFilter.grailsApplication = grailsApplication
    }

    void cleanup() {
        grailsApplication.config.restfulApi.methodFilter.methodsNotAllowedMap = null
    }

    def "Test missing methodsNotAllowedMap config property"() {
        setup:

        when:
        boolean isNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.CREATE)

        then:
        false == isNotAllowed
    }

    def "Test methodsNotAllowedMap is not a Map"() {
        setup:
        grailsApplication.config.restfulApi.methodFilter.methodsNotAllowedMap = "Not-A-Map"

        when:
        boolean isNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.CREATE)

        then:
        false == isNotAllowed
    }

    def "Test missing resource name"() {
        setup:
        grailsApplication.config.restfulApi.methodFilter.methodsNotAllowedMap =
            [
                "another-resource": []
            ]

        when:
        boolean isNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.CREATE)

        then:
        false == isNotAllowed
    }

    def "Test resource name with no methods specified"() {
        setup:
        grailsApplication.config.restfulApi.methodFilter.methodsNotAllowedMap =
            [
                "my-resource": []
            ]

        when:
        boolean isNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.CREATE)

        then:
        false == isNotAllowed
    }

    def "Test resource name with one method specified"() {
        setup:
        grailsApplication.config.restfulApi.methodFilter.methodsNotAllowedMap =
            [
                "my-resource": [Methods.CREATE]
            ]

        when:
        boolean isShowNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.SHOW)
        boolean isCreateNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.CREATE)
        boolean isUpdateNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.UPDATE)
        boolean isDeleteNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.DELETE)

        then:
        false == isShowNotAllowed
        true == isCreateNotAllowed
        false == isUpdateNotAllowed
        false == isDeleteNotAllowed
    }

    def "Test resource name with multiple methods specified"() {
        setup:
        grailsApplication.config.restfulApi.methodFilter.methodsNotAllowedMap =
            [
                "my-resource": [Methods.CREATE, Methods.UPDATE, Methods.DELETE]
            ]

        when:
        boolean isShowNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.SHOW)
        boolean isCreateNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.CREATE)
        boolean isUpdateNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.UPDATE)
        boolean isDeleteNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.DELETE)

        then:
        false == isShowNotAllowed
        true == isCreateNotAllowed
        true == isUpdateNotAllowed
        true == isDeleteNotAllowed
    }

    def "Test resource name with duplicate methods specified"() {
        setup:
        grailsApplication.config.restfulApi.methodFilter.methodsNotAllowedMap =
            [
                "my-resource": [Methods.CREATE, Methods.UPDATE, Methods.CREATE, Methods.DELETE, Methods.UPDATE]
            ]

        when:
        boolean isShowNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.SHOW)
        boolean isCreateNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.CREATE)
        boolean isUpdateNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.UPDATE)
        boolean isDeleteNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.DELETE)

        then:
        false == isShowNotAllowed
        true == isCreateNotAllowed
        true == isUpdateNotAllowed
        true == isDeleteNotAllowed
    }

    def "Test resource name with method specified not a List"() {
        setup:
        grailsApplication.config.restfulApi.methodFilter.methodsNotAllowedMap =
            [
                "my-resource": Methods.CREATE
            ]

        when:
        boolean isShowNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.SHOW)
        boolean isCreateNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.CREATE)
        boolean isUpdateNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.UPDATE)
        boolean isDeleteNotAllowed = restMethodFilter.isMethodNotAllowed("my-resource", Methods.DELETE)

        then:
        false == isShowNotAllowed
        true == isCreateNotAllowed
        false == isUpdateNotAllowed
        false == isDeleteNotAllowed
    }
}
