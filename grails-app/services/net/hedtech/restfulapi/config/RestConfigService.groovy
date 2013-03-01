/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import net.hedtech.restfulapi.*

class RestConfigService {
    def grailsApplication

    private RESTConfig config

    RESTConfig getConfig() {
        if (!config) {
            config = RESTConfig.parse( grailsApplication, grailsApplication.config.restfulApiConfig )
        }
        config
    }
}