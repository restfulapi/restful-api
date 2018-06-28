/* ****************************************************************************
 * Copyright 2018 Ellucian Company L.P. and its affiliates.
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

package net.hedtech.restfulapi.apiversioning

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

import net.hedtech.restfulapi.ApiVersion
import net.hedtech.restfulapi.ApiVersionParser

@TestMixin(GrailsUnitTestMixin)
class BasicApiVersionParserSpec extends Specification {

    ApiVersionParser apiVersionParser

    void setup() {
        apiVersionParser = new BasicApiVersionParser()
    }

    def "Test successful parse"(String resourceName, String mediaType, String version, String schema) {
        when:
        ApiVersion apiVersion = apiVersionParser.parseMediaType(resourceName, mediaType)

        then:
        resourceName == apiVersion.resourceName
        version == apiVersion.version
        schema == apiVersion.schema
        mediaType == apiVersion.mediaType

        where:
        resourceName | mediaType                                     | version  | schema
        'resource-1' | 'application/json'                            | null     | null
        'resource-1' | 'application/vnd.hedtech+json'                | null     | 'vnd.hedtech'
        'resource-1' | 'application/vnd.hedtech.v0+json'             | 'v0'     | 'vnd.hedtech'
        'resource-1' | 'application/vnd.hedtech.v1+json'             | 'v1'     | 'vnd.hedtech'
        'resource-1' | 'application/vnd.hedtech.v2+json'             | 'v2'     | 'vnd.hedtech'
        'resource-1' | 'application/vnd.hedtech.maximum.v2+json'     | 'v2'     | 'vnd.hedtech.maximum'
        'resource-1' | 'application/vnd.hedtech.v0.0.0+json'         | 'v0.0.0' | 'vnd.hedtech'
        'resource-1' | 'application/vnd.hedtech.v1.2.3+json'         | 'v1.2.3' | 'vnd.hedtech'
        'resource-1' | 'application/vnd.hedtech.v2.0.0+json'         | 'v2.0.0' | 'vnd.hedtech'
        'resource-1' | 'application/vnd.hedtech.maximum.v2.0.0+json' | 'v2.0.0' | 'vnd.hedtech.maximum'
        'resource-1' | 'application/vnd.hedtech.q3+json'             | null     | 'vnd.hedtech.q3'
        'resource-2' | 'application/vnd.hedtech.v2.0.0+json'         | 'v2.0.0' | 'vnd.hedtech'
    }

    def "Test failed parse"(String mediaType, String errorMessage) {
        when:
        apiVersionParser.parseMediaType("resource-1", mediaType)

        then:
        IllegalArgumentException e = thrown()
        errorMessage == e.getMessage()

        where:
        mediaType                                     | errorMessage
        'application/vnd.hedtech.vabc+json'           | 'Wrong version format - expected VERSION - got abc'
        'application/vnd.hedtech.v1.2.3.4+json'       | 'Wrong semantic version format - expected MAJOR.MINOR.PATCH - got 1.2.3.4'
        'application/vnd.hedtech.v1.-2.3+json'        | 'Wrong version format - expected positive values for MAJOR.MINOR.PATCH - got 1.-2.3'
        'application/vnd.hedtech.v1.b.3+json'         | 'Wrong version format - expected MAJOR.MINOR.PATCH - got 1.b.3'
    }

}
