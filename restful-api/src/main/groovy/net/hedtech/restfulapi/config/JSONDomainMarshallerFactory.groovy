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

import net.hedtech.restfulapi.marshallers.json.DeclarativeDomainClassMarshaller

/**
 * A factory for creating DeclarativeDomainClassMarshaller instances
 * from configuration.
 */
class JSONDomainMarshallerFactory {

    static DeclarativeDomainClassMarshaller instantiateMarshaller(JSONDomainMarshallerConfig config, RestConfig restConfig) {
        //Merge the include chain into a final config
        config = restConfig.jsonDomain.getMergedConfig( config )

        def marshaller = new DeclarativeDomainClassMarshaller(
            app:restConfig.grailsApplication
        )
        if (config.useIncludedFields) {
            marshaller.includedFields = config.includedFields
        }
        marshaller.fieldNames.putAll                config.fieldNames
        marshaller.excludedFields.addAll            config.excludedFields
        marshaller.additionalFieldClosures.addAll   config.additionalFieldClosures
        marshaller.additionalFieldsMap.putAll       config.additionalFieldsMap
        marshaller.fieldResourceNames.putAll        config.fieldResourceNames
        marshaller.deepMarshalledFields.putAll      config.deepMarshalledFields
        marshaller.marshalledNullFields.putAll      config.marshalledNullFields
        if (config.isSupportClassSet)       marshaller.supportClass       = config.supportClass
        if (config.isShortObjectClosureSet) marshaller.shortObjectClosure = config.shortObjectClosure
        if (config.includeId != null)       marshaller.includeId          = config.includeId
        if (config.includeVersion != null)  marshaller.includeVersion     = config.includeVersion
        if (config.requireIncludedFields != null) marshaller.requireIncludedFields = config.requireIncludedFields
        if (config.deepMarshallAssociations != null) marshaller.deepMarshallAssociations = config.deepMarshallAssociations
        if (config.marshallNullFields != null) marshaller.marshallNullFields = config.marshallNullFields

        marshaller
    }

}
