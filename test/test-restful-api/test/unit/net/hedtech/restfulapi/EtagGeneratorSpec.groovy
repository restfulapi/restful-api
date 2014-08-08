/* ****************************************************************************
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
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

class EtagGeneratorSpec extends Specification {

    def "Test resource supports getEtag method"() {
        setup:
        EtagGenerator etag = new EtagGenerator()

        when:
        def values = etag.getValuesFor(new SupportsEtag(), 'foo')

        then:
        ['foo', 'this is my etag data'] == values
    }

    def "Test id with version"() {
        setup:
        EtagGenerator etag = new EtagGenerator()

        when:
        def values = etag.getValuesFor(new VersionClass(id:1, version:2), 'foo')

        then:
        ['foo', '1', '2'] == values
    }

    def "Test id with lastUpdated"() {
        setup:
        EtagGenerator etag = new EtagGenerator()

        when:
        def now = new Date()
        def values = etag.getValuesFor(new LastUpdatedClass(id:1, lastUpdated: now), 'foo')

        then:
        ['foo', '1', now.toString()] == values
    }

    def "Test id with lastModified"() {
        setup:
        EtagGenerator etag = new EtagGenerator()

        when:
        def now = new Date()
        def values = etag.getValuesFor(new LastModifiedClass(id:1, lastUpdated: now), 'foo')

        then:
        ['foo', '1', now.toString()] == values
    }

    def "Test with id only"() {
        setup:
        EtagGenerator etag = new EtagGenerator()

        when:
        def now = new Date()
        def values = etag.getValuesFor(new IdOnly(id:1), 'foo')

        then:
        2     == values.length
        'foo' == values[0]
        '1'   != values[1]
    }

    def "Test collections"() {
        setup:
        EtagGenerator etag = new EtagGenerator()
        def now = new Date()
        def collection = []
        collection.add(new SupportsEtag())
        collection.add(new VersionClass(id:1, version:2))
        collection.add(new LastUpdatedClass(id:1, lastUpdated: now))
        collection.add(new LastModifiedClass(id:1, lastUpdated: now))

        when:
        def values = etag.getValuesFor(collection, 50, 'foo')

        then:
        ['foo', '50', 'this is my etag data', '1', '2', '1', now.toString(), '1', now.toString()]

    }

    class SupportsEtag {
        String getEtag() {
            return "this is my etag data"
        }
    }

    class VersionClass {
        long id
        long version
        Date lastUpdated
        Date lastModified
    }

    class LastUpdatedClass {
        long id
        Date lastUpdated
        Date lastModified
    }

    class LastModifiedClass {
        long id
        Date lastUpdated
        Date lastModified
    }

    class IdOnly {
        long id
    }

}
