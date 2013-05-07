#RESTful API plugin release notes

next release
* NON-BACKWARD COMPATIBLE change: in the UrlMappings, the controller method 'save' has been replaced with 'create'
* Inclusion of an HQL builder that constructs a statement and parameter map based upon 'filter' query parameters
* Optional support for CORS via grails-cors plugin
* Support for lists of nested resources
* RestSpecification now supports a requestFactory attribute to use a custom connection factory for making requests; see CORSSpec for an example of usage
* Support for overriding the name of the service to use on a per-resource basis
* Support for limiting what methods a resource supports
* NON-BACKWARD COMPATIBLE change: net.hedtech.restfulapi.marshallers.BasicDomainClassMarshaller and net.hedtech.restfulapi.marshallers.BasicHalDomainClassMarshaller have been removed and replaced with the versions in net.hedtech.restfulapi.marshallers.json.


v0.1.0

* Initial release