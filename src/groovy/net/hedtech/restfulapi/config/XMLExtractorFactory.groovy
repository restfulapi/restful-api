/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import net.hedtech.restfulapi.extractors.xml.DeclarativeXMLExtractor

/**
 * A factory for creating DeclarativeXMLExtractor instances
 * from configuration.
 */
class XMLExtractorFactory {

    static DeclarativeXMLExtractor instantiate(XMLExtractorConfig config, RestConfig restConfig) {
        //Merge the include chain into a final config
        config = restConfig.xmlExtractor.getMergedConfig( config )

        def extractor = new DeclarativeXMLExtractor()
        extractor.dottedRenamedPaths.putAll config.dottedRenamedPaths
        extractor.dottedValuePaths.putAll config.dottedValuePaths
        extractor.dottedShortObjectPaths.addAll config.dottedShortObjectPaths
        extractor.dottedFlattenedPaths.addAll config.dottedFlattenedPaths
        if (config.isShortObjectClosureSet) extractor.shortObjectClosure = config.shortObjectClosure

        extractor
    }

}