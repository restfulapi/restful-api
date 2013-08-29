/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
grails.project.work.dir = 'target'

grails.project.repos.snap.url="http://m039200.ellucian.com:8081/artifactory/core-architecture-snapshot"

grails.project.dependency.resolution = {
    inherits 'global'
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsCentral()
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        test "org.spockframework:spock-grails-support:0.7-groovy-2.0"
    }

    plugins {
        build(":release:2.2.1") {
            export = false
        }
        compile(":inflector:0.2",
                ":cache-headers:1.1.5")
    }
}
