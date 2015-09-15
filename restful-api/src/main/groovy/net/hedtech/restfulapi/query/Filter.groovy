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

package net.hedtech.restfulapi.query

import grails.gorm.DetachedCriteria

import net.hedtech.restfulapi.Inflector

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import grails.core.GrailsApplication
import grails.core.GrailsDomainClass
import grails.core.GrailsDomainClassProperty


/**
 * Represents a filter to be used when retrieving a list of resource instances.
 * This class provides a factory method that will return a list of Filter instances
 * extracted from the Grails params map.  To successfully extract Filter
 * instances, the URL parsed by Grails must include query parameters as illustrated
 * in the following example:
 * things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
 * filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
 **/
class Filter {

    // Note the URL filters are not (yet) internationalizable
    private static def filterRE = /filter\[([0-9]+)\]\[(field|operator|value|type)\]=(.*)/

    private static final Range ALIASES      = 'a'..'p'
    private static List FILTER_TERMS        = ['field', 'operator', 'value']
    private static List STRING_OPERATORS    = ['eq', 'equals', 'contains']
    private static List NUMERIC_OPERATORS   = ['eq', 'equals', 'lt', 'gt', 'le', 'ge']
    private static List DATE_OPERATORS      = ['eq', 'equals', 'lt', 'gt', 'le', 'ge']
    private static List SUPPORTED_OPERATORS = STRING_OPERATORS + NUMERIC_OPERATORS + DATE_OPERATORS

    private static List NUMERIC_TYPES       = ['num', 'number']
    private static List STRING_TYPES        = ['string']
    private static List DATE_TYPES          = ['date']
    private static List SUPPORTED_TYPES     = STRING_TYPES + NUMERIC_TYPES + DATE_TYPES

    protected static final Log log = LogFactory.getLog(Filter.class)

    public String field
    public String operator
    public def    value
    public String type // if null we'll assume the value is of type = String
    public GrailsDomainClassProperty persistentProperty
    public boolean isAssociation
    public boolean isMany
    private boolean valid = false


    public boolean isValid() {
        if (valid) return true
        if (!field || !operator || !value || !persistentProperty) return false
        if (!(SUPPORTED_OPERATORS.contains(operator))) return false
        if (('contains' == operator) && (null == value)) return false
        if (type && !(SUPPORTED_TYPES.contains(type))) return false
        if (isNumeric() && !(NUMERIC_OPERATORS.contains(operator))) return false
        if (isDate() && !(DATE_OPERATORS.contains(operator))) return false
        if (isMany) return false // we don't support filtering on collections (yet?)
        true
    }


    public boolean isNumeric() { NUMERIC_TYPES.contains(type) }
    public boolean isDate()    { DATE_TYPES.contains(type) }

    public String toString() {
        "Filter[field=$field,operator=$operator,value=$value,type=${persistentProperty?.type},isAssociation=$isAssociation,isMany=$isMany]"
    }


// ------------------------------ Static Methods -------------------------------


    /**
     * Returns a list of Filters extracted from 'filter' query parameters.
     **/
    public static List extractFilters( GrailsApplication application, Map params ) {

        if (!params.pluralizedResourceName) throw new RuntimeException("params map must contain 'pluralizedResourceName'")

        GrailsDomainClass domainClass = params.domainClass ?: getGrailsDomainClass( application, params.pluralizedResourceName )
        GrailsDomainClassProperty[] properties = domainClass.getProperties()

        // Now that we've created filters from the params map, we'll augment them
        // with a bit more information
        Map filters = createFilters( params )
        filters.each {
            def filter = it.value
            filter.persistentProperty = findProperty( properties, filter.field )
            if (filter.persistentProperty) {
                filter.isAssociation = isDomainClassProperty( filter.persistentProperty )
                filter.isMany = isCollection( filter.persistentProperty.type )
            }
        }
        filters.sort().collect { it.value }
    }


    private static Map createFilters( Map params ) {

        def filters = [:].withDefault { new Filter() }
        def matcher

        params.each {

            if (it.key.startsWith('filter')) {
                matcher = ( it =~ filterRE )
                if (matcher.count) {
                    filters[matcher[0][1]]."${matcher[0][2]}" = matcher[0][3]
                }
            }
            // the resource may be a 'nested resource' - if so, we'll add a filter
            else if (it.key == 'parentPluralizedResourceName') {
                filters['-1'].field = "${getDomainClassName(it.value, false)}"
                filters['-1'].operator = '='
                filters['-1'].value = params.parentId
                filters['-1'].valid = true
            }
        }
        filters
    }


    public static String getDomainClassName(pluralizedResourceName, boolean capitalizeFirstLetter = true) {
        def singularizedName = Inflector.singularize(pluralizedResourceName)
        Inflector.camelCase(singularizedName, capitalizeFirstLetter)
    }


    public static GrailsDomainClass getGrailsDomainClass(application, pluralizedResourceName) {
        def className = getDomainClassName(pluralizedResourceName)
        application.domainClasses.find { it.clazz.simpleName == className }
    }


    public static isCollection(Class type) {
        [Collection, Object[]].any { it.isAssignableFrom(type) }
    }


    public static GrailsDomainClassProperty findProperty(GrailsDomainClassProperty[] properties, String name) {
        properties.find { it.name == name }
    }


    public static boolean isDomainClassProperty(GrailsDomainClassProperty property) {
        property?.getReferencedDomainClass() != null
    }

}
