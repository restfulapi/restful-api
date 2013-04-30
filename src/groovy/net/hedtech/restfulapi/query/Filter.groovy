/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.query

import grails.gorm.DetachedCriteria

import net.hedtech.restfulapi.Inflector

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty


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

    private static def filterRE = /filter\[([0-9]+)\]\[(field|operator|value)\]=(.*)/
    private static final Range ALIASES = 'a'..'p'
    private static List FILTER_TERMS = ['field', 'operator', 'value']
    private static List SUPPORTED_OPERATORS = ['eq', 'equals', 'contains']

    protected static final Log log = LogFactory.getLog(Filter.class)

    public String field
    public String operator
    public def value
    public GrailsDomainClassProperty persistentProperty
    public boolean isAssociation
    public boolean isMany
    private boolean valid = false


    public boolean isValid() {
        if (valid) return true
        if (!field || !operator || !value || !persistentProperty) return false
        if (!(SUPPORTED_OPERATORS.contains(operator))) return false
        true
    }

    public String toString() {
        "Filter[field=$field,operator=$operator,value=$value,type=${persistentProperty?.type},isAssociation=$isAssociation,isMany=$isMany]"
    }


// ------------------------------ Static Methods -------------------------------


    /**
     * Returns a list of Filters extracted from 'filter' query parameters.
     **/
    public static List extractFilters( GrailsApplication application, Map params ) {

        if (!params.pluralizedResourceName) throw new RuntimeException("params map must contain 'pluralizedResourceName'")

        GrailsDomainClass domainClass = getGrailsDomainClass(application, params.pluralizedResourceName)
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
