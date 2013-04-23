/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6

grails.plugin.location.'restful-api' = "../.."

grails.project.dependency.resolution = {

    inherits("global") { }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()
        mavenRepo name: "core-architecture",root: "http://m039200.ellucian.com:8081/artifactory/core-architecture"
    }
    dependencies {
        test "org.spockframework:spock-grails-support:0.7-groovy-2.0"
        //dependency for CORS testing. see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6996110
        test "commons-httpclient:commons-httpclient:3.1"
    }

    plugins {
        runtime ":hibernate:$grailsVersion"
        runtime ":jquery:1.7.2"
        runtime ":resources:1.1.6"
        build   ":tomcat:$grailsVersion"
        runtime ":database-migration:1.1"
        compile ':cache:1.0.0'
        test(":spock:0.7") {
          exclude "spock-grails-support"
        }
        //test ":rest-client-builder:1.0.2"
        compile ":functional-spock:0.6"
        //plugin CORS testing.  currently using private fork until
        //changes are accepted in official plugin
        runtime "core-architecture:grails-cors:1.0.5-SNAPSHOT"

        // 'grails install-plugin inflector' updated
        // application.properties file.  Source code is at:
        // https://github.com/tomaslin/grails-inflector
    }
}
