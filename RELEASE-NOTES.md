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