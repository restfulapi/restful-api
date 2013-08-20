/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.extractors

class ShortObjectExtractionException extends SyntaxException {

    ShortObjectExtractionException( def badValue ) {
        super('default.rest.extractor.unparsableShortObject', [badValue])
        this.params = params
    }
}
