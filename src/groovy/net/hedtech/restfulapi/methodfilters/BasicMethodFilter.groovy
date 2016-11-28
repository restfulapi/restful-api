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

package net.hedtech.restfulapi.methodfilters

import net.hedtech.restfulapi.MethodFilter


/**
 * A method filter implementation for use with the 'restful-api' plugin.
 **/
class BasicMethodFilter implements MethodFilter {


    // must inject grailsApplication into this bean
    def grailsApplication

    // define resource name and the list of methods not allowed
    // for that resource as a map in the config file
    Map methodFilterMethodsNotAllowedMap


    /**
     * Dynamically determine whether a method is not allowed for a resource.
     **/
    def boolean isMethodNotAllowed(String resourceName, String methodName) {
        assert grailsApplication != null
        log.debug("Determining whether method=$methodName is not allowed for resource=$resourceName")
        def resourceValue = getMethodFilterMethodsNotAllowedMap()?.get(resourceName)
        List methodsNotAllowed = ((resourceValue instanceof List) ? resourceValue : (resourceValue != null ? [resourceValue] : []))
        boolean isNotAllowed = methodsNotAllowed.contains(methodName)
        log.debug("Determined method=$methodName is not allowed=$isNotAllowed")
        return isNotAllowed
    }


    /**
     * Get the method filter methods not allowed map from the configuration.
     **/
    private Map getMethodFilterMethodsNotAllowedMap() {
        if (methodFilterMethodsNotAllowedMap == null) {
            def value = grailsApplication.config?.restfulApi?.methodFilter?.methodsNotAllowedMap
            methodFilterMethodsNotAllowedMap = ((value instanceof Map) ? value : [:])
        }
        return methodFilterMethodsNotAllowedMap
    }
}
