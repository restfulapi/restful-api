/*******************************************************************************
Copyright 2009-2013 Ellucian Company L.P. and its affiliates.
*******************************************************************************/
package net.hedtech.restfulapi

import org.apache.commons.logging.LogFactory



/**
 * A grails filter used to establish a 'Tenant Context' to illustrate how
 * multi-tenancy may be supported when using the restful-api plugin.
 **/
class TenantFilters {

    def dlog = LogFactory.getLog( getClass() )

    def filters = {

        /**
         * This filter sets a 'TenantContext' (thread local) based upon a
         * tenant identifer within the URI.
         */
        setTenantContext( controller:'*', action:'*' ) {

            before = {

                if (params.tenant) {
                    // The tenant will be in the params map if URL mapping is configured
                    // to recognizes a 'tenant' URL part or if a query parameter was used.
                    TenantContext.set( params.tenant )
                }
                else {
                    // See if a tenant is identified using a subdomain
                    String serverName = request.getServerName()
                    String tenant = serverName.substring( 0, serverName.indexOf(".") ).toLowerCase()
                    TenantContext.set( tenant )
                }
                dlog.debug "TenantFilter 'before' filter set a TenantContext of ${TenantContext.get()}."
            }

            after = {
                TenantContext.clear()
            }

            afterView = { }
        }
    }

}
