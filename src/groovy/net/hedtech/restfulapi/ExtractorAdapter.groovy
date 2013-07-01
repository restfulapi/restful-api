/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import net.hedtech.restfulapi.extractors.*

import javax.servlet.http.HttpServletRequest

interface ExtractorAdapter {

    Map extract(JSONExtractor extractor, HttpServletRequest request)

    Map extract(XMLExtractor extractor, HttpServletRequest request)

    Map extract(RequestExtractor extractor, HttpServletRequest request)
}