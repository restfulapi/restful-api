/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 

package net.hedtech.restfulapi

import grails.test.mixin.*
import org.junit.*

import org.modeshape.common.text.Inflector


/**
 * Some illustrative tests for using the Inflector class.
 */
class InflectorTests {


	void testRegisteredSingularizeRule() {
		assert 'mouse' == Inflector.instance.singularize('mice')
	}

    void testCamelCase() {

        def inflectorCamelCase = { String text, boolean capitalizeFirst, char... delimChars ->
            Inflector.instance.camelCase( text, capitalizeFirst, delimChars )
        }

        def lowerCC = inflectorCamelCase.rcurry(false, ' ' as char, '-' as char)
        def upperCC = inflectorCamelCase.rcurry(true, ' ' as char, '-' as char)

    	def fix = Inflector.instance
    	assert 'camelCase' == lowerCC('CamelCase')
    	assert 'CamelCase' == upperCC('CamelCase')
    	assert 'camelCase' == lowerCC('Camel Case')
    	assert 'CamelCase' == upperCC('Camel Case')
    	assert 'camelCase' == lowerCC('Camel-Case')
    	assert 'CamelCase' == upperCC('Camel-Case')
    	assert 'camelCase' == lowerCC('camel_case')
    	assert 'CamelCase' == upperCC('camel_case')
    }
}
