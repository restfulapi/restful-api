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
