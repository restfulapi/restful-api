/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.extractors.configuration

import net.hedtech.restfulapi.extractors.XMLExtractor

class XMLExtractorConfigurationHolder {

    private static XMLExtractorConfigurationHolder INSTANCE = new XMLExtractorConfigurationHolder()

    //key of outer map is the pluralized resource name.  Key of inner map is the format for a resource representation.
    private final Map<String, Map<String,XMLExtractor>> extractors = new HashMap<String,Map<String,XMLExtractor>>()

    XMLExtractorConfigurationHolder() {
        //singleton
    }

    synchronized
    static void registerExtractor( String pluralizedResourceName, String format, XMLExtractor extractor ) {
        Map map = getInstance().extractors.get(pluralizedResourceName)
        if (!map) {
            map = new HashMap<String,Object>()
            getInstance().extractors.put(pluralizedResourceName,map)
        }
        map.put(format,extractor)
    }

    static XMLExtractor getExtractor( String pluralizedResourceName, String format ) {
        getInstance().extractors.get( pluralizedResourceName )?.get(format)
    }

    static XMLExtractorConfigurationHolder getInstance() {
        return INSTANCE
    }
}