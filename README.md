<!-- ********************************************************************
     Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************** -->

#RESTful API plugin documentation

##Status
Early Access.  The plugin may be used gain familiarity and provide testing feedback, but should not be used with any shipping software yet.  The plugin has not been through export compliance.

##Overview
The restful-api plugin is an implementation designed to conform to the our [API Strategy](http://m037138.ellucian.com:8082/job/Ellucian%20API%20Strategy%20Documentation/HTML_Report) document.  It is not intended to implement all optional features of the strategy.  It is not intended to provide support for multiple ways to accomplish the same requirements; in general, it implements the recommended approaches specified in the strategy.

##Installation
The recommended approach is to install the plugin as a git submodule.  Another option is to use a Maven repository.

###Git submodule
The plugin repo is located at ssh://git@devgit1/framework/plugins/restful-api.git.  Releases will be tagged as release-x.y.  For those familiar with Banner XE development please follow the rules for Banner XE on installing plugins as submodules.

In general, add the submodule to your Grails application:

        projects/restful_test_app (master)$ git submodule add ssh://git@devgit1/framework/plugins/restful-api.git plugins/restful-api.git
        Cloning into 'plugins/restful-api.git'...
        remote: Counting objects: 1585, done.
        remote: Compressing objects: 100% (925/925), done.
        remote: Total 1585 (delta 545), reused 309 (delta 72)
        Receiving objects: 100% (1585/1585), 294.45 KiB | 215 KiB/s, done.
        Resolving deltas: 100% (545/545), done.

Add the in-place plugin definition to BuildConfig.groovy:

        grails.plugin.location.'restful-api' = "plugins/restful-api.git"


###Maven
If you cannot use the recommended approach, you can also use the plugin via grail's dependency management.

To do so, define a new repository in your project's BuildConfig.groovy repositories section:

            mavenRepo name: "core-architecture",
                  root: "http://m039200.ellucian.com:8081/artifactory/core-architecture"

In the plugins section of BuildConfig.groovy add:

        compile "core-architecture:grails-restful-api:0.1"


Note that if you are using the artifactory repository, that this is a *internal only* site.  Source distributed to clients that depends on downloads from the internal artifactory repository will not work.  If you are not using the recommended git submodule approach, ensure that you package the plugin with your application or otherwise make it avaiable.

###Configure plugin dependencies
Irrespective of the method used to install the RESTful plugin, the following changes must be made to include the plugin dependencies.  The plugin depends on both the inflector plugin, and spock plugins.  (The spock dependency is for the RestSpecification testing class, that you may use to [test your API](#api-testing).

In the dependencies section of BuildConfig.groovy add:

    test "org.spockframework:spock-grails-support:0.7-groovy-2.0"

In the plugins section of BuildConfig.groovy add:


        compile ":inflector:0.2"

        test(":spock:0.7") {
          exclude "spock-grails-support"
        }

###Testing the Plugin
The plugin contains a test application that uses Spock to test the plugin. To run all tests from the plugin's root directory run:
```bash
grails clean && (cd test/test-restful-api && grails clean && grails test-app)
```

##Details

###Use of conventions
The plugin relies heavily on convention-over-configuration.  A request to a resource named 'things' will be delegated to the bean registered as 'thingService'.  De-pluralization of the resource name happens automatically; it is also assumed that there is a unique bean registered as thingService that can be looked up in the grails application.

You can override this convention by specifying a different service name in the configuration.

###Url Mapping
To use the restful plugin, you need to configure your UrlMappings.groovy to route the requests to the controller.

**Important:** parseRequest must be set to false; otherwise, grails will attempt to parse the request body and add values from it to the params object.  The restful plugin is designed to pass any content from the body to the service layer via an extracted map, not the params object.

    static mappings = {

        // Mappings supported by resource-specific controllers
        // should be added before the default mapping used for
        // resources handled by the default RestfulApiController.

        // name otherthingRestfulApi: "api/other-things/$id"

        // Default controller to handle RESTful API requests.
        // Place URL mappings to specific controllers BEFORE this mapping.
        //
        "/api/$pluralizedResourceName/$id"(controller:'restfulApi') {
            action = [GET: "show", PUT: "update",
                      DELETE: "delete"]
            parseRequest = false
            constraints {
                // to constrain the id to numeric, uncomment the following:
                // id matches: /\d+/
            }
        }
        "/api/$pluralizedResourceName"(controller:'restfulApi') {
            action = [GET: "list", POST: "create"]
            parseRequest = false
        }

        // Support for nested resources. You may add additional URL mappings to handle
        // additional nested resource requirements.
        //
        "/api/$parentPluralizedResourceName/$parentId/$pluralizedResourceName/$id"(controller:'restfulApi') {
            action = [GET: "show", PUT: "update", DELETE: "delete"]
            parseRequest = false
            constraints {
                // to constrain the id to numeric, uncomment the following:
                // id matches: /\d+/
            }
        }

        "/api/$parentPluralizedResourceName/$parentId/$pluralizedResourceName"(controller:'restfulApi') {
            action = [GET: "list", POST: "create"]
            parseRequest = false
        }



        "/"(view:"/index")
        "500"(view:'/error')
    }

Note that in order to conform to the strategy and for support for limiting methods exposed by a resource to operate correctly, all the api urls must conform to the pattern above.  That is, 'show', 'update', and 'delete' methods are mapped to urls that have an even number of parts after the api prefix; 'list' and 'create' methods map to urls that have an odd number of parts after the api prefix.

###Use of custom media types
The plugin relies on custom media types to specify resource representations.  For requests, the media type in the Content-Type header is used to identify the resource representation in the request body, in order to extract parameters from it to be passed to the resource's service.

Media types used must end with either 'json' or 'xml' (e.g. 'application/json' or 'application/vnd.hedtech.v0+xml').  Any media type ending with 'json' is assumed to be a format that can be parsed as JSON; any type ending with 'xml' is a format that is assumed to be parsed as XML.

###Media type of responses
For anything except a 500 response, the plugin will always return a Content-Type header of either 'application/xml' or 'application/json'.  This is intended as a convenience to viewing responses in browsers or other tools without having to configure them for all custom media types used by an API.  You can think of it as the Content-Type of the response identifies the format of the response body, but not the specific type of the representation contained within.

Successful responses generated by the plugin will always include a 'X-hedtech-Media-Type' header specifying the (versioned) media-type of the representation, e.g., 'application/vnd.hedtech.v0+json' or 'application/vnd.hedtech.minimal-thing.v0+xml'.  Consumers of the API can use the X-hedtech-Media-Type header to determine how to perform data binding on the resource representation returned.

###Media type of error responses
If a request results in an error response, the Media Type will always be 'application/json' or 'application/xml', based on whether the Accept header in the request specified an xml or json format.  If the Accept header could not be understood to request json or xml, then 'application/json' will be used for any data in the return body.

Errors are not considered part of the resource representation, and do not have versioned or custom representations.

###Media type of requests
The plugin uses the Content-Type header to determine the resource representation sent in a request body.  It uses the Accept header to determine the type of resource representation that should be used in the response.  The Content-Type and Accept headers on a request are not required to match.

###Content negotiation on requests
The plugin performs content negotiation for a request by parsing the Accept Header, taking any q-values into account.  The plugin will then use the best known representation for the accepted type(s).

For exmaple, for a request with an Accept Header of "application/xml;q=0.9,application/vnd.hedtech.v0+xml;q=1.0", the plugin would attempt to return the representation for 'application/vnd.hedtech.v0+xml', if one is configured.

Note that at present, the selection of representation is done based only on what representations are configured.  If an error occurs when marshalling a response to that representation, the plugin does not fall back to the next best representation as specified by the Accept Header.  This may change in future releases.

###Unsupported media types
If a request specified an unsupported media type in the Accept header, for a request that returns a reponse body, the plugin will respond with a 406 status code.
If the request specified an unsupported media type in the Content-Type header, for a request that processes a request body, the plugin will respond with a 415 status code.
A delete operation does not return a response body, and will not return a 406 regardless of the Accept header.
Request bodies for list and show operations are ignored (and will not generate a 415) regardless of the Content-Type header.

###Response envelope.
Any response body will be one of the following:

* A representation of a resource
* An array of representations of a resource (either a JSON array, or list representation in xml)
* an empty body

Any 'envelope' information is conveyed in headers.
Currently, the following response headers are supported:

* X-hedtech-totalCount.  Returned with list responses and contains the total count of objects.
* X-hedtech-pageOffset.  Returned with list responses.
* X-hedtech-pageMaxSize. Returned with list responses.
* X-hedtech-Media-Type.  Returned with all (sucess) responses, and contains the exact type of the response.
* X-hedtech-message.  May optionally be returned with any response.  Contains a localized message for the response.
* X-Status-Reason.  Optionally returned with a 400 response to provide additional information on why the request could not be understood.

##Cache Headers
The plugin supports client caching (unless disabled within Config.groovy).  When caching support is enabled, the plugin will include both 'ETag' and 'Last-Modified' HTTP Headers within responses to GET requests and will support conditional GET requests containing either an 'If-None-Match' or 'If-Modified-Since' header.  This support is provided by the [cache-headers](https://github.com/Grailsrocks/grails-cache-headers) plugin.

Specifically, a GET request (for either a resource or collection of resources) will include an ETag header (whose value is a SHA1 specifically calculated for the resource representation) and a 'Last-Modified' header (whose value is based upon a 'lastUpdated' or 'lastModified' property when available).

A subsequent conditional GET request containing an 'If-None-Match' header (whose value is an ETag value) will result in a '304 Not Modified' if processing the request results in a newly calculated ETag that is unchanged.  (Note this does not necessarily reduce server processing, but it does preclude sending unchanged content over the network.)  Similarly, when a GET request includes an 'If-Modified-Since' header, a 304 will be returned if the requested resource has not changed.  When the GET is for a collection, the latest 'lastUpdated' (or 'lastModified') date is used.

Note that at this time, conditional PUT requests are not supported.  Although a conditional PUT request is not supported, optimistic lock violations will be reported (so the end result is that a client will receive a 409 'conflict' versus a 412 'precondition failure' when using a conditional PUT request).

##Service layer contract
The plugin delegates to the following methods on a service:

* list
* count
* show
* create
* update
* delete

###list method
The list method will be passed the request parameters object directly.
If paging support is being used, params.max, and params.offset should be used to indicate the maximum number of results to return and the offset. Support for filtering the list is also provided as discussed [below](#filter-list).

The list method must return a list of the objects for the resource being listed.  These objects will be rendered as a resource representation via the configured marshallers.

###count method
The count method must return the total number of intances of the resource.  It is used in conjunction with the list method when returning a list of resources.  Support for filtering the count is also provided as discussed [below](#filter-list).

###show method
The show method will be passed the request parameters object directly.
It must return an object.  This object will be rendered as a resource representation via the configured marshallers for the resource.  For example, show may simply return a domain instance.

###create method
The create method is passed a content Map extracted from the request as well as the Grails params Map.  (The 'content' map is generated by the Extractor registered for the resource and format.)
The create method is responsible for using the map to create a new instance of the resource.  For domain services, it is recommended that the extractor produce a map that will work with data binding, e.g., the create method is passed a content Map containing the extracted content as well as the Grails params Map. It can create a new object as:

`new Thing( content )`

The create method must return an object representing the resource that was created.  This object will be rendered as a resource representation via the configured marshallers for the resource.  For example, create may simply return the domain instance created.

###update method
The update method is passed the resource id, a content Map extracted from the request, and the Grails params Map.  (The 'content' map is generated by the Extractor registered for the resource and format.)

The controller will first check that if the extracted map contains an 'id' property, that it matches the resource id passed in the url.  (That is, the controller assumes that an 'id' property extracted from the resource reprentation is the resource id).  The controller will raise an exception if this assumption is violated.

The update method is responsible for using the map to update an instance of the resource.

For domain services, it is recommended that the extractor produce a map that will work with data binding, e.g., the object can be updated by:

`thing.properties = content`

The update method must return an object representing the resource that was updated.  This object will be rendered as a resource representation via the configured marshallers for the resource.  For example, create may simply return the domain instance created.

###delete method
The delete method is passed the id of the resource to delete, (optionally) a content Map representing the content extracted from the request (if any), and (optionally) the Grails params map.  For example, a resource representation may be included as part of a delete request to convey optimistic locking information if deemed appropriate.

The controller will first check that if the extracted map contains an 'id' property, that it matches the resource id passed in the url.  (That is, the controller assumes that an 'id' property extracted from the resource reprentation is the resource id).  The controller will raise an exception if this assumption is violated.

The delete method returns void.

##Adapting an Existing Service
To support services that have a contract different than the service contract described above, a 'RestfulServiceAdapter' may be registered in the Spring application context. If an adapter is registered, it will be used by the RestfulApiController for all interaction with servcies. (Currently, only a single adapter may be registered, so it must be implemented to support all services that are used to support a RESTful API.)

##<a id="filter-list"></a>Filtering
As a convenience, the plugin provides a Filter class that includes a factory method (extractFilters) that may be used to create a list of Filter instances from the query parameters.  The URL query parameters that may be used with this class must be in the format:
```
part-of-things?filter[0][field]=description&filter[1][value]=6&filter[0][operator]=contains&filter[1][field]=thing&filter[1][operator]=eq&filter[0][value]=AZ&max=50
```
The filters may be used to filter on a resource's property whether a primitive property or one representing an associated resource.  Currently only single resource assocations are supported (i.e., not collections).  The operators include 'eq' (or 'equals') and 'contains' (which performs a case insensative 'ilike'-like comparison, although not actually using 'ilike' as that is not supported by Oracle). When filtering on a date or number, a 'filter\[n\]\[type\]' should be included with a value of 'date' or 'num' respectively. When filtering on a String, the 'type' component may (should) be omitted. When filtering on 'num' or 'date', the filter 'operator' may contain 'eq', 'gt' or'lt' but not 'contains'.

The plugin also includes an HQLBuilder utility class that will construct an HQL statement (and a corresponding substitution parameters map) from the request params object, when the resource corresponds to a domain object. This will leverage the Filter class to extract filters and will then construct the HQL statement.  Following is an example usage:
```
def query = HQLBuilder.createHQL( application, params )
def result = PartOfThing.executeQuery( query.statement, query.parameters, params )
```

Note the 'pluralizedResourceName' entry within the params map is used to determine the domain class to be queried, unless the params map explicitly specifies the domain class (which is useful when the resource name differs from the domain class name).
```
params << [ domainClass: Filter.getGrailsDomainClass( application, 'things' ) ]
def query = HQLBuilder.createHQL( application, params )
def result = PartOfThing.executeQuery( query.statement, query.parameters, params )
```

HQLBuilder is not a sophisticated query engine, and provides limited support for filtering by a primitive property and by a property that represent an association to another domain object.  HQLBuilder also handles whether the resource has been nested under another resource (i.e., when the params map contains a 'parentPluralizedResourceName' entry).  HQLBuilder is provided as a convenience but may not be suitable in all situations. Complex queries will likely require a specific implementation.

The createHQL method accepts a third argument which is a boolean, and if true indicates the statement should be generated to perform a 'count(*)' versus a select. That is, the HQLBuilder may be used in both list() and count() service methods to ensure the totalCount properly reflects the filters provided on the request.

##Supplemental data/affordances/HATEOS
Since the plugin does not use an envelope in the message body, any affordances must be present in the resource representations themselves.

The exact method of support is still being researched.  Current thinking is that supplemental data will be provided to the marshallers via meta-programming on the instance returned from a service method.  For example, consider a 'Thing' service, which needs to provide supplemental data on an object by computing a sha1 hash of several fields:

`MessageDigest digest = MessageDigest.getInstance("SHA1")
digest.update("code:${thing.getCode()}".getBytes("UTF-8"))
digest.update("description${thing.getDescription()}".getBytes("UTF-8"))
def properties = [sha1:new BigInteger(1,digest.digest()).toString(16).padLeft(40,'0')]
thing.metaClass.getSupplementalRestProperties << {-> properties }`

Note that the getSupplementalRestProperties method is being added only to the single object instance, not the entire class.  A marshaller can check to see whether the instance it is marshalling support the method, and if so, extract data from it to generate affordances.

##Exception handling
When an exception is encountered while servicing a request, the controller will classify the exception into one of the following existing categories:

* ApplicationException
* OptimisticLockException
* Validation Exception
* UnsupportedRequestRepresentationException
* UnsupportedResponseRepresentationException
* AnyOtherException

Each one of these categories has a registered handler that specifies the status code to return, along with any additional headers or response body.

Except for the ApplicationException category, the other categories represent hard-coded responses mapped to exceptions as follows:

* OptimisticLockException: if the exception is an instance of org.springframework.dao.OptimisticLockingFailureException
* Validation Exception: if the exception is an instance of grails.validation.ValidationException
* UnsupportedRequestRepresentationException: internal exception thrown when a request  specifies a Content-Type that cannot be supported
* UnsupportedResponseRepresentationException: internal exception thrown when a request specifies a media type in the Accept header that cannot be supported.
* AnyOtherException: encountered an exception that doesn't fit any other category.  Will result in a 500 status

###ApplicationException
The ApplicationException is treated as a special case that allows applications using the plugin to customize how their exceptions map to response codes.

An ApplicationException is not determined by inheritance; instead duck typing is used.  If the controller encounters an exception that responds to 'getHttpStatusCode' (it has a method getHttStatusCode()) and has a property named 'returnMap' that is an instance of Closure, then the controller will treat that exception as an ApplicationException, and extract data from it as follow:

* getHttpStatusCode() will be invoked to obtain the http status code that should be returned
* the returnMap closure will be invoked, passing in a localizer so that localized messages can be contructed.  The closure must return a Map; entries in the map will be used as follows:
    * if the map contains a 'headers' key, the value is expected to be a map of header names/header values to return in the error response
    * if the map contains a 'message' key, the value is expected to be a (localized) string to be returned in the X-hedtech-message header.
    * if the map contains an 'errors' key, the value is expected to be an object that is to be rendered as JSON or xml in the response body

This definition of ApplicationException allows any application to customize error handling without extending or overriding any controller methods.  However, the implementation of any application exceptions must take responsibility for conforming to the Ellucian REST strategy.  For example, if an application exception represents a validation exception, it needs to return a 400 status code, and should also return an 'X-Status-Reason:Validation failed' header.

Also note that the contract on what to recognize as an application exception was chosen to be compatible with the existing Banner Core ApplicationException.

##Data binding and response marshalling
The overall processing of a request proceeds as follows:

* The plugin is responsible for content negotiation, parsing the Accept and Content-Type headers
* For request with bodies to process, the controller media type from the Content-Type header to identify an extractor for the resource.  If no extractor is configured for that resource representation, the representation is treated as unsupported (415 status)
* The controller parses the request body as either JSON or xml, and passes the JSONObject or GPathResult to the appropriate extractor.
* The controller passes the Map returned from the extractor to the appropriate service method.
* The controller takes the map returned by the service method, and selects a resource representation based on the best match for the Accept header  to marshall the response into JSON or xml.  If none of the media types in the Accept header identify a supported representation of the resource, a 406 status will be returned.
* The controller renders the response along with appropriate associated headers and status code.
* If at any point, an exception is thrown, it is rendered according to the rules in Exception handling (see above).

##Configuration
The controller uses the grails converter mechanism to marshall responses, and extractor classes to convert resource representations in requests to Map instances that are passed to the services.

Configuration is performed by assigning a closure to the restfulApiConfig property in the grails configuration.

For example, in Config.groovy:

    restfulApiConfig = {
        resource 'things' config {
            representation {
                mediaTypes = ["application/json"]
                marshallers {
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.json.BasicDomainClassMarshaller(app:grailsApplication)
                        priority = 100
                    }
                }
                extractor = new net.hedtech.restfulapi.extractors.json.DefaultJSONExtractor()
            }
            representation {
                mediaTypes = ["application/xml"]
                jsonAsXml = true
                marshallers {
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.xml.JSONObjectMarshaller()
                        priority = 200
                    }
                }
                extractor = new net.hedtech.restfulapi.extractors.xml.JSONObjectExtractor()
            }
        }
    }

This declares that a resources 'things' is to be exposed via the plugin.  Resource 'things' has two available representations, one for json, and one for xml.  In the case of the json representation, a named configuration for the JSON converter will be created (based on the resource name and media type), and the BasicDomainClassMarshaller will be registed under that configuration, and that JSON converter configuration will be used to marshall any thing representation for 'application/json'.  Any requests for things with a Content-Type of 'application/json' will use the DefaultJSONExtractor to convert the resource representation to a map to be passed to the backing service.

Note that each resource representation gets its own isolated set of marshallers, allowing for complete control of how any objects are marshalled for that representation.  In particular, this allows for versioned representations.

grailsApplication can be used within the restfulApiConfig closure; and references the grailsApplication instance as expected.

Consider another example in which we are supporting versioned json representations of two resources:

    restfulApiConfig = {
        resource 'colleges' config {
            representation {
                mediaTypes = ["application/vnd.hedtech.v0+json"]
                marshallers {
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.json.v0.CollegeMarshaller(grailsApplication)
                        priority = 100
                    }
                }
                extractor = new net.hedtech.restfulapi.extractors.json.v0.CollegeExtractor()
            }
            representation {
                mediaTypes = ["application/vnd.hedtech.v1+json"]
                marshallers {
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.json.v1.CollegeMarshaller(grailsApplication)
                        priority = 100
                    }
                }
                extractor = new net.hedtech.restfulapi.extractors.json.v1.CollegeExtractor()
            }
        }
        resource 'students' config {
            representation {
                mediaType = ["application/vnd.hedtech.v0+json"]
                marshallers {
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.json.v0.StudentMarshaller(grailsApplication)
                        priority = 100
                    }
                }
                extractor = new net.hedtech.restfulapi.extractors.json.v0.StudentExtractor()
            }
        }
    }

This configuration exposes 2 resources, 'colleges' and 'students'.  The 'colleges' resource support two versioned representations, while the 'students' resource only has one.  Note that the custom media types are re-used across the resources.

####Whitelisting vs dynamic exposure of resources
The plugin allows you to explicitly whitelist resources to be exposed - an appropriate error response code will be thrown for resources not listed.  You can also dynamically expose resources, in which case any resource name that maps by convention to a service can be serviced.

To dynamically expose services, you specify an anyResource block:

    restfulApiConfig = {
        anyResource {
            representation {
                mediaTypes = ["application/json"]
                marshallers {
                    jsonDomainMarshaller {
                        priority = 101
                    }
                    jsonGroovyBeanMarshaller {
                        priority = 100
                    }
                }
                jsonExtractor {}
            }
        }
    }

The configuration in the anyResource block applies to any resource that isn't explicitly listed in the configuration.  anyResource would be primarily used when you want to dynamically expose resources, and are not using versioned representations; e.g., you have a 'simple' system in which the resource representations can map directly to the objects they represent and the json or xml can be dynamically generated.  The above example uses the declarative domain and groovy bean marshallers to handle all resources.

Any configuration options for a resource/representation listed below can be applied to the anyResource block as well.  (So you can assign affordances, etc.)

If you define an anyResource block, and then expose one or more resources explicitly, the whitelisted resource configuration is used for that resource, with no fallback.  For example:

    restfulApiConfig = {
        anyResource {
            representation {
                mediaTypes = ["application/json"]
                marshallers {
                    jsonDomainMarshaller {
                        priority = 101
                    }
                    jsonGroovyBeanMarshaller {
                        priority = 100
                    }
                }
                jsonExtractor {}
            }
        }

        resource 'things' config {
            representation {
                mediaTypes = ["application/xml"]
            }
        }
    }

In this configuration, 'things' does not have a json representation.  The configuration in an anyResource block only applies to a resource that is not explicitly listed.

####Default representations
You can also assign multiple media types to the same representation.  The most common use for this is to have a media type such as 'application/json' represent a particular version as a default.  For example:

    restfulApiConfig = {
        resource 'colleges' config {
            representation {
                mediaTypes = ["application/vnd.hedtech.v0+json"]
                marshallers {
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.json.v0.CollegeMarshaller(grailsApplication)
                        priority = 100
                    }
                }
                extractor = new net.hedtech.restfulapi.extractors.json.v0.CollegeExtractor()
            }
            representation {
                mediaTypes = ["application/vnd.hedtech.v1+json", "application/json"]
                marshallers {
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.json.v1.CollegeMarshaller(grailsApplication)
                        priority = 100
                    }
                }
                extractor = new net.hedtech.restfulapi.extractors.json.v1.CollegeExtractor()
            }
        }
    }

Both 'application/vnd.hedtech.v1+json' and 'application/json' identify the same representation.  As new versions are added, the 'application/json' can be moved to them, so that a client that always wants the latest/default representation can obtain it with 'application/json'.

####Marshaller groups
It is likely that you will have marshallers that you will want to re-use across multiple resources/representations; for example, a common marshaller for objects like dates, addresses, etc.  You can configure a collection of marshallers as a marshaller group:

    restfulApiConfig = {
        marshallerGroups {
            groups 'defaultJSON' marshallers {
            marshaller {
                instance = new net.hedtech.restulfapi.marshallers.json.DateMarshaller()
                priority = 100
            }
            marshaller {
                instance = new net.hedtech.restulfapi.marshallers.json.AddressMarshaller(grailsApplication)
                priority = 100
            }
        }
        resource 'students' config {
            representation {
                mediaTypes = ['application/json']
                marshallers {
                    marshallerGroup 'defaultJSON'
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                        priority = 100
                    }
                }
            }
        }
    }

This configuration defines a reusable group of marshallers named 'defaultJSON' containing marshallers for dates and addresses.  The 'students' resource representation is using those marshallers.  The above configuration is equivalent to:

    restfulApiConfig = {
        resource 'students' config {
            representation {
                mediaTypes = ['application/json']
                marshallers {
                    marshaller {
                        instance = new net.hedtech.restulfapi.marshallers.json.DateMarshaller()
                        priority = 100
                    }
                    marshaller {
                        instance = new net.hedtech.restulfapi.marshallers.json.AddressMarshaller(grailsApplication)
                        priority = 100
                    }
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                        priority = 100
                    }
                }
            }
        }
    }

####Ordering
Configuration elements are applied in the order in which they are encountered.  This means that ordering in the configuration is signficant.  For example:

    restfulApiConfig = {
        resource 'students' {
            representation {
                mediaTypes = ['application/json']
                marshallers {
                    marshallerGroup 'student'
                }
            }
        }
        marshallerGroups {
            group 'student' marshallers {
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                    priority = 100
                }
            }
        }
    }

Will result in an exception, as the first reference to the 'student' marshaller group occurs before the group is defined.

####Overriding the service used for a resource.
By default, the name of the service to use is derived from the name of the resource.  You can override the name of the service bean to use for the resource with the 'serviceName' property when configuring a resource.  For example:

    restfulApiConfig = {
        marshallerGroup {
            name = 'student'
            marshaller {
                instance = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                priority = 100
            }
        }
        resource 'students' config {
            serviceName = 'studentFacadeService'
            representation {
                mediaTypes = ['application/json']
                marshallers {
                    marshallerGroup 'student'
                }
            }
        }

    }

Will cause the list, show, create, update, and delete methods for the resource 'students' to be delegated to the bean registered under the name 'studentFacadeService' instead of 'studentService'.

####Limiting methods for a resource
By default, any configured resource supports 'list', 'show', 'create', 'update', and 'delete' methods.  You can customize which methods are exposed by specifying a subset of the 5 methods as an array assigned to the methods property of a resource.

For example, to expose students as a read-only resource that just supports list and show:

    restfulApiConfig = {
        marshallerGroup {
            name = 'student'
            marshaller {
                instance = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                priority = 100
            }
        }
        resource 'students' config {
            methods = ['list','show']
            representation {
                mediaTypes = ['application/json']
                marshallers {
                    marshallerGroup 'student'
                }
            }
        }
    }

If a request for an unsupported method is received, the plugin will respond with a 405 status code and an Allow header specifying the HTTP methods the url supports.

Note that if you are using the grails-cors plugin to support CORS functionality, the OPTIONS method will return results that are inconsistent with the methods actually supported.  That is, an OPTIONS request to an api url will always return that all methods are supported, even if the configuration restricts the methods for a given resource.  This may be addressed in a contribution to a future release of the CORS plugin.


##JSON-as-xml
The plugin supports a 'cheap' method of supporting both json and xml representation, while only requiring JSON marshallers/extractors to be created.
To enable it for a representation, set jsonAsXml = true and specify the default jsonAsXml marshaller and extractor:

    representation {
        mediaTypes = ["application/xml"]
        jsonAsXml = true
        marshallers {
            marshaller {
                marshaller = new net.hedtech.restfulapi.marshallers.xml.JSONObjectMarshaller()
                priority = 100
            }
        }
        extractor = new net.hedtech.restfulapi.extractors.xml.JSONObjectExtractor()
    }

For producing responses with this representation, the controller will:

* convert the xml media type to the equivalent json by means of convention.  For example, 'application/xml' becomes 'application/json'.
* use the json marshaller chain for that media type to produce a JSON resource representation.
* parses the json representation into a JSONObject, then uses the marshaller chain for the xml format to render the JSONObject as xml.

The plugin includes a JSONObjectMarshaller designed to convert a JSONObject to xml in a consistent manner.  Note that it is not possible to achieve a 1-to-1 mapping between JSON an xml.  For example, JSON allows backspaces and form-feeds to be encoded, while these characters are illegal in xml 1.0.

For handling request with a jsonAsXml representation, the controller will

* parse the xml content and use the configured extractor to construct a JSONObject from it.
* convert the xml media type to the equivalent json by means of convention.  For example, 'application/xml' becomes 'application/json'.
* use the extractor defined for the equivalent json media type to conver the (intermediate) json object to a map to pass to the service.

The json-as-xml support is not intended as a complete replacement for custom xml marshallers and extractors, but could be used in situations where xml support is required, but clients can understand 'json-like' semantics, but are using an xml parser/marshaller toolkit.

##Marshalling
To fully take advantage of marshalling, you should understand how the grails converters for JSON and xml work.  The plugin takes advantage of named converter configurations.  Each resource/representation in the configuration results in a new named converter configuration for JSON or XML.  When marshalling an object, the restful-api controller will use that named configuration to marshall the object.  It does this by asking each marshaller in the named configuration (marshallers with higher priority first) if the marshaller supports the object.  This means that the marshallers registered for a resource/representation are used only for that representation, and is what allows the plugin to support different representations for the same resource.

However, you should be aware that these named configurations will fallback to the default converter configurations if they cannot locate a marshaller within the named config.  For example, let's say in your application's bootstrap you add

    JSON.registerObjectMarshaller(Date) { return it?.format("yyyy-MM-dd'T'HH:mm:ssZ") }

This registers a JSON marshaller for java.util.Date.  If you have an object with a date field, and define a representation for it, when the grails converter tries to marshall the date field, it will fallback to the above object marshaller.  You can take advantage of this behavior to establish default marshalling behavior for common objects like dates.

##Declarative Marshalling of Domain classes to JSON
The plugin contains a BasicDomainClassMarshaller and a DeclarativeDomainClassMarshaller, designed to simplify marshalling of grails domain objects to a json representation.
The BasicDomainClassMarshaller will marshall persistent fields (except for some excluded ones, such as 'password'), and contains a number of protected methods which can be overridden in subclasses to customize marshalling behavior.  For example, additional fields can be added to the representation to support affordances.

Use of the BasicDomainClassMarshaller requires new subclasses to be created to customize marshalling behavior.  Marshalling of domain classes can be customized without resorting to writing custom marshallers however with the DeclarativeDomainClassMarshaller.

The DeclarativeDomainClassMarshaller is a marshaller that can be used to customize json representations without code.

By default, the DeclarativeDomainClassMarshaller behaves the same as the BasicDomainClassMarshaller; however, it can be configured to include or exclude fields, add custom affordances or other fields, rename fields, and customize how references to associated objects are rendered.

To use the marshaller directly, see the javadocs for the class.

The preferred way to utilize the class is to use the built-in support for the class in the configuration DSL.

Anywhere you can add a marshaller (in a marshaller group or a representation), you can configure and add a json declarative domain marshaller with

    jsonDomainMarshaller {}

The closure specifies how to configure the marshaller.  Specifying no information will register a marshaller that behaves identically to BasicDomainClassMarshaller; that is, it will marshall all but the default excluded fields, and use conventions to determine resource names.

The best way to describe the use of the marshaller is by examples.

###Limiting the marshaller to a class (and it's subclasses)
By default, a declarative domain marshaller will support any object whose class is a grails domain class.  If you are including or excluding fields however, it is likely that you want an instance of the marshaller for a particular class (or subclasses).  You can control this with the supports option:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                }
            }
        }
    }


This will register a declarative domain marshaller that will support the Thing class, and any subclasses of Thing.  Note that it is your responsibility to ensure that Thing is a grails domain class - if it is not, you should register a different type of marshaller for it.

###Excluding specific fields from a representation
By default, the json domain marshaller will ignore any non-persistent fields and any field named 'lastModified', 'lastModifiedBy', 'dataOrigin', 'createdBy', or 'password'.  You can exclude additional fields with the excludedFields block:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    excludesFields {
                        field 'description'
                        field 'manufactureDate'
                    }
                }
            }
        }
    }

This will marshall all fields in Thing except for the default excluded ones and the 'description' and 'manufactureDate' fields.

###Renaming fields
By default, when a field is marshalled, its name in the representation is identical to its name in the class.  You can override this behavior by specifying a substitute name for a field:

     resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    field 'code'        name 'productCode'
                    field 'description' name 'desc'
                }
            }
        }
    }

This will marshall all fields in Thing, but the value of the 'code' field will be marshalled to 'productCode', and the value of the 'description' field will be marshalled to 'desc'.

###Including only specified fields.
There are times when you want to include only certain fields in a representation such as producing a 'lightweight' representation for a list, or when versioning representations.  You can do so with the includedFields block:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    includesFields {
                        field 'code'
                        field 'description'
                    }
                }
            }
        }
    }

This will marshall only the 'code' and 'description' fields into the representation.  You can rename the fields as well.

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    includesFields {
                        field 'code' name 'productCode'
                        field 'description'
                    }
                }
            }
        }
    }

If you specify both an includedFields and excludedFields block, the excludedFields block will be ignored.

###Representing associations
By default, the declarative marshaller renders the objects in any assocation as 'short objects'.  The default rendering of a 'short object' is a JSON object containing a single '_link' property having a value of '/resource/id' where resource is the pluralized resource name of the associated object (as derived by convention), and id is the id of the object.  So, for example, if the Thing class had a field called customer  holding a reference to an instance of class Customer with id 15, the customer field would render as :

    customer:{"_link":"/customers/15"}

###Representing associations where conventions cannot be used.
What happens when you are adding restful APIs to a legacy system, where domain class names do not represent singularized versions of the resource name?  For example, consider a class Thing with a field customer that is a 1-1 association to an instance of the LegacyCustomer class.

We have decided that LegacyCustomer will be exposed as the 'customers' resource, but when the declarative marshaller marshalls the customer class as a short object, we will get:

    customer:{"_link":"/legacy-customers/15"}

In this case, we can override the default behavior of pluralizing and hyphenating the domain class name to get the resource name, and specify the resource name directly:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    field 'customer' resource 'customers'
                }
            }
        }
    }

Now when the customer field is marshalled, we will get:

    customer:{"_link":"/customers/15"}

Note that you can use both name and resource options on a field definition (both inside and outside of an include block), and in either order.  For example, the following are all equivalent:

    field 'customer' name 'preferredCustomer' resource 'customers'

    field 'customer' resource 'customers' name 'preferredCustomer'

However, if you declare the same field multiple times, the final declaration is the one that will be used.  For example

    field 'customer' name 'preferredCustomer' resource 'customized-customers'
    includesFields {
        field 'customer'
    }

will result in the 'customer' field being output with the default name ('customer') and the default resource name.  That is, the second "field 'customer'" declaration completely replaces the first one; they are not merged.


###Customizing short-object behavior.
You can override how a declarative json marshaller renders short-objects by specifying a closure that generates the content.  The marshaller will
automatically pass the closure a Map containing the following keys:

        grailsApplication
        property
        refObject
        json
        resourceId
        resourceName

where:

* grailsApplication is a reference to the grailsApplication context
* property is a GrailsDomainClassProperty instance for the field being marshalled
* refObject is the associated object to generate a short-object representation for
* json is the JSON converter that should be used to generate content
* resourceId is the id of the refObject
* resourceName is the resource name that the refObject is exposed as by the API.  If a field declaration for the field being rendered contains a 'resource' option, that value is passed in resourceName.  Otherwise, the pluralized, hyphenated domain name will be passed.

If the configuration specifies a resource name for the field being marshalled as a short object, that will be passed as a resource name, otherwise, resource will be the pluralized and hyphenated version of the associated class (per convention).

For example, if you wanted the short-object representation to contain a "link" field, as well as separate fields for id and resource name, you could override the default behavior this way:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    includesFields {
                        field 'code'
                        field 'description'
                        shortObject { Map map ->
                            def json = map['json']
                            def writer = json.getWriter()
                            def resource = map['resourceName']
                            def id = map['resourceId']
                            writer.object()
                            writer.key("link").value("/$resource/$id")
                            writer.key("resource").value(resource)
                            writer.key("id").value(id)
                            writer.endObject()
                        }
                    }
                }
            }
        }
    }

Note that since the map contains the grails applicatin context, you can also look up and delegate to services to generate content for the short-object representation, etc.

###Changing short-object representations globally.
Obviously, you don't want to copy the short object closure to every representation - in general, you would want consistent short object rendering for all resources.

You can define the short object closure once, and re-use it for all declarative json marshallers with a domain marshaller template.  Templates must be defined before their use, but then any jsdon domain marshaller declaration can inherit them.

    jsonDomainMarshallerTemplates {
        template 'json-shortObject' config {
            shortObject { Map map ->
                def json = map['json']
                def writer = json.getWriter()
                def resource = map['resourceName']
                def id = map['resourceId']
                writer.object()
                writer.key("link").value("/$resource/$id")
                writer.key("resource").value(resource)
                writer.key("id").value(id)
                writer.endObject()
            }
        }
    }

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits=['json-shortObject']
                    supports net.hedtech.restfulapi.Thing
                }
            }
        }
    }

    resource 'customers' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits=['json-shortObject']
                    supports net.hedtech.restfulapi.Customer
                }
            }
        }
    }

Now we have defined the short-object behavior in one location, and have multiple resource representations re-using it.  We will cover marshaller templates in more detail in a later section.

###Adding additional fields
In general, it is necessary to add non-persistent fields to the representation.  The most common requirement is to add affordances, such as an _href attribute referring to the resource, or supplemental data not present as persistent fields.

The declarative domain marshaller allows any number of closures to be added to marshall additional content.  For example, let's say we want to add affordances to all of our json representations.  We will define a marshaller template containing the closure for the affordance, then add it to the marshallers:

    jsonDomainMarshallerTemplates {
        template 'json-affordance' config {
            additionalFields {Map map ->
                map['json'].property("_href", "/${map['resourceName']}/${map['resourceId']}" )
            }
        }
    }

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits=['json-affordance']
                    supports net.hedtech.restfulapi.Thing
                }
            }
        }
    }

    resource 'customers' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits=['json-affordance']
                    supports net.hedtech.restfulapi.Customer
                }
            }
        }
    }

Like with overriding short-object behavior, the closure receives a map.  The map contains the following keys:

    grailsApplication
    beanWrapper
    json
    resourceName
    resourceId

where

* grailsApplication is a reference to the grailsApplication context
* beanWrapper is a BeanWrapper instance wrapping the object being marshalled
* json is the JSON converter that should be used to generate content
* resourceName is the resource name obtained as the pluralized, hyphenated version of the domain class
* resourceId is the id of the domain object

You can pass in additional values to the closures by specifying an additionalFieldsMap:

    resource 'customers' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Customer
                    additionalFields { Map m -> //some content}
                    additionalFieldsMap = ['a':'b']
                }
            }
        }
    }

The values specified in the additionalFieldsMap will be merged into the map passed into the additional fields closures.  Note that this can be used to override the resourceName when creating affordances.  For example:

    jsonDomainMarshallerTemplates {
        template 'json-affordance' config {
            additionalFields {Map map ->
                map['json'].property("_href", "/${map['resourceName']}/${map['resourceId']}" )
            }
        }
    }

    resource 'some-things' config {
        representation {
            mediaTypes = ["application/json"]
            serviceName = 'thingService'
            marshallers {
                jsonDomainMarshaller {
                    inherits=['json-affordance']
                    supports net.hedtech.restfulapi.Thing
                    additionalFieldsMap = ['resourceName':'some-things']
                }
            }
        }
    }

This will cause the affordances generated for the 'some-things' resource to look like

    "_href":"/some-things/123"

instead of using the derived resource name of 'things'.

You can use additionalFields multiple times on any declarative marshaller configuration.
For example:

    resource 'customers' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Customer
                    additionalFields { Map m -> //some content}
                    additionalFields { Map m -> //some more content}
                }
            }
        }
    }

However, it is recommended to keep (resuable) field code, such as for supplemental data and affordances that cross-cut resources in separate templates.  For example

    jsonDomainMarshallerTemplates {
        template 'json-affordance' config {
            additionalFields {Map map ->
                //add affordances
            }
        }
        template 'json-supplemental' config {
            additionalFields {Map map ->
                //add supplemental fields
            }
        }
    }

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits=['json-affordance','json-supplemental']
                    supports net.hedtech.restfulapi.Thing
                }
            }
        }
    }

    resource 'customers' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits=['json-affordance']
                    supports net.hedtech.restfulapi.Customer
                }
            }
        }
    }

This allows specific behavior to be re-used as needed. For example, the 'things' representation will contain both affordances and supplemental data, while the 'customers' representation only has affordances added.

###Controlling whether id and version information is included.
By default the declarative domain marshaller will automatically add 'id' and 'version' fields for the object id and (optimistic lock) version values.  If they should not be included in a representation, you can turn them off:

    resource 'customers' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Customer
                    includesId false
                    includesVersion false
                }
            }
        }
    }

###Full list of configuration elements for a json domain marshaller
The configuration block for the marshaller can contain the following in any order:

    inherits = <array of json marshaller template names>
    supports <class>
    <field-block>*
    includesFields {
        <field-block>*
    }
    excludesFields {
        <fieldsBlock>*
    }
    includesId <true|false>
    includesVersion <true|false>
    additionalFields <closure>
    shortObject <closure>

Where \<field-block\>* is any number of field-blocks, and a field-block is

    field 'name' [name 'output-name'] [resource 'resource-name']

Where values in brackets are optional.

The elements in the configuration block can occur in any order, and repeat any number of times; however, later values will override earlier values.  So, for example:

        includesId true
        includesId false

is equivalent to

        includesId false.

Another example:

        field 'foo' name 'bar'
        includesFields {
            field 'foo' name 'foobar'
        }

is equivalent to

        includesFields {
            field 'foo' name 'foobar'
        }

If an includedFields block is present, any excludedFields block is ignored:

        includesFields {
            field 'foo'
            field 'bar'
        }

        excludesFields {
            field 'baz'
        }

        includesFields {
            field 'baz'
        }

is the same as

        includesFields {
            field 'foo'
            field 'bar'
            field 'baz'
        }

###Domain marshaller templates
JSON domain marshaller templates are configuration blocks that do not directly create a marshaller.  The 'config' block accepts any configuration that the jsonDomainMarshaller block does (including the inherits option).  When a jsonDomainMarshaller directive contains an 'inherits' element, the templates referenced will be merged with the configuration for the marshaller in a depth-first manner.  Elements that represent collections or maps are merged together (later templates overriding previous ones, if there is a conflict), with the configuration in the jsonDomainMarshaller block itself overriding any previous values.

In general, templates are useful for defining affordances and short-object behavior that need to be applied across many representations.

###Template inheritance order.
When a json domain marshaller declaration includes an inherits directive, then the configuration of each template is merged with the declaration itself in depth-first order.  For example, consider the use of nested configuration:

    jsonDomainMarshallerTemplates {
        template 'one' config {
            //some config
        }
        template 'two' config {
            //some config
        }
        template 'three' config {
            inherits = ['one','two']
            //some config
        }
        template 'four' config {
            //some config
        }
    }

    resource 'customers' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits = ['three','four']
                    supports net.hedtech.restfulapi.Customer
                    includesId false
                    includesVersion false
                }
            }
        }
    }

The domain marshaller will be configured with the results of merging the configuration blocks in the following order: 'one', 'two', 'three', 'four' and the contents of the jsonDomainMarshaller block itself.

###Configuration merging
The order in which configurations are merged is significant.  When two configurations, first and second are merged, boolean values, or single valued options that are set in the second config override the first.  Collection or map values are combined.

##Declarative Marshalling of Groovy Beans to JSON
The plugin contains a GroovyBeanMarshaller and a DeclarativeGroovyBeanMarshaller, designed to simplify marshalling of groovy beans to a json representation.  The functioning of the marshallers is similar to the domain class marshallers, but operate against groovy bean instances, intead of domain objects.  (Of course, where a domain object can be treated as a groovy bean, the bean marshallers can also be used.)  The options these marshallers support are very similar to those of the domain marshallers, except that the bean marshallers do not have support for recognizes object associations.

The GroovyBeanMarshaller will marshall properties, and public non-static/non-transient fields of a groovy bean.  The properties 'class', 'metaClass', and 'pasword' are automatically excluded from being marshalled.

Use of the GroovyBeanMarshaller requires new subclasses to be created to customize marshalling behavior.  Marshalling of groovy beans can be customized without resorting to writing custom marshallers with the DeclarativeGroovyBeanMarshaller.

The DeclarativeGroovyBeanMarshaller is a marshaller that can be used to customize json representations without code.

By default, the DeclarativeGroovyBeanMarshaller behaves the same as the GroovyBeanMarshaller; however, it can be configured to include or exclude fields, add custom affordances or other fields, and rename fields.

To use the marshaller directly, see the javadocs for the class.

The preferred way to utilize the class is to use the built-in support for the class in the configuration DSL.

Anywhere you can add a marshaller (in a marshaller group or a representation), you can configure and add a json declarative groovy bean marshaller with

    jsonGroovyBeanMarshaller {}

The closure specifies how to configure the marshaller.  Specifying no information will register a marshaller that behaves identically to GroovyBeanMarshaller; that is, it will marshall all but the default excluded fields.

The best way to describe the use of the marshaller is by examples.

###Limiting the marshaller to a class (and it's subclasses)
By default, a declarative groovy bean marshaller will support any object that is an instance of GroovyObject.  If you are including or excluding fields however, it is likely that you want an instance of the marshaller for a particular class (or subclasses).  You can control this with the supports option:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonGroovyBeanMarshaller {
                    supports net.hedtech.restfulapi.Thing
                }
            }
        }
    }


This will register a declarative groovy bean marshaller that will support the Thing class, and any subclasses of Thing.  Note that it is your responsibility to ensure that Thing can be treated as a groovy bean - if it is not, you should register a different type of marshaller for it.

###Excluding specific fields from a representation
By default, the json groovy bean marshaller marshall all properties, and any public non-static non-transient field.  You can exclude additional fields or properties with the excludedFields block:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonGroovyBeanMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    excludesFields {
                        field 'description'
                        field 'manufactureDate'
                    }
                }
            }
        }
    }

This will marshall all fields in Thing except for the default excluded ones and the 'description' and 'manufactureDate' fields.

###Renaming fields
By default, when a field is marshalled, its name in the representation is identical to its name in the class.  You can override this behavior by specifying a substitute name for a field:

     resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonGroovyBeanMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    field 'code'        name 'productCode'
                    field 'description' name 'desc'
                }
            }
        }
    }

This will marshall all fields in Thing, but the value of the 'code' field will be marshalled to 'productCode', and the value of the 'description' field will be marshalled to 'desc'.

###Including only specified fields.
There are times when you want to include only certain fields in a representation such as producing a 'lightweight' representation for a list, or when versioning representations.  You can do so with the includedFields block:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonGroovyBeanMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    includesFields {
                        field 'code'
                        field 'description'
                    }
                }
            }
        }
    }

This will marshall only the 'code' and 'description' fields into the representation.  You can rename the fields as well.

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonGroovyBeanMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    includesFields {
                        field 'code' name 'productCode'
                        field 'description'
                    }
                }
            }
        }
    }

If you specify both an includedFields and excludedFields block, the excludedFields block will be ignored.

###Adding additional fields
You can add additional fields not directly present in a groovy bean to its marshalled representation.

The declarative groovy bean marshaller allows any number of closures to be added to marshall additional content.  For example, let's say we want to add affordances to all of our json representations.  We will define a marshaller template containing the closure for the affordance, then add it to the marshallers:

    jsonGroovyBeanMarshallerTemplates {
        template 'json-bean-affordance' config {
            additionalFields {Map map ->
                map['json'].property("_href", "/${map['resourceName']}/${map['resourceId']}" )
            }
        }
    }

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonGroovyBeanMarshaller {
                    inherits=['json-bean-affordance']
                    supports net.hedtech.restfulapi.Thing
                }
            }
        }
    }

the closure receives a map.  The map contains the following keys:

    grailsApplication
    beanWrapper
    json
    resourceName
    resourceId (optional)

where

* grailsApplication is a reference to the grailsApplication context
* beanWrapper is a BeanWrapper instance wrapping the object being marshalled
* json is the JSON converter that should be used to generate content
* resourceName is the resource name obtained as the pluralized, hyphenated version of the domain class
* resourceId is the id (if available) of the groovy bean

Note that if resourceName is specified in the additionalFieldsMap (see below), then that value is passed instead of the derived name.

Note that resourceId may not be present in the map. The marshaller attempts to provide a value for resourceId as follows:

* if the bean's metaClass has an id property, it's value is used
* if the bean's metaClass responds to a 'getId' method taking zero-arguments, the value the method returns is used
* if the bean has an 'id' property, the value of the property is used
* if the bean has a public, non-transient, non-static id field, the value of the id field is used

If none of the above conditions apply, then resourceId will not be passed in the map.

You can use additionalFields multiple times on any declarative marshaller configuration.
For example:

    resource 'customers' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonGroovyBeanMarshaller {
                    supports net.hedtech.restfulapi.Customer
                    additionalFields { Map m -> //some content}
                    additionalFields { Map m -> //some more content}
                }
            }
        }
    }

###Full list of configuration elements for a json groovy bean marshaller
The configuration block for the marshaller can contain the following in any order:

    inherits = <array of json groovy bean marshaller template names>
    supports <class>
    <field-block>*
    includesFields {
        <field-block>*
    }
    excludesFields {
        <fieldsBlock>*
    }
    additionalFields <closure>

Where \<field-block\>* is any number of field-blocks, and a field-block is

    field 'name' [name 'output-name']

Where values in brackets are optional.

Example:

        field 'foo' name 'bar'
        includesFields {
            field 'foo' name 'foobar'
        }

is equivalent to

        includesFields {
            field 'foo' name 'foobar'
        }

If an includedFields block is present, any excludedFields block is ignored:

        includesFields {
            field 'foo'
            field 'bar'
        }

        excludesFields {
            field 'baz'
        }

        includesFields {
            field 'baz'
        }

is the same as

        includesFields {
            field 'foo'
            field 'bar'
            field 'baz'
        }

###Groovy bean marshaller templates
JSON groovy bean marshaller templates are configuration blocks that do not directly create a marshaller.  The 'config' block accepts any configuration that the jsonGroovyBeanMarshaller block does (including the inherits option).  When a jsonGroovyBeanMarshaller directive contains an 'inherits' element, the templates referenced will be merged with the configuration for the marshaller in a depth-first manner.  Elements that represent collections or maps are merged together (later templates overriding previous ones, if there is a conflict), with the configuration in the jsonGroovyBeanMarshaller block itself overriding any previous values.

In general, templates are useful for defining affordances and other behavior that need to be applied across many representations.

###Template inheritance order.
When a json groovy bean marshaller declaration includes an inherits directive, then the configuration of each template is merged with the declaration itself in depth-first order.  For example, consider the use of nested configuration:

    jsonGroovyBeanMarshallerTemplates {
        template 'one' config {
            //some config
        }
        template 'two' config {
            //some config
        }
        template 'three' config {
            inherits = ['one','two']
            //some config
        }
        template 'four' config {
            //some config
        }
    }

    resource 'customers' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonGroovyBeanMarshaller {
                    inherits = ['three','four']
                    supports net.hedtech.restfulapi.Customer
                }
            }
        }
    }

The domain marshaller will be configured with the results of merging the configuration blocks in the following order: 'one', 'two', 'three', 'four' and the contents of the jsonGroovyBeanMarshaller block itself.

###Configuration merging
The order in which configurations are merged is significant.  When two configurations, first and second are merged, boolean values, or single valued options that are set in the second config override the first.  Collection or map values are combined.

##Declarative extraction of JSON content
If JSON content needs to be manipulated before being bound to domain objects, POGOs, etc, it is possible to do so declaratively.  Any incoming JSON content will be a single object containing other JSON objects or JSON arrays.  This ability is not intended to deal with type coercion or data binding, but to provide a simple way to rename keys, provide default values, or convert 'short object' representations back into plain IDs.  It functions as a counterpart to the declarative json marshalling support.

The best way to describe the use of declarative extraction is by examples.

###Renaming keys
Consider a JSON object for a purchase order:

    {
        "productId":"123",
        "quantity":50,
        "customer":{
            "name":"Smith"
        }
    }

Suppose that the domain objects for purchase order and customer expected the productId to be called productNumber and the name to be lastName.  We can configure an extractor that will handle the renaming automatically:

    resource 'purchase-orders' config {
        representation {
            mediaTypes = ["application/json"]
            jsonExtractor {
                property 'productId'     name 'productNumber'
                property 'customer.name' name 'lastName'
            }
        }
    }

This will automatically register an extractor for the representation that will product the map:

    ['productNumber':'123', 'quantity':50, 'customer':['lastName':'Smith'] ]

Note the use of dot-notation to identify the key to rename.

When the path of a key to rename traverserses one or more JSON arrays, the rename is applied to all elements of the array that are JSON objects:

    resource 'purchase-orders' config {
        representation {
            mediaTypes = ["application/json"]
            jsonExtractor {
                property 'customers.name' name 'lastName'
            }
        }
    }

This configuration will produce an extractor that when applied to

        {
            "customers": [
                {"name":"Smith"},
                {"name":"Jones"}
            ]
        }

Would result in the map

    [ "customers":[ ["lastName":"Smith"], ["lastName":"Jones" ] ] ]

When specifying paths, alway do so in terms of the incoming JSON.  The extractor will automatically rename keys correctly, regardless of what order they are specified in.  For example:

    resource 'purchase-orders' config {
        representation {
            mediaTypes = ["application/json"]
            jsonExtractor {
                property 'customers'      name 'preferredCustomers'
                property 'customers.name' name 'lastName'
            }
        }
    }

Will work correctly.

###Providing default values.
As your system evolves, you may introduce new, required fields to domain objects.  If you are using versioned APIs, the new field cannot be added to existing representation(s) without breaking them, so when a caller uses one of these representations, it will be necessary to add a default value.  This can, of course, be done at the service layer, but only if the service layer can provide an appropriate default - it is more likely that the service will treat the missing value as a validation exception.  The declarative extractor can be configured to provide a default value for any missing key.

    resource 'purchase-orders' config {
        representation {
            mediaTypes = ["application/json"]
            jsonExtractor {
                property 'orderType' defaultValue 'standard'
            }
        }
    }

This will create an extractor that will added the key/value pair 'orderType'/'standard' to the map, if 'orderType' does not already exist as a key.  If 'orderType' exists as a key (even with a null value), then no action is taken.

As with renaming keys, if any part of the property path traverses a JSON array, the defaultValue rule will apply to all members of the collection that are JSON objects.

###Treating a value as a short-object representation
The declarative domain class marshalling renders associations as 'short-objects' by default.  When consuming the same representation, it may be desirable to have the short object representations converted back to plain IDs, or some other format conducive to data binding.  This can be accomplished by declaring the field to be a short object representation.

        resource 'purchase-orders' config {
            representation {
                mediaTypes = ["application/json"]
                jsonExtractor {
                    property 'customer' shortObject true
                }
            }
        }

For input

    { "orderId":12345,
      "customer": { "_link": "/customers/123"}
    }

The above configuration would produce the map:

    ['orderID':12345, 'customer': [ 'id':'123' ] ]

If a property is declared as a short-object, by default the extractor will assume the short-object format is a json object containing a '_link' property, that has a url or url fragment as the value.  It takes anything after the last '/' as the id value, and converts the json object into a map of [id:\<idvalue\>].  Note that this is compatible with the default short-object behavior of the declarative json marshaller.

Note that if you intend to use grails data binding, then for single references, like in the above example, you will also need to 'flatten' the customer reference; see "Flattening the final map" below.

A field that represents an array of short-objects is converted to an array of IDs. For example:

        resource 'purchase-orders' config {
            representation {
                mediaTypes = ["application/json"]
                jsonExtractor {
                    property 'customers' shortObject true
                }
            }
        }

Given input

    { "orderId":12345,
      "customers": [ { "_link": "/customers/123" }, { "_link":"/customers/456" } ]
    }

Will convert to

    ['orderID':12345, 'customers': [ '123', '456' ] ]

The reason collections are treated differently than a single reference is due to how grails data-binding operates.

If you have customized what a short-object reference looks like, you can override the behavior by assigning a closure specifying how to convert short-objects.  For example, suppose that your short-object representations instead of containing a url, are a json object containing resource-name and id separately, like:

    { "orderId":12345,
      "customers": [ { "resource-name": "customers", "id":123 }, { "resource-name":'customers', "id":123 } ]
    }

    To convert that back into id:value maps, you could override the short-object behavior like so:

    resource 'purchase-orders' config {
        representation {
            mediaTypes = ["application/json"]
            jsonExtractor {
                property 'customers' shortObject true
            }
            shortObject { def value ->
                if (value == null) return value
                if (Collection.class.isAssignableFrom(value.getClass())) {
                    def result = []
                    value.each() {
                        if (it !=null && Map.class.isAssignableFrom(it.getClass())) {
                            result.add( it['id'] )
                        } else {
                            throw new Exception( "Cannot convert from short object for $it" )
                        }
                    }
                    return result.toArray() //grails data binding requires a java array
                } else {
                    if (Map.class.isAssignableFrom(value.getClass())) {
                        def v = value['id']
                        return [id:v]
                    } else {
                        throw new Exception( "Cannot convert from short object for $value" )
                    }
                }
            }
        }
    }

Which would result in the map:

    ['orderID':12345, 'customers': [ '123', '456' ] ]

Note that the closure must be able to handle both single short-object references, and collections of them.  In general, if you are overriding the short-object behavior, you would want to override it for all representations.  This is possible by using templates; see below for more details on how to do so.

###Flattening the final map
If you intend to use grails data binding to bind the output of a declarative extractor to grails domain objects or POGOs, then you may need to flatten parts of the map that represent sub-objects.  This is because the data binding is designed to work with parameters submitted from web forms, so when dealing with nested objects, it expects key names to describe the associations, rather than nested maps.  For example it expects

    ['orderID':12345, 'customer.name':'Smith']

instead of

    ['orderID':12345, 'customer': [ 'name':'Smith' ] ]

You can instruct the declarative extractor to 'flatten' a property whose value is a map.  This will cause the original property to be removed, and replaced with a new property for each key in the map.  The new property name will be formed by joining the original property name with the key in the map value, joined with a '.'.

For example, suppose you have a deep-rendered representation that looks like the following:

    {"orderId":123,
     "customer": {
        "name":"Smith"
        "id":456,
        "phone-number":"555-555-5555"
     }
    }

    resource 'purchase-orders' config {
        representation {
            mediaTypes = ["application/json"]
            jsonExtractor {
                property 'customer' flatObject true
            }
        }
    }

Will generate a final map that looks like:

    ['orderId':123, 'customer.name':'Smith', 'customer.id':456, 'customer.phone-number':'555-555-5555']

Which is suitable for grails data binding.

Note that definining a property as a flat object only applies directly to the map (or collection of maps) representing the sub-object..  That is, it does not automatically handle further nested sub-objects.  If the customer sub-object in the above example also contained an address sub-object that needed to be flattened, that would need to be specified separately:

    resource 'purchase-orders' config {
        representation {
            mediaTypes = ["application/json"]
            jsonExtractor {
                property 'customer'         flatObject true
                property 'customer.address' flatObject true
            }
        }
    }

If the value of the property declared as a flat-object is a collection, then the extractor will automatically index the new keys created, e.g.

        ['orderID':12345, 'customers[0].id':'123', 'customers[1].id':'123']

The extractor will index based solely on the order in which it encounters sub-ojects.  Make certain you understand grails data binding and how it works when using collections of sub-objects, particularly if using sets of objects with no ordering.  The declarative extractor can flatten json objects into formats suitable for grails data-binding, but has no knowledge of the objects (domain or otherwise) that the output will be bound to.  Any of the challenges inherent to grails data binding from data submitted via a form stil remain.

In general, for collections of sub-objects, it is recommened to not use grails data binding (that is, not flatten the sub-objects).  This is because grails data binding operates against collections based on the index of entries in the collection, not their IDs.

For example, suppose we have a one-to-many relations of Author to Book.  We have an existing Author, that has three books having IDs 1, 2, and 3 and titles 'Book1', 'Book2', and 'Book3'.  If we print the books for this author, it would look like:

    [[Book id=1, title=Book1], [Book id=2, title=Book2], [Book id=3, title=Book3] ]

Now suppose we receive a JSON representation where we are using full rendered version of the Books instead of short-objects, say something like:

    {"books":[ "id":2, "title":"ChangedTitle"]}

We flatten the map to look like

    def m = ["books[0].id":2, "books[0].title":"ChangedTitle"]

And use grails data binding against the Author:

    author.properties = m

If we save this and print the books collection for that author, we will get:

    [[Book id=2, title=ChangedTitle], [Book id=2, title=ChangedTitle], [Book id=3, title=Book3] ]

This is because when using the index and dot notation for grails data binding, grails is being told to change what the index zero reference in the books collection points to.

In other words, representations that deep-render associated objects, and use grails-data binding by flattening the objects, require the consumer of the API to worry about ordering within collections when performing an update.  For this reason, it is recommended that representations either use short-objects (i.e, representations only control the association of objects, but do not allow properties of sub-objects to be manipulated in the same API call), or the backing services should implement their own data binding strategies rather than rely on grails indexed data binding.  For example, the service could iterate through an array of associated objects on an update, retrieve them by ID instead of index position, and then bind the sub-map to them directly.

###JSON extractor templates
JSON extractor templates are configuration blocks that do not directly create an extractor.  The 'config' block accepts any configuration that the jsonExractor block does (including the inherits option).  When a jsonExractor directive contains an 'inherits' element, the templates referenced will be merged with the configuration for the extractor in a depth-first manner.  Elements that represent collections or maps are merged together (later templates overriding previous ones, if there is a conflict), with the configuration in the jsonDomainMarshaller block itself overriding any previous values.

In general, json domain templates are useful for overriding short-object handling, or defining rules that apply to properties present in all incoming JSON objects.

###Template inheritance order.
When a json extractor declaration includes an inherits directive, then the configuration of each template is merged with the declaration itself in depth-first order.  For example, consider the use of nested configuration:

    jsonExtractorTemplates {
        template 'one' config {
            //some config
        }
        template 'two' config {
            //some config
        }
        template 'three' config {
            inherits = ['one','two']
            //some config
        }
        template 'four' config {
            //some config
        }
    }

    resource 'customers' config {
        representation {
            mediaTypes = ["application/json"]
            jsonExractor {
                inherits = ['three','four']
            }
        }
    }

The extractor will be configured with the results of merging the configuration blocks in the following order: 'one', 'two', 'three', 'four' and the contents of the jsonExractor block itself.

###Configuration merging
The order in which configurations are merged is significant.  When two configurations, first and second are merged, boolean values, or single valued options that are set in the second config override the first.  Collection or map values are combined.


##Logging
Errors encountered while servicing a request are logged at error level to the log target 'RestfulApiController_messageLog'.  This is so errors occuring from the requests (which will typically be errors caused by invalid input, etc) can be separated from errors in the controller.

##<a id="api-testing"></a>Testing an API
The plugin contains the class net.hedtech.restfulapi.spock.RestSpecification which may be extended to write functional spock tests.  Spock is a testing and specification framework that is very expressive.  It is strongly recommended that you use spock to test your APIs.

The RestSpecification class contains a number of convenience methods for making calls to Restful APIs as part of the functional test phase.

For example

    class ThingAPISpec extends RestSpecification {

        static final String localBase = "http://127.0.0.1:8080/thing-api"

        def "Test list with json response"() {
            setup:
            createThing('AA')
            createThing('BB')

            when:"list with  application/json accept"
            get("$localBase/api/things") {
                headers['Content-Type'] = 'application/json'
                headers['Accept']       = 'application/json'
            }

            then:
            200 == response.status
            'application/json' == response.contentType
            def json = JSON.parse response.text
            2 == json.size()
            "AA" == json[0].code
            "An AA thing" == json[0].description

            // assert localization of the message
            "List of thing resources" == responseHeader('X-hedtech-message')

            //check pagination headers
            "2" == responseHeader('X-hedtech-totalCount')
            null != responseHeader('X-hedtech-pageOffset')
            null != responseHeader('X-hedtech-pageMaxSize')
            json[0]._href?.contains('things')
            null == json[0].numParts
            null == json[0].sha1
        }
    }

Clone the plugin's git repo and look at the tests under test-restful-api for more examples of using spock and the RestSpecification to do functional testing of your APIs.

##Cross-Origin Resource Sharing
User agents typically apply same-origin restrictions to network requests; this prevents a client-side Web application from invoking APIs provided by the plugin from a different origin.

Applications using the RESTFul API plugin can optionally use the [grails-cors](http://grails.org/plugin/cors) plugin to allow cross-origin requests (via XMLHttpRequest).

To use it, add the following plugin-dependency (use at least verion 1.1.0 of the plugin):

    runtime ":cors:1.1.0"


In order for a client to fully access the api, you will need to configure the cors plugin to issue CORS headers, including exposing the custom headers the plugin returns.  For example, if your application is exposing the APIs at /apis, and you want to expose all the custom headers the plugin can return, and you want to allow cross-origin requests from any origin, you would add the following to your Config.groovy:

    cors.url.pattern = '/api/*'
    cors.allow.origin.regex='.*'
    cors.expose.headers='content-type,X-hedtech-totalCount,X-hedtech-pageOffset,X-hedtech-pageMaxSize,X-hedtech-message,X-hedtech-Media-Type'

Note that at present, the cors plugin can produce responses to OPTIONS requests that are inconsistent with the restful plugin.  The cors plugin returns that all HTTP methods are supported by any url that matches cors.url.pattern; in reality, only some of the urls matched by the pattern will map to actual resources; and for those resources, the methods supported may be restricted.  In those cases, an OPTIONS request to a a given url may return that all methods are supported, while actually using one of those methods returns a 405.  We may make a future contribution to the cors plugin to eliminate this inconsistency.



