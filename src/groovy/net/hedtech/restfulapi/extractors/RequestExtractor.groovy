/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.extractors

import javax.servlet.http.HttpServletRequest

interface RequestExtractor extends Extractor {

    Map extract( HttpServletRequest request )
}