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
* In net.hedtech.restfulapi.query.HQLBuilderSpec.groovy, replace the import of grails.plugin.spock.IntegrationSpec with grails.test.spock.IntegrationSpec
* In BuildConfig.groovy, update the plugin dependencies for tomcat and hibernate to match the targeted versions for your grails 2.3.x release.
* In Config.groovy, search for instances of ClosureOjectMarshaller (this classname was mispelled in grails 2.2.x).  Replace it with the fixed named in grails 2.3.x: ClosureObjectMarshaller
* In Config.groovy, search for the resource 'part-of-things', and in the jsonExtractor, change flatObject from true to false.  (Grails 2.3.x and onward use the grails databinder by default, instead of Spring's.  The Grail's data binder uses maps of maps, e.g. [name:'bar', foo:[id:1]] instead of dotteed notation [name:'bar', foo.id:1], so incoming object references should no be flattened out.)

To run under grails 2.4.x, perform the changes necessary for 2.3.x, as well as the additional modifications:

* Run the grails set-grails-version command (see http://grails.org/doc/2.4.0.M1/guide/upgradingFrom23.html)
* Edit the applicationContext.xml and remove the grailsResourceLoader bean and the injection of it into the grails application (see http://grails.org/doc/2.4.0.M1/guide/upgradingFrom23.html)
* In BuildConfig.groovy, set grails.project.dependency.resolver = "maven"
* Update the hibernate and tomcat plugins to match the version of grails 2.4.x (use hibernate 3, not 4).
* Metaclass changes in BasicJSONExtractorSpec.groovy and BasicXMLExtractorSpec.groovy do not seem to be getting cleaned up between test runs.  Add a cleanup() method to each test to manually clean them up:

```
def cleanup() {
   GroovySystem.metaClassRegistry.removeMetaClass BasicJSONExtractor
}
```

```
def cleanup(){
   GroovySystem.metaClassRegistry.removeMetaClass BasicXMLExtractor
}
```
