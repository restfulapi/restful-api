/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.extractors

import org.codehaus.groovy.grails.web.json.JSONObject

interface JSONExtractor extends Extractor {

    Map extract( JSONObject content )
}