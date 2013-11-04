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

import org.codehaus.groovy.grails.commons.GrailsApplication

class MarshallerGroupsDelegate {

    private RestConfig restConfig

    MarshallerGroupsDelegate(RestConfig restConfig) {
        this.restConfig = restConfig
    }

    def group(String name) {
        GroupDelegate delegate = new GroupDelegate()
        [marshallers:{Closure c ->
            c.delegate = delegate
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()
            MarshallerGroupConfig config = new MarshallerGroupConfig(name:name,marshallers:delegate.marshallers)
            restConfig.marshallerGroups[name] = config
        }]
    }

    class GroupDelegate {
        def marshallers = []

        GrailsApplication getGrailsApplication() {
            restConfig.grailsApplication
        }

        GroupDelegate marshaller( Closure c ) {
            MarshallerConfig config = new MarshallerConfig()
            c.delegate = config
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()
            marshallers.add config
            this
        }

        GroupDelegate jsonDomainMarshaller( Closure c ) {
            JSONDomainMarshallerDelegate delegate = new JSONDomainMarshallerDelegate()
            c.delegate = delegate
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()

            def marshaller = JSONDomainMarshallerFactory.instantiateMarshaller(delegate.config,restConfig)

            MarshallerConfig marshallerConfig = new MarshallerConfig()
            marshallerConfig.instance = marshaller
            marshallerConfig.priority = delegate.config.priority

            marshallers.add marshallerConfig

            this
        }

        GroupDelegate jsonBeanMarshaller( Closure c ) {
            JSONBeanMarshallerDelegate delegate = new JSONBeanMarshallerDelegate()
            c.delegate = delegate
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()

            def marshaller = JSONBeanMarshallerFactory.instantiateMarshaller(delegate.config,restConfig)

            MarshallerConfig marshallerConfig = new MarshallerConfig()
            marshallerConfig.instance = marshaller
            marshallerConfig.priority = delegate.config.priority

            marshallers.add marshallerConfig

            this
        }

        GroupDelegate xmlDomainMarshaller( Closure c ) {
            XMLDomainMarshallerDelegate delegate = new XMLDomainMarshallerDelegate()
            c.delegate = delegate
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()

            def marshaller = XMLDomainMarshallerFactory.instantiateMarshaller(delegate.config,restConfig)

            MarshallerConfig marshallerConfig = new MarshallerConfig()
            marshallerConfig.instance = marshaller
            marshallerConfig.priority = delegate.config.priority

            marshallers.add marshallerConfig

            this
        }

        GroupDelegate xmlBeanMarshaller( Closure c ) {
            XMLBeanMarshallerDelegate delegate = new XMLBeanMarshallerDelegate()
            c.delegate = delegate
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c.call()

            def marshaller = XMLBeanMarshallerFactory.instantiateMarshaller(delegate.config,restConfig)

            MarshallerConfig marshallerConfig = new MarshallerConfig()
            marshallerConfig.instance = marshaller
            marshallerConfig.priority = delegate.config.priority

            marshallers.add marshallerConfig

            this
        }
    }

}
