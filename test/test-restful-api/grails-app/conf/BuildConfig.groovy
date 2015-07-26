/* ****************************************************************************
 * Copyright 2013 Ellucian Company L.P. and its affiliates.
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

grails.servlet.version          = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.work.dir         = 'target'
grails.project.target.level     = 1.6
grails.project.source.level     = 1.6

grails.plugin.location.'restful-api' = "../.."

grails.project.dependency.resolver = "maven"

grails.project.dependency.resolution = {

    inherits 'global'
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()

    }

    dependencies {

        // Dependency for CORS testing. see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6996110
        test 'org.apache.httpcomponents:httpclient:4.3.3'
    }

    plugins {
        compile ':cache:1.1.8'
        //compile ":scaffolding:2.1.2"
        compile ':inflector:0.2'
        compile ':cache-headers:1.1.7'
        compile ":asset-pipeline:2.1.5"

        runtime ":hibernate:3.6.10.18"
        runtime ":database-migration:1.4.0"
        runtime ":jquery:1.11.1"
        runtime ":resources:1.2.8"
        runtime ":cors:1.1.0"

        test ":funky-spock:0.2.1"

        build ":tomcat:7.0.55.2" 
    }
}

