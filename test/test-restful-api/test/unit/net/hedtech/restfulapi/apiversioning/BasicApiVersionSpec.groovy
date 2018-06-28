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
import net.hedtech.restfulapi.ApiVersion
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class BasicApiVersionSpec extends Specification {

    def "Test compare ordering"() {
        setup:
        List<ApiVersion> orderedApiVersions = new ArrayList<>()
        orderedApiVersions.add(new BasicApiVersion('resource-1', -1, -1, -1, null))
        orderedApiVersions.add(new BasicApiVersion('resource-1', -1, -1, -1, 'vnd.hedtech'))
        orderedApiVersions.add(new BasicApiVersion('resource-1', 0, -1, -1, 'vnd.hedtech'))
        orderedApiVersions.add(new BasicApiVersion('resource-1', 0, 0, 0, 'vnd.hedtech'))
        orderedApiVersions.add(new BasicApiVersion('resource-1', 1, -1, -1, 'vnd.hedtech'))
        orderedApiVersions.add(new BasicApiVersion('resource-1', 1, 2, 3, 'vnd.hedtech'))
        orderedApiVersions.add(new BasicApiVersion('resource-1', 2, -1, -1, 'vnd.hedtech.maximum'))
        orderedApiVersions.add(new BasicApiVersion('resource-1', 2, -1, -1, 'vnd.hedtech'))
        orderedApiVersions.add(new BasicApiVersion('resource-1', 2, 0, 0, 'vnd.hedtech.maximum'))
        orderedApiVersions.add(new BasicApiVersion('resource-1', 2, 0, 0, 'vnd.hedtech'))
        orderedApiVersions.add(new BasicApiVersion('resource-1', 3, 0, 0, 'vnd.hedtech'))
        orderedApiVersions.add(new BasicApiVersion('resource-1', 3, 0, 2, 'vnd.hedtech'))
        orderedApiVersions.add(new BasicApiVersion('resource-1', 3, 4, 0, 'vnd.hedtech'))
        orderedApiVersions.add(new BasicApiVersion('resource-2', 2, 0, 0, 'vnd.hedtech'))

        when:
        List<ApiVersion> sortedApiVersions = new ArrayList<>()
        List<ApiVersion> reversedApiVersions = new ArrayList<>()
        for (int i=0; i<orderedApiVersions.size(); i++) {
            sortedApiVersions.add(orderedApiVersions.get(i))
            reversedApiVersions.add(orderedApiVersions.get(i))
        }
        sortedApiVersions = sortedApiVersions.reverse()
        sortedApiVersions = sortedApiVersions.sort()
        reversedApiVersions = reversedApiVersions.sort()
        reversedApiVersions = reversedApiVersions.reverse()

        then:
        for (int i=0; i<orderedApiVersions.size(); i++) {
            assert sortedApiVersions.get(i) == orderedApiVersions.get(i)
            assert reversedApiVersions.get(i) == orderedApiVersions.get(orderedApiVersions.size() - 1 - i)
        }
    }

    def "Test hashcode and equals"() {
        setup:
        ApiVersion apiVersion1 = new BasicApiVersion('resource-1', 1, 2, 3, 'vnd.hedtech', 'a')
        ApiVersion apiVersion2 = new BasicApiVersion('resource-1', 4, 5, 6, 'vnd.hedtech', 'b')
        ApiVersion apiVersion3 = new BasicApiVersion('resource-1', 1, 2, 3, 'vnd.hedtech', 'c')
        ApiVersion apiVersion4 = new BasicApiVersion('resource-1', 4, 5, 6, null, 'd')
        ApiVersion apiVersion5 = new BasicApiVersion('resource-2', 4, 5, 6, 'vnd.hedtech', 'b')

        when:
        Map apiVersionMap = [:]
        apiVersionMap.put(apiVersion1, "one")
        apiVersionMap.put(apiVersion2, "two")
        apiVersionMap.put(apiVersion5, "three")

        then:
        "one" == apiVersionMap.get(apiVersion1)
        "two" == apiVersionMap.get(apiVersion2)
        apiVersion1 == apiVersion3
        "one" == apiVersionMap.get(apiVersion3)
        apiVersion2 != apiVersion4
        null == apiVersionMap.get(apiVersion4)
        "three" == apiVersionMap.get(apiVersion5)
        apiVersion2 != apiVersion5
    }

}
