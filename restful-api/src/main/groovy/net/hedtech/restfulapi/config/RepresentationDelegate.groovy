/* ****************************************************************************
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

class RepresentationDelegate {

    def mediaTypes = []
    String marshallerFramework
    String contentType
    def jsonArrayPrefix
    def marshallers = []
    def extractor

    private RestConfig restConfig

    RepresentationDelegate(RestConfig config) {
        this.restConfig = config
    }

    RepresentationDelegate extractor(Object obj) {
        this.extractor = obj
        return this
    }

    RepresentationDelegate jsonExtractor(Closure c) {
        JSONExtractorDelegate delegate = new JSONExtractorDelegate()
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        extractor = JSONExtractorFactory.instantiate(delegate.config, restConfig)
        this
    }

    RepresentationDelegate xmlExtractor(Closure c) {
        XMLExtractorDelegate delegate = new XMLExtractorDelegate()
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        extractor = XMLExtractorFactory.instantiate(delegate.config, restConfig)
        this
    }

    RepresentationDelegate marshallers(Closure c) {
        MarshallersDelegate delegate = new MarshallersDelegate(this)
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()

        this
    }

    protected MarshallerGroupConfig getMarshallerGroup(String name) {
        restConfig.getMarshallerGroup(name)
    }

}
