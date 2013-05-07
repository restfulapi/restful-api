#RESTful API plugin release notes

##next release
* NON-BACKWARD COMPATIBLE change: in the UrlMappings, the controller method 'save' has been replaced with 'create'
* Inclusion of an HQL builder that constructs a statement and parameter map based upon 'filter' query parameters
* Optional support for CORS via grails-cors plugin
* Support for lists of nested resources
* RestSpecification now supports a requestFactory attribute to use a custom connection factory for making requests; see CORSSpec for an example of usage
* Support for overriding the name of the service to use on a per-resource basis
* Support for limiting what methods a resource supports
* NON-BACKWARD COMPATIBLE change: net.hedtech.restfulapi.marshallers.BasicDomainClassMarshaller and net.hedtech.restfulapi.marshallers.BasicHalDomainClassMarshaller have been removed and replaced with the versions in net.hedtech.restfulapi.marshallers.json.
* NON-BACKWARD COMPATIBLE change: most of the overridable methods in BasicDomainClassMarshaller have been renamed, or had their signatures changed.  In particular, the processSpecificFields and processSimpleFields methods have been combined in processField.  Also, the return of processField has been reversed from what it previously was.  If the BasicDomainClassMarshaller should perform default marshalling of the field,

            processField(BeanWrapper beanWrapper, GrailsDomainClassProperty property, JSON json)
should return true.  If processField has completely handled the field and no further processing should occur for that field, it should return false.