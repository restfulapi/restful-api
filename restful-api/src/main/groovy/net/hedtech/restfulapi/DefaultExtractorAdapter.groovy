/* ***************************************************************************
 * Copyright 2013 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/

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
