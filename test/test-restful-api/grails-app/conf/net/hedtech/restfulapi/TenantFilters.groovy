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

import org.apache.commons.logging.LogFactory


/**
 * A grails filter used to establish a 'Tenant Context'.
 * This filter is intended to illustrate how multi-tenancy may be
 * supported when using the restful-api plugin.
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
                    String tenant
                    int index = serverName.indexOf(".")
                    if (index > 0) {
                        tenant = serverName.substring( 0, serverName.indexOf(".") ).toLowerCase()
                        TenantContext.set( tenant )
                    }
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
