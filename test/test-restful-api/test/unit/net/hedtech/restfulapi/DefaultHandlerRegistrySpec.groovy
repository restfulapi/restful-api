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

class DefaultHandlerRegistrySpec extends Specification {

    def "Test prioritization"() {
        setup:
        DefaultHandlerRegistry<String,StringHandler> registry = new DefaultHandlerRegistry<String,StringHandler>()

        when:
        registry.add(new StringHandler("a"))
        registry.add(new StringHandler("b"), 1)
        registry.add(new StringHandler("c"), 2)

        then:
        ['c','b','a'] == registry.getOrderedHandlers()*.target
        'b'           == registry.getHandler("b").target
    }

    def "Test last registry wins for same priority"() {
        setup:
        DefaultHandlerRegistry<String,StringHandler> registry = new DefaultHandlerRegistry<String,StringHandler>()
        StringHandler h1 = new StringHandler("a")
        StringHandler h2 = new StringHandler("b")
        StringHandler h3 = new StringHandler("a")

        when:
        registry.add(h1,1)
        registry.add(h2,1)
        registry.add(h3,1)

        then:
        h3 == registry.getHandler("a")
    }

    class StringHandler implements Handler<String> {
        String target
        StringHandler(String s) {
            this.target = s
        }

        boolean supports(String s) {
            return s == target
        }
    }
}
