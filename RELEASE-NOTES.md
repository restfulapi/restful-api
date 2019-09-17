#1.9.0
* Modify RestfulApiController to invalidate the session after the completion of each REST method as part of an overall solution to an application performance issue when not caching the database connection.
* Allow service name to be specified at the representation level - list requests only - so that a single service can be used for multiple resources when the underlying functionality is the same (ex: bulk list requests using alternate representations).

#1.8.0
* Replace overrideVersionRangeMediaType=true setting with useHighestSemanticVersion=true to dynamically replace all versioned media types with the highest semantic version where the major version matches. This is to facilitate easier caller adoption of non-breaking API changes. This feature requires an ApiVersionParser to be configured. 
* Add setting useAcceptHeaderAsMediaTypeHeader=true in Config.groovy to return the Accept request header as the X-Media-Type response header for some callers to delay transitioning to full semantic versioning of the X-Media-Type response header. 

#1.7.0
* Add setting overrideVersionRangeMediaType=true to allow override of a whole digit representation version number with the highest semantic version where the major version matches. This is in support of version ranges that facilitate easier caller adoption of non-breaking API changes. This feature requires an ApiVersionParser to be configured. 
* Correct extensibility to not execute WRITE SQL if the request is a POST /qapi request (this represents a query by post read-only request). 

#1.6.0
* Add support for specifying optional metadata at the representation level of a resource definition.
* Add feature that can parse the media types of resource representations to produce an ApiVersion object that can be associated with each representation. Generic media types (ex: application/json) can then be assigned the highest ApiVersion from the list of media types in that same resource representation. The media type header in the response can then be overwritten with the highest versioned media type. The ApiVersionParser is a bean specific to a restful-api application. BasicApiVersion and BasicApiVersionParser classes are provided as reference implementations.

#1.5.0
* Enhance PagedResultArrayList to allow the creation of the class omitting the total count. When there is no total count specified, TOTAL_COUNT will be omitted from the response. This is typically used only when the count method is not performant, and TOTAL_COUNT is not needed by any calling application.

#1.4.0
* Add feature to dynamically extend JSON request and response content.

#1.3.0
* Implement support for reporting and discovery of all resources configured for the restful-api (with optional metadata)
* Implement capability to restrict HTTP methods based on media type

#1.2.0
* Add feature that can force marshallers to remove null fields without having to specify it for each marshaller.
* Add feature that can force marshallers to remove empty collections (only when the marshaller is also configured to remove null fields)
* Add feature that can perform content filtering of JSON/XML request and response content.

#1.1.0
* Add feature for copying specific response headers to allow older headers to be deprecated.

#1.0.0
* BREAKING CHANGE.  The service layer contract and RestfulServiceAdapter interface have been changed so that all method signatures are consistent.  The resource id will no longer be passed as a separate parameter to the update and delete methods, as it is also available via the params map.  (Issue #8)  If you have services that expect the id to be passed explicitly, you can continue to support this behavior with use of RestfulServiceAdapter that extracts the id from the params and passes it in as a separate argument.
* BREAKING CHANGE.  The JSON and XML extractors will no longer parse dates in a lenient fashion by default (Issue #17).  In previous releases, a date such as '1999-99-99' would have sucessfully parsed (lenient parsing by default).  In 1.0.0, it will throw an exception, resulting in a 400 response.  You can retain the lenient behavior if necessary by setting lenientDates = true on the extractor.  See "Parsing Dates" in the README for details.
* Fix eTag not being computed correctly for collections (Issue #15).

#0.10.0
* BREAKING CHANGE.  By default, on a DELETE, the controller will ignore the Content-Type and request body, and send an empty content map to the service.  This is the opposite of previous behavior.  The original behavior can be obtained with the bodyExtractedOnDelete setting.  See the delete method documentation in the README.
* Optional marshalling of null fields.  By default, if a field has a null value when marshalled, the field is included in the rendered representation with a null value.  This behavior can now be overridden on a per-marshaller, or per-field basis so that a field with a null value is excluded from the representation.  See the marshallsNull and marshallsNullFields descriptions in the README.
* Fix Issue #16.  Upgrade to use v 1.1.7 of cache-headers.


#0.9.1
* Fix Issue #9 to improve error logging.
* Fix Issue #10 to make compatible with Grails 2.4.x.
* Various upgrades to the test application to make running it under Grails 2.4.x easier.

#0.9.0
* Add X-Request-ID support.
* Allow custom exception handlers to be configured to map application or framework errors to responses.
* Fix comparison between id field in content and params to convert the content field to a string first.  Add ability to configure a resource to ignore this check with the idMatchEnforced property in the DSL.
* Allow configuration of a 'jsonArrayPrefix' that will be added in front of a JSON Array, in order to protect from a CSRF vulnerability in older browsers (where the JSON Array constructor can be re-implemented). See http://haacked.com/archive/2008/11/20/anatomy-of-a-subtle-json-vulnerability.aspx

#0.8.0
First open source release.