/* ****************************************************************************
 * Copyright 2013-2018 Ellucian Company L.P. and its affiliates.
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

        mavenRepo "http://repo.grails.org/grails/core"
        mavenRepo "http://repo.grails.org/grails/plugins"
    }

    dependencies {
        test "org.spockframework:spock-grails-support:0.7-groovy-2.0"

        // Dependency for CORS testing. see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6996110
        build 'org.apache.httpcomponents:httpclient:4.3.3'
    }

    plugins {
        compile ':cache:1.1.6'
        compile 'org.grails.plugins:inflector:0.2'
        compile 'org.grails.plugins:cache-headers:1.1.7'

        runtime ":hibernate:$grailsVersion"
        runtime ":jquery:1.11.1"
        runtime ":resources:1.2.8"
        runtime ":cors:1.1.0"

        test(":spock:0.7") {
          exclude "spock-grails-support"
        }

        test ":funky-spock:0.2.1"

        build ":tomcat:$grailsVersion"
    }
}

