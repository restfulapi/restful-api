/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

import net.hedtech.restfulapi.marshallers.json.*

/**
 * A factory for creating DeclarativeBeanMarshaller instances
 * from configuration.
 */
class JSONBeanMarshallerFactory {

    static DeclarativeBeanMarshaller instantiateMarshaller(JSONBeanMarshallerConfig config, RestConfig restConfig) {
        //Merge the include chain into a final config
        config = restConfig.jsonBean.getMergedConfig( config )

        def marshaller = new DeclarativeBeanMarshaller(
            app:restConfig.grailsApplication
        )
        marshaller.fieldNames.putAll                config.fieldNames
        marshaller.includedFields.addAll            config.includedFields
        marshaller.excludedFields.addAll            config.excludedFields
        marshaller.additionalFieldClosures.addAll   config.additionalFieldClosures
        marshaller.additionalFieldsMap.putAll       config.additionalFieldsMap
        if (config.isSupportClassSet) marshaller.supportClass = config.supportClass
        if (config.requireIncludedFields != null) marshaller.requireIncludedFields = config.requireIncludedFields

        marshaller
    }

}
