/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.extractors

import groovy.util.slurpersupport.GPathResult

interface XMLExtractor extends Extractor {

    Map extract( GPathResult content )
}