/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 

package net.hedtech.restfulapi

import grails.test.mixin.*
import org.junit.*


/**
 * Some illustrative tests for using the Inflector class.
 */
class InflectorTests {


	void testRegisteredSingularizeRule() {
		assert 'mouse' == Inflector.singularize('mice')
	}

    void testCamelCase() {

    	assert 'camelCase' == Inflector.camelCase('CamelCase')
    	assert 'camelCase' == Inflector.camelCase('Camel Case')
    	assert 'camelCase' == Inflector.camelCase('Camel-Case')
    	assert 'camelCase' == Inflector.camelCase('camel_case')
    }
}
