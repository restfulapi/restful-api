/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

/**
 * Exception thrown when a representation references an undefined
 * marshaller group.
 **/
class MissingMarshallerGroupException extends RuntimeException {
    String name

    String getMessage() {
        "Marshaller group $name not defined.  Marshaller groups must be defined before the resources that use them."
    }
}