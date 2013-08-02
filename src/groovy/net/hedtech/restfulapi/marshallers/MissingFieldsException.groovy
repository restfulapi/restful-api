/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.marshallers

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException

class MissingFieldsException extends ConverterException {
    List<String> missingNames

    MissingFieldsException( List<String> missing ) {
        super("Object is missing required included fields " + missing)
        this.missingNames = missing
    }
}
