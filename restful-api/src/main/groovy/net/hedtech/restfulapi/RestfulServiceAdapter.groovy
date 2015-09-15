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


/**
 * An interface for services and or adapters.
 * This interface is admittedly very loose, as it uses duck typing ('def').
 * Please see README.md for a full explanation.
 **/
interface RestfulServiceAdapter {


    def list(def service, Map params) throws Throwable

    def count(def service, Map params) throws Throwable

    def show(def service, Map params) throws Throwable

    def create(def service, Map content, Map params) throws Throwable

    def update(def service, Map content, Map params) throws Throwable

    void delete(def service, Map content, Map params) throws Throwable

}
