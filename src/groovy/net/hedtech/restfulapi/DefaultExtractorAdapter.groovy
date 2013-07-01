/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import net.hedtech.restfulapi.extractors.*

import javax.servlet.http.HttpServletRequest

class DefaultExtractorAdapter implements ExtractorAdapter {

    Map extract(JSONExtractor extractor, HttpServletRequest request) {
        extractor.extract(request.JSON)
    }

    Map extract(XMLExtractor extractor, HttpServletRequest request) {
        def map = extractor.extract(request.XML)
    }

    Map extract(RequestExtractor extractor, HttpServletRequest request) {
        extractor.extract(request)
    }
}