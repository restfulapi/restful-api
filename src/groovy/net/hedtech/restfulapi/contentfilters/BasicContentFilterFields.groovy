/* ***************************************************************************
 * Copyright 2016 Ellucian Company L.P. and its affiliates.
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

package net.hedtech.restfulapi.contentfilters

import net.hedtech.restfulapi.ContentFilterFields


/**
 * A content filter fields implementation for use with the 'restful-api' plugin.
 **/
class BasicContentFilterFields implements ContentFilterFields {


    // must inject grailsApplication into this bean
    def grailsApplication

    // define field list map in config file using resource name as the map key
    Map contentFilterFieldsMap


    /**
     * Retrieve list of fields or field patterns to be filtered from content.
     **/
    public List retrieveFields(String resourceName) {
        log.debug("Retrieving fields for resource=$resourceName")
        List fields = (getContentFilterFieldsMap()?.get(resourceName) ?: [])
        log.debug("Returning fields=$fields")
        return fields
    }


    /**
     * Get the content filter fields map from the configuration.
     **/
    private Map getContentFilterFieldsMap() {
        if (contentFilterFieldsMap == null) {
            def value = grailsApplication?.config.restfulApi.contentFilter.fieldsMap
            contentFilterFieldsMap = ((value instanceof Map) ? value : [:])
        }
        return contentFilterFieldsMap
    }
}
