/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

import grails.converters.JSON

import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder as CCH
import org.codehaus.groovy.grails.web.converters.configuration.DefaultConverterConfiguration as DCC

import net.hedtech.restfulapi.*
import net.hedtech.restfulapi.marshallers.json.*

class RestfulApiGrailsPlugin {

    def version = "0.1"
    def grailsVersion = "2.1 > *"
    def dependsOn = ['inflector': '0.2']
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Restful Api Plugin"
    def author = "Ellucian"
    def authorEmail = ""
    def description = '''\
        |The resful-api plugin facilitates exposing a RESTful API.
        |Both JSON and XML representations are supported using a
        |singleton controller and both custom and default marshallers.
        |'''.stripMargin()

    def documentation = "readme.markdown" // use with pandoc, Marked, or other generator

    def organization = [ name: "Ellucian", url: "http://www.ellucian.com/" ]


// ----------------------------------------------------------------------------


    def doWithWebDescriptor = { xml -> }

    def doWithSpring = { }

    def doWithDynamicMethods = { ctx -> }

    def doWithApplicationContext = { applicationContext ->

        // Register general marshallers.  Note: Applications will
        // need to register their own resource-specific marshallers
        // (which may extend this plugin's marshallers).  Please see
        // the test application's 'Bootstrap.groovy' for an example
        // of registering 'named configurations'.

        // ------------------------ Common marshallers -----------------------

        // 'json' (application/json) configuration
        //
        JSON.registerObjectMarshaller(new BasicDomainClassMarshaller(application), 100)

        // Use an ISO8601-compliant date format
        //
        JSON.registerObjectMarshaller(Date) { return it?.format("yyyy-MM-dd'T'HH:mm:ssZ") }

        // Note: The below is an example showing an alternate approach for
        // overriding the default converters that also facilitates registering
        // named configurations (also shown below).  Since we expect each application
        // will need to specify named configurations for their versioned resources,
        // this is commented out and retained only for documentation.

        // // 'jsonv0' (application/vnd.hedtech.v0+json) configuration
        //
        // DCC<JSON> jsonConfig = new DCC<JSON>(CCH.getConverterConfiguration(JSON.class))
        // jsonConfig.registerObjectMarshaller(new BasicDomainClassMarshaller(application), 100)

        // // Override the default converter with our own... application/json
        //
        // CCH.setDefaultConfiguration(JSON, jsonConfig) // for now, using same configuration

        // // and add a configuration to support appication/vnd.hedtech.v0+json
        //
        // CCH.setNamedConverterConfiguration(JSON, 'jsonv0', jsonConfig)
    }

    def onChange = { event -> }

    def onConfigChange = { event -> }

    def onShutdown = { event -> }
}