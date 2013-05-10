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
    def dependsOn = [:]
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "RESTful API Plugin"
    def author = "Ellucian"
    def authorEmail = ""
    def description = '''\
        |The resful-api plugin facilitates exposing a RESTful API that is
        |compliant with the Ellucian API Strategy document.
        |Both JSON and XML representations are supported using a
        |singleton controller and both custom and default marshallers.
        |'''.stripMargin()

    def documentation = "README.md" // use with pandoc, Marked, or other generator

    def organization = [ name: "Ellucian", url: "http://www.ellucian.com/" ]


// ----------------------------------------------------------------------------


    def doWithWebDescriptor = { xml -> }

    def doWithSpring = { }

    def doWithDynamicMethods = { ctx -> }

    def doWithApplicationContext = { applicationContext ->

        // Use an ISO8601-compliant date format
        //
        JSON.registerObjectMarshaller(Date) { return it?.format("yyyy-MM-dd'T'HH:mm:ssZ") }

        // ------------------------ Common marshallers -----------------------
        // Initialize the Restful API controller (so it will register JSON and XML marshallers)
        //
        def artefact = application.getArtefactByLogicalPropertyName("Controller", "restfulApi")
        def restfulApiController = applicationContext.getBean(artefact.clazz.name)
        restfulApiController.init()

    }

    def onChange = { event -> }

    def onConfigChange = { event -> }

    def onShutdown = { event -> }
}