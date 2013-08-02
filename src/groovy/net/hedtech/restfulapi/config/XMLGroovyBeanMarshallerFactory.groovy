/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

import net.hedtech.restfulapi.marshallers.xml.*

/**
 * A factory for creating DeclarativeGroovyBeanMarshaller instances
 * from configuration.
 */
class XMLGroovyBeanMarshallerFactory {

    static DeclarativeGroovyBeanMarshaller instantiateMarshaller(XMLGroovyBeanMarshallerConfig config, RestConfig restConfig) {
        //Merge the include chain into a final config
        config = restConfig.xmlGroovyBean.getMergedConfig( config )

        def marshaller = new DeclarativeGroovyBeanMarshaller(
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
