
# 2.0.0

* BREAKING CHANGE. Established a dependency of Grails 2.5.2 and above. This is to allow use of HTTP PATCH. Consequently, the project is no longer compatible with previous Grails releases. The 1.x.x releases of this plugin will no longer be actively updated. 

* BREAKING_CHANGE: A 'general' error (one handled by the DefaultExceptionHandler) no longer includes the errorMessage. This is logged, but no longer included in the response to the client. 

* BREAKING CHANGE. Custom headers have been renamed as shown below. Note the old names may still be used by configuring these headers within the Config.groovy file.

Old name                | New name
----------------------- | ---
'X-hedtech-totalCount'  | X-Total-Count
'X-hedtech-pageOffset'  | (deprecated)
'X-hedtech-pageMaxSize' | (deprecated)
'X-hedtech-message'     | X-Status-Reason (existing, but now used for non-Error messages as well)
'X-hedtech-Media-Type'  | X-Media-Type

The X-Request-ID header name remains the same. 

Note the test-app unit and functional tests currently use the 'old' names. This just required an update to the test-app Config.groovy to configure the use of the old names.  Nevertheless, this is a 'breaking change'.

* Pagination information is now returned within a [Link Header](http://www.rfc-editor.org/rfc/rfc5988.txt). Note a LinkHeaderUtils.groovy utility class has been added and may be used to generate and parse a Link header (e.g., if you want to assert this header in your own tests). Note the X-hedtech-pageOffset and X-hedtech-pageMaxSize headers are deprecated but still included in the response.

* BREAKING CHANGE: The service contract now includes 'patch' to support HTTP Patch. The controller may be configured to either use this service 'patch' method or to apply a [JSON Patch](https://tools.ietf.org/html/rfc6902) itself before delegating to the service 'update' method. The JSON Patch support to apply a patch is implemented in the plugin (see JSONPatchSupport.groovy) as other implementations required too many dependencies. JSON Patch 'diff' support is not currently implemented. NOTE: Functional testing of 'patch' is currently ignored until Spring dependencies are updated with PATCH support. 

# 1.0.0

* BREAKING CHANGE.  The service layer contract and RestfulServiceAdapter interface have been changed so that all method signatures are consistent.  The resource id will no longer be passed as a separate parameter to the update and delete methods, as it is also available via the params map.  (Issue #8)  If you have services that expect the id to be passed explicitly, you can continue to support this behavior with use of RestfulServiceAdapter that extracts the id from the params and passes it in as a separate argument.
* BREAKING CHANGE.  The JSON and XML extractors will no longer parse dates in a lenient fashion by default (Issue #17).  In previous releases, a date such as '1999-99-99' would have sucessfully parsed (lenient parsing by default).  In 1.0.0, it will throw an exception, resulting in a 400 response.  You can retain the lenient behavior if necessary by setting lenientDates = true on the extractor.  See "Parsing Dates" in the README for details.
* Fix eTag not being computed correctly for collections (Issue #15).

# 0.10.0

* BREAKING CHANGE.  By default, on a DELETE, the controller will ignore the Content-Type and request body, and send an empty content map to the service.  This is the opposite of previous behavior.  The original behavior can be obtained with the bodyExtractedOnDelete setting.  See the delete method documentation in the README.
* Optional marshalling of null fields.  By default, if a field has a null value when marshalled, the field is included in the rendered representation with a null value.  This behavior can now be overridden on a per-marshaller, or per-field basis so that a field with a null value is excluded from the representation.  See the marshallsNull and marshallsNullFields descriptions in the README.
* Fix Issue #16.  Upgrade to use v 1.1.7 of cache-headers.


# 0.9.1

* Fix Issue #9 to improve error logging.
* Fix Issue #10 to make compatible with Grails 2.4.x.
* Various upgrades to the test application to make running it under Grails 2.4.x easier.

# 0.9.0

* Add X-Request-ID support.
* Allow custom exception handlers to be configured to map application or framework errors to responses.
* Fix comparison between id field in content and params to convert the content field to a string first.  Add ability to configure a resource to ignore this check with the idMatchEnforced property in the DSL.
* Allow configuration of a 'jsonArrayPrefix' that will be added in front of a JSON Array, in order to protect from a CSRF vulnerability in older browsers (where the JSON Array constructor can be re-implemented). See http://haacked.com/archive/2008/11/20/anatomy-of-a-subtle-json-vulnerability.aspx

# 0.8.0

First open source release.