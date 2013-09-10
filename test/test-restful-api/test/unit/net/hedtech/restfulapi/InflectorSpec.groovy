/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
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
