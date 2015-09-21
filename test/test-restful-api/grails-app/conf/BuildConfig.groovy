/* ****************************************************************************
 * Copyright 2013-2015 Ellucian Company L.P. and its affiliates.
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
        test 'org.springframework:spring-expression:4.1.4.RELEASE'
        test 'org.springframework:spring-aop:4.1.4.RELEASE'

        test 'org.apache.httpcomponents:httpclient:4.4.1'

        // Dependency for CORS testing. see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6996110
        test 'org.hamcrest:hamcrest-all:1.3'
    }

    plugins {
        compile ':inflector:0.2'
        compile ':cache-headers:1.1.7'

        runtime ":hibernate4:4.3.10"
        runtime ":jquery:1.11.1"
        runtime ":resources:1.2.8"
        runtime ":cors:1.1.0"

        test ":funky-spock:0.2.1"

        build ":tomcat:7.0.55.3"
    }
}
