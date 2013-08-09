/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.extractors

class DateParseException extends SyntaxException {

    DateParseException( String badValue ) {
        super('default.rest.extractor.unparsableDate', [badValue])
        this.params = params
    }
}
