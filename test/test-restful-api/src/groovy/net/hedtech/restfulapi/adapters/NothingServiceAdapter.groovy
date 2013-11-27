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
package net.hedtech.restfulapi.adapters

import net.hedtech.restfulapi.RestfulServiceAdapter
import net.hedtech.restfulapi.Thing

class NothingServiceAdapter implements RestfulServiceAdapter {


    def list(def service, Map params) throws Throwable {
        service.list(params)
    }

    def count(def service, Map params) throws Throwable {
        service.count(params)
    }

    def show(def service, Map params) throws Throwable {
        def result = service.show(params)
        if (result) {
            ((Thing)result).description = "Modified by the NothingServiceAdapter"
        }
        result
    }

    def create(def service, Map content, Map params) throws Throwable {
         service.create(content, params)
    }

    def update(def service, def id, Map content, Map params) throws Throwable {
        service.update(id,content,params)
    }

    void delete(def service, def id, Map content, Map params) throws Throwable {
        service.delete(id,content,params)
    }

}

