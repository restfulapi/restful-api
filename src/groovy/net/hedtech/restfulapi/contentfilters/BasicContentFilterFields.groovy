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

    // define resource name and the list of field patterns
    // for that resource as a map in the config file
    Map contentFilterFieldPatternsMap


    /**
     * Retrieve list of field patterns to be filtered from content.
     **/
    public List retrieveFieldPatterns(String resourceName) {
        assert grailsApplication != null
        log.debug("Retrieving field patterns for resource=$resourceName")
        def resourceValue = getContentFilterFieldPatternsMap()?.get(resourceName)
        List fieldPatterns = ((resourceValue instanceof List) ? resourceValue : (resourceValue != null ? [resourceValue] : []))
        fieldPatterns = fieldPatterns.unique().sort()
        log.debug("Returning field patterns=$fieldPatterns")
        return fieldPatterns
    }


    /**
     * Get the content filter field patterns map from the configuration.
     **/
    private Map getContentFilterFieldPatternsMap() {
        if (contentFilterFieldPatternsMap == null) {
            def value = grailsApplication.config?.restfulApi?.contentFilter?.fieldPatternsMap
            contentFilterFieldPatternsMap = ((value instanceof Map) ? value : [:])
        }
        return contentFilterFieldPatternsMap
    }
}
