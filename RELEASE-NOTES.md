#0.10.0
* BREAKING CHANGE.  By default, on a DELETE, the controller will ignore the Content-Type and request body, and send an empty content map to the service.  This is the opposite of previous behavior.  The original behavior can be obtained with the bodyExtractedOnDelete setting.  See the delete method documentation in the README.
* Optional marshalling of default fields.  By default, if a field has a null value when marshalled, the field is included in the rendered representation with a null value.  This behavior can now be overridden on a per-marshaller, or per-field basis so that a field with a null value is excluded from the representation.  See the marshallsNull and marshallsNullFields descriptions in the README.


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