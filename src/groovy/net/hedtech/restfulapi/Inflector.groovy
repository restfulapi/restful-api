/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 

package net.hedtech.restfulapi

import org.modeshape.common.text.Inflector as Inf


/**
 * Provide inflector methods. 
 * Includes pluralize, singularize, camelCase, hyphenate, etc.
 * Note: This class delegates to a 3rd party 'Inflector' class.
 **/
class Inflector {

    private static def inflector = Inf.instance


    public static void addSingularize(String plural, String singular) {
        inflector.addSingularize("mice\$", "\$1mouse")   
    }


    public static String asPropertyName(String source) {
        def s = inflector.singularize(source)
        Inflector.camelCase(s)
    }


    public static String camelCase(String source, boolean capitalize = false) {
        inflector.camelCase(source, capitalize, ' ' as char, '_' as char, '-' as char)
    }


    public static String singularize(String str) {
        inflector.singularize(str)
    }


    public static String pluralize(String str) {
        inflector.pluralize(str)
    }


    public static String hyphenate(String str) {
        inflector.underscore(str).replaceAll("_", "-")
    }
}
