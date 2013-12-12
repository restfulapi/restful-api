#Testing overview
There are no tests in the plugin project itself.

Instead, there is a robust test suite (current 850 tests) contained in a test application located in test/test-restful-api.  This application uses the plugin as an in-place plugin.

##Running the test suite.

    cd test/test-restful-api
    grails test-app

##Grails compatibility.

As written, the test suite works under grails 2.2.x.  It can be successfully run under grails 2.3.x with the following modifications:

* In BuildConfig.groovy, remove the test dependency on "org.spockframework:spock-grails-support:0.7-groovy-2.0"
* In BuildConfig.groovy, remove the plugin test dependency on ":spock:0.7"
* One of the tests currently fails if the new grails databinding is used.  In Config.groovy, add grails.databinding.useSpringBinder = true
* In net.hedtech.restfulapi.query.HQLBuilderSpec.groovy, replace the import of grails.plugin.spock.IntegrationSpec with grails.test.spock.IntegrationSpec
* In BuildConfig.groovy, update the plugin dependencies for tomcat and hibernate to match the targeted versions for your grails 2.3.x release.
* In Config.groovy, search for instances of ClosureOjectMarshaller (this classname was mispelled in grails 2.2.x).  Replace it with the fixed named in grails 2.3.x: ClosureObjectMarshaller