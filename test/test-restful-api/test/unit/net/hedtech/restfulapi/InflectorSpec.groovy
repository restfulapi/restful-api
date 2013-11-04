/* ****************************************************************************
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
package net.hedtech.restfulapi

import grails.test.mixin.*

import spock.lang.*

/**
 * Some illustrative tests for using the Inflector class.
 */
class InflectorSpec extends Specification {


    def "Test registered singularize rule"() {
        expect:
        'mouse' == Inflector.singularize('mice')
    }

    def "Test camel case"() {
        expect:
        'camelCase' == Inflector.camelCase('CamelCase')
        'camelCase' == Inflector.camelCase('Camel Case')
        'camelCase' == Inflector.camelCase('Camel-Case')
        'camelCase' == Inflector.camelCase('camel_case')
    }
}
