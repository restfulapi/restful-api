/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

import net.hedtech.restfulapi.extractors.json.DeclarativeJSONExtractor

/**
 * A factory for creating DeclarativeJSONExtractor instances
 * from configuration.
 */
class JSONExtractorFactory {

    static DeclarativeJSONExtractor instantiate(JSONExtractorConfig config, RestConfig restConfig) {
        //Merge the include chain into a final config
        config = restConfig.jsonExtractor.getMergedConfig( config )

        def extractor = new DeclarativeJSONExtractor()
        extractor.dottedRenamedPaths.putAll config.dottedRenamedPaths
        extractor.dottedValuePaths.putAll config.dottedValuePaths
        extractor.dottedShortObjectPaths.addAll config.dottedShortObjectPaths
        extractor.dottedFlattenedPaths.addAll config.dottedFlattenedPaths
        if (config.isShortObjectClosureSet) extractor.shortObjectClosure = config.shortObjectClosure
        extractor.dottedDatePaths = config.dottedDatePaths
        extractor.dateFormats = config.dateFormats

        extractor
    }

}
