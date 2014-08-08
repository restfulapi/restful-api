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

import java.security.*

import spock.lang.*

class EtagGeneratorSpec extends Specification {

    def "Test resource supports getEtag method"() {
        setup:
        EtagGenerator gen = new EtagGenerator()
        def expected = digest(['foo', 'this is my etag data'])

        when:
        def etag = gen.shaFor(new SupportsEtag(), 'foo')

        then:
        expected == etag
    }

    def "Test id with version"() {
        setup:
        EtagGenerator gen = new EtagGenerator()

        when:
        def etag = gen.shaFor(new VersionClass(id:1, version:2), 'foo')

        then:
        digest(['foo', '1', '2']) == etag
    }

    def "Test id with lastUpdated"() {
        setup:
        EtagGenerator gen = new EtagGenerator()

        when:
        def now = new Date()
        def etag = gen.shaFor(new LastUpdatedClass(id:1, lastUpdated: now), 'foo')

        then:
        digest(['foo', '1', now.toString()]) == etag
    }

    def "Test id with lastModified"() {
        setup:
        EtagGenerator gen = new EtagGenerator()

        when:
        def now = new Date()
        def etag = gen.shaFor(new LastModifiedClass(id:1, lastUpdated: now), 'foo')

        then:
        digest(['foo', '1', now.toString()]) == etag
    }

    /**
     * If there is only the id, then two generations should generate different values,
     * based on injecting a random UUID
     **/
    def "Test with id only"() {
        setup:
        EtagGenerator gen = new EtagGenerator()
        def object = new IdOnly(id:1)

        when:
        def etag1 = gen.shaFor(new IdOnly(id:1), 'foo')
        def etag2 = gen.shaFor(new IdOnly(id:1), 'foo')

        then:
        etag1 != etag2
    }

    def "Test collections"() {
        setup:
        EtagGenerator gen = new EtagGenerator()
        def now = new Date()
        def collection = []
        collection.add(new SupportsEtag())
        collection.add(new VersionClass(id:1, version:2))
        collection.add(new LastUpdatedClass(id:1, lastUpdated: now))
        collection.add(new LastModifiedClass(id:1, lastUpdated: now))
        def expected = digest(['foo', "${collection.size()}", '50', 'this is my etag data', '1', '2', '1', now.toString(), '1', now.toString()])

        when:
        def etag = gen.shaFor(collection, 50, 'foo')

        then:
        expected == etag
    }

    private String digest(def values) {
        MessageDigest digest = MessageDigest.getInstance( 'SHA1' )
        values.each { val ->
            digest.update(val.getBytes('UTF-8'))
        }
        return "\"${new BigInteger( 1, digest.digest() ).toString( 16 ).padLeft( 40,'0' )}\""
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
