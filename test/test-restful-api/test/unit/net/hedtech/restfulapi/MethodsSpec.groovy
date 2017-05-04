/* ****************************************************************************
 * Copyright 2017 Ellucian Company L.P. and its affiliates.
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

class MethodsSpec extends Specification {


    def "Test All Methods"() {
        expect:
        5 == Methods.getAllMethods().size()
        Methods.LIST   == Methods.getAllMethods().get(0)
        Methods.SHOW   == Methods.getAllMethods().get(1)
        Methods.CREATE == Methods.getAllMethods().get(2)
        Methods.UPDATE == Methods.getAllMethods().get(3)
        Methods.DELETE == Methods.getAllMethods().get(4)
    }

    def "Test All Http Methods"() {
        expect:
        4 == Methods.getAllHttpMethods().size()
        Methods.HTTP_GET    == Methods.getAllHttpMethods().get(0)
        Methods.HTTP_POST   == Methods.getAllHttpMethods().get(1)
        Methods.HTTP_PUT    == Methods.getAllHttpMethods().get(2)
        Methods.HTTP_DELETE == Methods.getAllHttpMethods().get(3)
    }

    def "Test Http Method"() {
        expect:
        Methods.HTTP_GET    == Methods.getHttpMethod(Methods.LIST)
        Methods.HTTP_GET    == Methods.getHttpMethod(Methods.SHOW)
        Methods.HTTP_POST   == Methods.getHttpMethod(Methods.CREATE)
        Methods.HTTP_PUT    == Methods.getHttpMethod(Methods.UPDATE)
        Methods.HTTP_DELETE == Methods.getHttpMethod(Methods.DELETE)
    }
}
