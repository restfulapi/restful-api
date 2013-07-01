/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

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