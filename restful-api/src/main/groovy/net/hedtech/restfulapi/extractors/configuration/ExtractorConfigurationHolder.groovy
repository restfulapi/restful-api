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
package net.hedtech.restfulapi.extractors.configuration

import net.hedtech.restfulapi.extractors.Extractor

class ExtractorConfigurationHolder {

    private static ExtractorConfigurationHolder INSTANCE = new ExtractorConfigurationHolder()

    //key of outer map is the pluralized resource name.  Key of inner map is the media type for a resource representation.
    private final Map<String, Map<String,Extractor>> extractors = new HashMap<String,Map<String,Extractor>>()

    ExtractorConfigurationHolder() {
        //singleton
    }

    synchronized
    static void registerExtractor( String pluralizedResourceName, String mediaType, Extractor extractor ) {
        Map map = getInstance().extractors.get(pluralizedResourceName)
        if (!map) {
            map = new HashMap<String,Object>()
            getInstance().extractors.put(pluralizedResourceName,map)
        }
        map.put(mediaType,extractor)
    }

    static Extractor getExtractor( String pluralizedResourceName, String mediaType ) {
        getInstance().extractors.get( pluralizedResourceName )?.get(mediaType)
    }

    static ExtractorConfigurationHolder getInstance() {
        return INSTANCE
    }

    static void clear() {
        getInstance().clearMap()
    }

    void clearMap() {
        this.extractors.clear()
    }
}
