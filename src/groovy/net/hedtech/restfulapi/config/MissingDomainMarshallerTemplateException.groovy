/* ***************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

/**
 * Exception thrown when a domain class marshaller definition includes
 * an unknown template.
 **/
class ConfigGroupMissingConfigException extends RuntimeException {
    String name

    String getMessage() {
        "Config $name not defined."
    }
}
