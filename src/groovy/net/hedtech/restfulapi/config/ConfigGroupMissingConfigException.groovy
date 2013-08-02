/* ***************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config


/**
 * Exception thrown when a domain class marshaller definition includes
 * an unknown template.
 **/
class MissingDomainMarshallerTemplateException extends RuntimeException {
    String name

    String getMessage() {
        "Domain Class Marshaller template $name not defined.  Marshaller templates must be defined before use."
    }
}
