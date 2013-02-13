/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.extractors.json

import net.hedtech.restfulapi.extractors.JSONExtractor

/**
 * Default extractor for JSON content.
 **/
class DefaultJSONExtractor implements JSONExtractor {

    Map extract( Map content ) {
        return content
    }

}