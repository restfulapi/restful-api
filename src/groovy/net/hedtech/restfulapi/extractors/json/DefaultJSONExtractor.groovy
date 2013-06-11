/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.extractors.json

import net.hedtech.restfulapi.extractors.JSONExtractor

import org.codehaus.groovy.grails.web.json.JSONObject

/**
 * Default extractor for JSON content.
 **/
class DefaultJSONExtractor implements JSONExtractor {

    Map extract( JSONObject content ) {
        return content
    }

}