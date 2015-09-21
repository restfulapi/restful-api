/* ****************************************************************************
 * Copyright 2013-2015 Ellucian Company L.P. and its affiliates.
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
package net.hedtech.restfulapi.extractors.json

import net.hedtech.restfulapi.extractors.JSONExtractor

import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Default extractor for JSON content.
 **/
class DefaultJSONExtractor implements JSONExtractor {

    Map extract( JSONObject content ) {
        unwrap( content )
    }

    // We'll build a Map from the JSONObject, as JSONObject
    // is not serializable and not a direct replacement for Map.
    //
    private def unwrap( def content ) {
        if (content == null) return null
        if (content == JSONObject.NULL) return null
        if (content instanceof Map) {
            Map map = [:]
            content.entrySet().each { entry ->
                map[entry.key] = unwrap(entry.value)
            }
            return map
        }
        return content
    }

}
