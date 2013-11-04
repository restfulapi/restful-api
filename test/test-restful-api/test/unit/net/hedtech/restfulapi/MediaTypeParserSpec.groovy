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

import grails.test.mixin.*

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*

import spock.lang.*


class MediaTypeParserSpec extends Specification {

    def "Test xml content with charset"() {
        setup:
        def parser = new MediaTypeParser()

        when:
        def types = parser.parse("text/xml; charset=UTF-8")

        then:
        1                 == types.size()
        'application/xml' == types[0].name
    }

    def "Test FireFox2 Accept Header Ordering"() {
        setup:
        def parser = new MediaTypeParser()

        when:
        def types = parser.parse("text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5")

        then:
        6 == types.size()
        ['application/xhtml+xml','application/xml', 'image/png', 'text/html', 'text/plain', '*/*'] == types.name
    }

    def "Test FireFox 3 Accept Header Ordering"() {
        setup:
        def parser = new MediaTypeParser()

        when:
        def types = parser.parse("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

        then:
        4 == types.size()
        ['text/html','application/xhtml+xml', 'application/xml', '*/*'] == types.name

    }

    def "Test Accept Header with quality ordering"() {
        setup:
        def parser = new MediaTypeParser()

        when:
        def types = parser.parse("text/html,application/xhtml+xml,application/xml;q=1.1,*/*;q=0.8")

        then:

        4 == types.size()
        ['application/xml','text/html','application/xhtml+xml', '*/*'] == types.name
    }

    def "Test prototype header ordering"() {
        setup:
        def parser = new MediaTypeParser()

        when:
        def types = parser.parse("text/javascript, text/html, application/xml, text/xml, */*")

        then:
        4 == types.size()
        ['text/javascript','text/html', 'application/xml', '*/*'] == types.name
    }

    def "Test old browser header"() {
        setup:
        def parser = new MediaTypeParser()

        when:
        def types = parser.parse("*/*")

        then:
        1 == types.size()
        ['*/*'] == types.name
    }

    def "Test accept extension with token without value"() {
        setup:
        def parser = new MediaTypeParser()

        when:
        def types = parser.parse("text/html,application/xhtml+xml,application/xml;token,*/*;q=0.8")

        then:
        4 == types.size()
        ['text/html','application/xhtml+xml','application/xml','*/*'] == types.name
    }

    def "Test no Q value"() {
        setup:
        def parser = new MediaTypeParser()

        when:
        def types = parser.parse("application/xml; charset=UTF-8")

        then:
        1 == types.size()
        ['application/xml'] == types.name
    }

}
