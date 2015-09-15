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
package net.hedtech.restfulapi.config

class RepresentationConfig {

    String mediaType
    String marshallerFramework
    String contentType
    def jsonArrayPrefix
    def marshallers = []
    def extractor


    /**
     * Returns the marshalling framework for the representation.
     * If not explicitly specified, will attempt to determine
     * it based on the mediatype.
     **/
    public String resolveMarshallerFramework() {
        if ('none' == marshallerFramework) {
            return null
        }

        if (null != marshallerFramework) {
            return marshallerFramework
        }

        switch(mediaType) {
            case ~/.*json$/:
                return 'json'
                break
            case ~/.*xml$/:
                return 'xml'
                break
            default:
                return null
                break
        }
    }
}
