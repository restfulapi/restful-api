/* ****************************************************************************
 * Copyright 2013-2014 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/

import grails.converters.JSON

import net.hedtech.restfulapi.*
import net.hedtech.restfulapi.marshallers.json.*

import org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationHolder as CCH
import org.codehaus.groovy.grails.web.converters.configuration.DefaultConverterConfiguration as DCC

class RestfulApiGrailsPlugin {

    def version = "1.1.0"
    def grailsVersion = "2.2.0 > *"
    def pluginExcludes = [
        "grails-app/views/**",
        "web-app/**"
    ]

    def title = "RESTful API Plugin"
    def author = "Charles Hardt, Shane Riddell"
    def authorEmail = "shane_riddell@icloud.com"
    def developers = [
        [name: "Charlie Hardt", email: "chasdev@me.com"]
    ]
    def description = '''\
        |The resful-api plugin facilitates exposing a non-trivial,
        | versioned RESTful API. The plugin provides a DSL that may
        | be used to declaratively specify how resources should be
        | marshalled. Please see the README.md for details.
        |'''.stripMargin()

    def documentation = "https://github.com/restfulapi/restful-api/blob/master/README.md"

    def scm = [url: "https://github.com/restfulapi/restful-api.git"]

    def issueManagement = [ system: "GITHUB", url: "https://github.com/restfulapi/restful-api/issues" ]

    def license = "APACHE"


// ----------------------------------------------------------------------------

    def doWithApplicationContext = { applicationContext ->
        // Initialize the Restful API controller (so it will register JSON and XML marshallers)
        //
        def artefact = application.getArtefactByLogicalPropertyName("Controller", "restfulApi")
        def restfulApiController = applicationContext.getBean(artefact.clazz.name)
        restfulApiController.init()
    }
}

