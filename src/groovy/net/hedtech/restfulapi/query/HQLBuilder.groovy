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
 * A builder that creates an HQL statement along with a map of named parameters,
 * ready for execution.
 **/
class HQLBuilder {

    private static def filterRE = /filter\[([0-9]+)\]\[(field|operator|value)\]=(.*)/
    private static final Range ALIASES = 'a'..'p'

    protected static final Log log = LogFactory.getLog(HQLBuilder.class)


    /**
     * Returns a map containing an HQL 'statement' and a map of named parameters.
     * The resulting Map is in the form:  [statement:'the HQL statement', namedParameters: [:] ]
     * Notes regarding the implementation with respect to SQL (or HQL) injection protection:
     * - Filter fields are checked to ensure they correspond to properties of the domain class.
     * - Filter operators are constrained to either 'eq', 'equals', or 'contains'
     * - Filter values are used as named parameters
     * - The 'sort' value is checked to ensure it represents a property of the domain class
     * - The 'order' value is constrained to 'asc' or 'desc'
     **/
    public static def createHQL( GrailsApplication application, Map params, boolean count = false ) {

        log.debug "createHQL() invoked with params = $params"

        GrailsDomainClass domainClass = Filter.getGrailsDomainClass(application, params.pluralizedResourceName)

        char firstAlias    = ALIASES[0]
        int lastAliasIndex = 0

        def selectFragment = count ? "SELECT count($firstAlias)" : "SELECT $firstAlias"
        def fromFragment   = " FROM " + domainClass.getName() + " $firstAlias"
        def joinFragment   = ""
        def whereFragment  = ""
        def orderFragment  = ""
        def namedParameters = [:]

        List filters = Filter.extractFilters( application, params )
        List badFilters = filters.findAll { !(it.isValid()) }
        if (badFilters) {
            throw new BadFilterException( params.pluralizedResourceName, badFilters )
        }

        // Note: a valid filter means the 'field' corresponds to a persistent property of the domain object
        //       so we don't have to escape 'field'. Only Filter 'value' must be escaped.
        filters.each {

            if (it.isAssociation) {
                char lastAlias = ALIASES[lastAliasIndex++]
                char alias     = ALIASES[lastAliasIndex]
                joinFragment += " INNER JOIN ${lastAlias}.${it.field} $alias"
                if (whereFragment != "") whereFragment += " AND "
                if (it.isMany) {
                    // TODO: Deal with nested queries and or projections (e.g., count(*))
                    log.error "extractFilters() - property $property is a collection and filtering on collections is not yet supported"
                }
                namedParameters."${it.field}" = "${it.value}".toLong()
                whereFragment += "${ALIASES[lastAliasIndex]}.id = :${it.field}"
            }
            else { // it's a primitive property
                if (whereFragment != "") whereFragment += " AND "

               if (it.operator == 'contains') {
                    namedParameters."${it.field}" = "%${it.value.toLowerCase()}%"
                    whereFragment += "lower(${firstAlias}.${it.field}) "
                    whereFragment += "LIKE lower(:${it.field})"
                }
                else {
                    namedParameters."${it.field}" = "${it.value}"
                    whereFragment += "${firstAlias}.${it.field} "
                    whereFragment += "= :${it.field}"
                }
            }
        }
        whereFragment = whereFragment ? " WHERE $whereFragment" : ""
        if (!count && params.sort && isProperty( domainClass, params.sort )) {
            orderFragment = " ORDER BY ${firstAlias}.${params.sort}"
        }
        if (!count && params.order && ['asc','desc'].contains(params.order)) {
            orderFragment += " ${params.order}"
        }

        def stmt = "$selectFragment$fromFragment$joinFragment$whereFragment$orderFragment".trim()
        log.debug "createHQL() is returning statement: $stmt"
        return [ statement: stmt, parameters: namedParameters ]
    }


    private static boolean isProperty( GrailsDomainClass domainClass, String propertyName ) {
        domainClass.getProperties()?.find { it.name == propertyName }
    }

}