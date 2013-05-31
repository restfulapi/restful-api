/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

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
        marshaller.fieldNames.putAll                config.fieldNames
        marshaller.includedFields.addAll            config.includedFields
        marshaller.excludedFields.addAll            config.excludedFields
        marshaller.additionalFieldClosures.addAll   config.additionalFieldClosures
        marshaller.additionalFieldsMap.putAll       config.additionalFieldsMap
        marshaller.fieldResourceNames.putAll        config.fieldResourceNames
        if (config.isSupportClassSet)       marshaller.supportClass       = config.supportClass
        if (config.isShortObjectClosureSet) marshaller.shortObjectClosure = config.shortObjectClosure
        if (config.includeId != null)       marshaller.includeId          = config.includeId
        if (config.includeVersion != null)  marshaller.includeVersion     = config.includeVersion

        marshaller
    }

}