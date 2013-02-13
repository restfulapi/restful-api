/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.extractors.configuration

import net.hedtech.restfulapi.extractors.JSONExtractor
import net.hedtech.restfulapi.extractors.json.DefaultJSONExtractor

class JSONExtractorConfigurationHolder {

    private static JSONExtractorConfigurationHolder INSTANCE = new JSONExtractorConfigurationHolder()

    //key of outer map is the pluralized resource name.  Key of inner map is the format for a resource representation.
    private final Map<String, Map<String,JSONExtractor>> extractors = new HashMap<String,Map<String,JSONExtractor>>()

    JSONExtractorConfigurationHolder() {
        //singleton
    }

    synchronized
    static void registerExtractor( String pluralizedResourceName, String format, JSONExtractor extractor ) {
        Map map = getInstance().extractors.get(pluralizedResourceName)
        if (!map) {
            map = new HashMap<String,Object>()
            getInstance().extractors.put(pluralizedResourceName,map)
        }
        map.put(format,extractor)
    }

    static JSONExtractor getExtractor( String pluralizedResourceName, String format ) {
        getInstance().extractors.get( pluralizedResourceName )?.get(format)
    }

    static JSONExtractorConfigurationHolder getInstance() {
        return INSTANCE
    }
}