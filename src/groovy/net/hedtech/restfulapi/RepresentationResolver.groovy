/* ***************************************************************************
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

/**
 * Interface class used as a facade to resolve the resource representation
 **/
interface RepresentationResolver {

    /**
     * Return the media type to be used to extract the request body
     * @param pluralizedResourceName
     * @param request
     * @return
     * @throws Throwable
     */
    public String getRequestRepresentationMediaType(def pluralizedResourceName,def request) throws Throwable

    /**
     * Return the media type to be used to marshal the response body
     * @param pluralizedResourceName
     * @param request
     * @return
     * @throws Throwable
     */
    public String getResponseRepresentationMediaType(def pluralizedResourceName,def request) throws Throwable

}