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


/*
Attribution:
Although this implementation is significantly different, the HqlQueryBuilder.java
file from the grails-restful-gorm plugin was reviewed and provided background that
was helpful.  The grails-restful-gorm plugin was developed by Matthias Hryniszak.

The specific code that was referenced is:
https://github.com/padcom/grails-restful-gorm/blob/master/src/java/org/grails/plugins/rest/impl/HqlQueryBuilder.java
*/

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
     * The GrailsDomainClass used within the query is determined using the params.pluralizedResourceName,
     * however may be overriden by supplying a params.domainClass whose value is a GrailsDomainClass.
     **/
    public static Map createHQL( GrailsApplication application, Map params, boolean count = false ) {

        log.debug "createHQL() invoked with params = $params"

        GrailsDomainClass domainClass = params.domainClass ?: Filter.getGrailsDomainClass(application, params.pluralizedResourceName)

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
            log.debug "Cannot create query with bad filters: ${badFilters*.toString()}"
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
                    // Should never be here (as when a filter.isMany=true it should not have been reported as a 'valid' filter)
                    throw RuntimeException("HQLBuilder found field ${it.field} to be a collection and filtering on collections is not supported")
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
                    if (it.isNumeric()) namedParameters."${it.field}" = "${it.value}".toLong()
                    else if (it.isDate()) namedParameters."${it.field}" = parseDate( params, it )
                    else  namedParameters."${it.field}" = "${it.value}"

                    whereFragment += "${firstAlias}.${it.field} "
                    if ('lt' == it.operator) whereFragment += "< :${it.field}"
                    else if ('gt' == it.operator) whereFragment += "> :${it.field}"
                    else if ('le' == it.operator) whereFragment += "<= :${it.field}"
                    else if ('ge' == it.operator) whereFragment += ">= :${it.field}"
                    else whereFragment += "= :${it.field}"
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

    private static Date parseDate(Map params, Filter filter) {
        if (filter.value == null) return null
        //see if its numeric, if so, treat as millis since Epoch
        try {
            Long l = Long.valueOf(filter.value)
            return new Date(l)
        } catch (Exception e) {
            //can't parse as a long
        }
        //try to parse as ISO 8601
        try {
            def cal = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(filter.value)
            return cal.toGregorianCalendar().getTime()
        } catch (Exception e) {
            //can't parse as ISO 8601
        }
        //wasn't able to parse as a date
        throw new BadDateFilterException(params.pluralizedResourceName,filter)
    }

}
