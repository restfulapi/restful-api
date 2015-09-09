/* ****************************************************************************
 * Copyright 2015 Ellucian Company L.P. and its affiliates.
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

import grails.test.mixin.*

import spock.lang.*

class LinkHeaderUtilsSpec extends Specification {

    @Unroll
    def 'Test Link header generation and extraction'( int offset, int limit, long totalCount,
                                                      String first, String prev, String next, String last ) {
        setup:
        def resourceName = 'things'

        when:
        def linkHeader = LinkHeaderUtils.generate( resourceName, offset, limit, totalCount )

        then:
        def links = LinkHeaderUtils.parse(linkHeader)
        assert( first == links['first'])
        assert( last ==  links['last'])
        assert( prev ==  links['prev'])
        assert( next ==  links['next'])

        where:
        offset | limit | totalCount | first                         | prev                          | next                          | last
        0      | 20    | 127        | null                          | null                          | '/things?offset=20&limit=20'  | '/things?offset=120&limit=20'
        20     | 20    | 127        | '/things?offset=0&limit=20'   | '/things?offset=0&limit=20'   | '/things?offset=40&limit=20'  | '/things?offset=120&limit=20'
        40     | 20    | 127        | '/things?offset=0&limit=20'   | '/things?offset=20&limit=20'  | '/things?offset=60&limit=20'  | '/things?offset=120&limit=20'
        100    | 20    | 127        | '/things?offset=0&limit=20'   | '/things?offset=80&limit=20'  | '/things?offset=120&limit=20' | '/things?offset=120&limit=20'
        120    | 20    | 127        | '/things?offset=0&limit=20'   | '/things?offset=100&limit=20' | null                          | null
        // 0      | 20    | 12         | null                          | null                          | null                          | null
    }
}
