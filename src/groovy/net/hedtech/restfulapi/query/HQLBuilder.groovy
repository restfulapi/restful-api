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


class HQLBuilder {

    private static def filterRE = /filter\[([0-9]+)\]\[(field|operator|value)\]=(.*)/
    private static final Range ALIASES = 'a'..'p'

    protected static final Log log = LogFactory.getLog(HQLBuilder.class)


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

        List filters = Filter.extractFilters( application, params ).findAll { it.isValid() }
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
                whereFragment += "${ALIASES[lastAliasIndex]}.id = ${it.value}"
            }
            else { // it's a primitive property
                if (whereFragment != "") whereFragment += " AND "

                if (it.operator == 'contains') {
                    whereFragment += "lower(${firstAlias}.${it.field}) "
                    whereFragment += "LIKE lower('%${it.value?.toLowerCase()}%')"
                }
                else {
                    whereFragment += "${firstAlias}.${it.field} "
                    whereFragment += "= '${it.value}'"
                }
            }
        }
        whereFragment = whereFragment ? " WHERE $whereFragment" : ""
        if (!count && params.sort) orderFragment = " ORDER BY ${firstAlias}.${params.sort}"
        if (!count && params.order) orderFragment += " ${params.order}"

        def stmt = "$selectFragment$fromFragment$joinFragment$whereFragment$orderFragment".trim()
        log.debug "createHQL() is returning statement: $stmt"
        stmt
    }

}