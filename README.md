<!-- ********************************************************************
     Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************** -->

#RESTful API plugin documentation

##Status
Early Access.  The plugin may be used gain familiarity and provide testing feedback, but should not be used with any shipping software yet.  The plugin has not been through export compliance.

##Overview
The restful-api plugin is an implementation designed to conform to the Ellucian API Strategy Document.  It is not intended to implement all optional features of the Document.  It is not intended to provide support for multiple ways to accomplish the same requirements; in general, it implements the recommended approaches specified in the document.

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
Irrespective of the method used to install the RESTful plugin, the following changes must be made to include the plugin dependencies.  The plugin depends on both the inflector plugin, and spock plugins.  (The spock dependency is for the RestSpecification testing class.)

In the dependencies section of BuildConfig.groovy add:

    test "org.spockframework:spock-grails-support:0.7-groovy-2.0"

In the plugins section of BuildConfig.groovy add:


        compile ":inflector:0.2"

        test(":spock:0.7") {
          exclude "spock-grails-support"
        }


##Details

###Use of conventions
The plugin relies heavily on convention-over-configuration.  A request to a resource named 'things' will be delegated to the bean registered as 'thingService'.  De-pluralization of the resource name happens automatically; it is also assumed that there is a unique bean registered as thingService that can be looked up in the grails application.

You can override this convention by specifying a different service name in the configuration.

###Url Mapping
To use the restful plugin, you need to configure your UrlMappings.groovy to route the requests to the controller.

**Important:parseRequest must be set to false; otherwise, grails will attempt to parse the request body and add values from it to the params object.  The restful plugin is designed to pass any content from the body to the service layer via an extracted map, not the params object.**

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
The create method is passed a Map extracted from the request.  (The map is generated by the Extractor registered for the resource and format.)
The create method is responsible for using the map to create a new instance of the resource.  For domain services, it is recommended that the extractor produce a map that will work with data binding, e.g., the create method is passed a Map params, and can create a new object as

`new Thing( params )`

The create method must return an object representing the resource that was created.  This object will be rendered as a resource representation via the configured marshallers for the resource.  For example, create may simply return the domain instance created.

###update method
The update method is passed the resource id and a Map extracted from the request.  (The map is generated by the Extractor registered for the resource and format.)

The controller will first check that if the extracted map contains an 'id' property, that it matches the resource id passed in the url.  (That is, the controller assumes that an 'id' property extracted from the resource reprentation is the resource id).  The controller will raise an exception if this assumption is violated.

The update method is responsible for using the map to update an instance of the resource.

For domain services, it is recommended that the extractor produce a map that will work with data binding, e.g., the object can be updated by

`thing.properties = params`

The update method must return an object representing the resource that was updated.  This object will be rendered as a resource representation via the configured marshallers for the resource.  For example, create may simply return the domain instance created.

###delete method
The delete method is passed the id of the resource to delete, and (optionally) a Map representing the content extracted from the request (if any).  For example, a resource representation may be included as part of a delete request to convey optimistic locking information.

The controller will first check that if the extracted map contains an 'id' property, that it matches the resource id passed in the url.  (That is, the controller assumes that an 'id' property extracted from the resource reprentation is the resource id).  The controller will raise an exception if this assumption is violated.

The delete method returns void.

##Adapting an Existing Service
To support services that have a contract different than the service contract described above, a 'RestfulServiceAdapter' may be registered in the Spring application context. If an adapter is registered, it will be used by the RestfulApiController for all interaction with servcies. (Currently, only a single adapter may be registered, so it must be implemented to support all services that are used to support a RESTful API.)

As a convenience, a 'RestfulServiceBaseAdapter' is included within this plugin to faciliate delegation to Banner XE services that extend the 'ServiceBase' found within the 'banner-core' plugin. (Note while this adapter is specific to Banner XE, this plugin has no dependencies on either Banner XE or the banner-core plugin.)

##<a id="filter-list"></a>Filtering
As a convenience, the plugin provides a Filter class that includes a factory method (extractFilters) that may be used to create a list of Filter instances from the query parameters.  The URL query parameters that may be used with this class must be in the format:
```
part-of-things?filter[0][field]=description&filter[1][value]=6&filter[0][operator]=contains&filter[1][field]=thing&filter[1][operator]=eq&filter[0][value]=AZ&max=50
```
The filters may be used to filter on a resource's property whether a primitive property or one representing an associated resource.  Currently only single resource assocations are supported (i.e., not collections).  The operators include 'eq' (or 'equals') and 'contains' (which performs a case insensative 'ilike'-like comparison, although not actually using 'ilike' as that is not supported by Oracle).

The plugin also includes an HQLBuilder utility class that will construct an HQL statement from the request params object. This will leverage the Filter class to extract filters and will then construct the HQL statement.  Following is an example usage:
```
def queryStatement = HQLBuilder.createHQL( grailsApplication, params )
def result = PartOfThing.executeQuery( queryStatement, [], params )
```
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

Configuration is currently performed by assigning a closure to the restfulApiConfig property in the grails configuration.

For example, in Config.groovy:

    restfulApiConfig = {
        resource {
            name = 'things'
            representation {
                mediaType = "application/json"
                addMarshaller {
                    marshaller = new net.hedtech.restfulapi.marshallers.BasicDomainClassMarshaller(grailsApplication)
                    priority = 100
                }
                extractor = new net.hedtech.restfulapi.extractors.json.DefaultJSONExtractor()
            }
            representation {
                mediaType = "application/xml"
                jsonAsXml = true
                addMarshaller {
                    marshaller = new net.hedtech.restfulapi.marshallers.xml.JSONObjectMarshaller()
                    priority = 200
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
        resource {
            name = 'colleges'
            representation {
                mediaType = "application/vnd.hedtech.v0+json"
                addMarshaller {
                    marshaller = new net.hedtech.restfulapi.marshallers.json.v0.CollegeMarshaller(grailsApplication)
                    priority = 100
                }
                extractor = new net.hedtech.restfulapi.extractors.json.v0.CollegeExtractor()
            }
            representation {
                mediaType = "application/vnd.hedtech.v1+json"
                addMarshaller {
                    marshaller = new net.hedtech.restfulapi.marshallers.json.v1.CollegeMarshaller(grailsApplication)
                    priority = 100
                }
                extractor = new net.hedtech.restfulapi.extractors.json.v1.CollegeExtractor()
            }
        }
        resource {
            name = 'students'
            representation {
                mediaType = "application/vnd.hedtech.v0+json"
                addMarshaller {
                    marshaller = new net.hedtech.restfulapi.marshallers.json.v0.StudentMarshaller(grailsApplication)
                    priority = 100
                }
                extractor = new net.hedtech.restfulapi.extractors.json.v0.StudentExtractor()
            }
        }
    }

This configuration exposes 2 resources, 'colleges' and 'students'.  The 'colleges' resource support two versioned representations, while the 'students' resource only has one.  Note that the custom media types are re-used across the resources.

####Default representations
You can also assign multiple media types to the same representation.  The most common use for this is to have a media type such as 'application/json' represent a particular version as a default.  For example:

    restfulApiConfig = {
        resource {
            name = 'colleges'
            representation {
                mediaType = "application/vnd.hedtech.v0+json"
                addMarshaller {
                    marshaller = new net.hedtech.restfulapi.marshallers.json.v0.CollegeMarshaller(grailsApplication)
                    priority = 100
                }
                extractor = new net.hedtech.restfulapi.extractors.json.v0.CollegeExtractor()
            }
            representation {
                mediaType = "application/vnd.hedtech.v1+json"
                mediaType = "application/json"
                addMarshaller {
                    marshaller = new net.hedtech.restfulapi.marshallers.json.v1.CollegeMarshaller(grailsApplication)
                    priority = 100
                }
                extractor = new net.hedtech.restfulapi.extractors.json.v1.CollegeExtractor()
            }
        }
    }

Both 'application/vnd.hedtech.v1+json' and 'application/json' identify the same representation.  As new versions are added, the 'application/json' can be moved to them, so that a client that always wants the latest/default representation can obtain it with 'application/json'.

####Marshaller groups
It is likely that you will have marshallers that you will want to re-use across multiple resources/representations; for example, a common marshaller for objects like dates, addresses, etc.  You can configure a collection of marshallers as a marshaller group:

    restfulApiConfig = {
        marshallerGroup {
            name = 'defaultJSON'
            addMarshaller {
                marshaller = new net.hedtech.restulfapi.marshallers.json.DateMarshaller()
                priority = 100
            }
            addMarshaller {
                marshaller = new net.hedtech.restulfapi.marshallers.json.AddressMarshaller(grailsApplication)
                priority = 100
            }
        }
        resource {
            name = 'students'
            representation {
                mediaType = 'application/json'
                addMarshaller getMarshallerGroup('defaultJSON')
                addMarshaller {
                    marshaller = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                    priority = 100
                }
            }
        }
    }

This configuration defines a reusable group of marshallers named 'defaultJSON' containing marshallers for dates and addresses.  The 'students' resource representation is using those marshallers.  The above configuration is equivalent to:

    restfulApiConfig = {
        resource {
            name = 'students'
            representation {
                mediaType = 'application/json'
                addMarshaller {
                    marshaller = new net.hedtech.restulfapi.marshallers.json.DateMarshaller()
                    priority = 100
                }
                addMarshaller {
                    marshaller = new net.hedtech.restulfapi.marshallers.json.AddressMarshaller(grailsApplication)
                    priority = 100
                }
                addMarshaller {
                    marshaller = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                    priority = 100
                }
            }
        }
    }

####Ordering
Configuration elements are applied in the order in which they are encountered.  This means that ordering in the configuration is signficant.  For example:

    restfulApiConfig = {
        resource {
            name = 'students'
            representation {
                mediaType = 'application/json'
                addMarshaller getMarshallerGroup('student')
            }
        }
        marshallerGroup {
            name = 'student'
            addMarshaller {
                marshaller = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                priority = 100
            }
        }
    }

Will result in an exception, as the first call to `getMarshallerGroup('student')` occurs before the marshallerGroup is defined.

####Overriding the service used for a resource.
By default, the name of the service to use is derived from the name of the resource.  You can override the name of the service bean to use for the resource with the 'serviceName' property when configuring a resource.  For example:

    restfulApiConfig = {
        resource {
            name = 'students'
            serviceName = 'studentFacadeService'
            representation {
                mediaType = 'application/json'
                addMarshaller getMarshallerGroup('student')
            }
        }
        marshallerGroup {
            name = 'student'
            addMarshaller {
                marshaller = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                priority = 100
            }
        }
    }

Will cause the list, show, create, update, and delete methods for the resource 'students' to be delegated to the bean registered under the name 'studentFacadeService' instead of 'studentService'.

####Limiting methods for a resource
By default, any configured resource supports 'list', 'show', 'create', 'update', and 'delete' methods.  You can customize which methods are exposed by specifying a subset of the 5 methods as an array assigned to the methods property of a resource.

For example, to expose students as a read-only resource that just supports list and show:

    restfulApiConfig = {
        resource {
            name = 'students'
            methods = ['list','show']
            representation {
                mediaType = 'application/json'
                addMarshaller getMarshallerGroup('student')
            }
        }
        marshallerGroup {
            name = 'student'
            addMarshaller {
                marshaller = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                priority = 100
            }
        }
    }

If a request for an unsupported method is received, the plugin will respond with a 405 status code and an Allow header specifying the HTTP methods the url supports.

Note that if you are using the grails-cors plugin to support CORS functionality, the OPTIONS method will return results that are inconsistent with the methods actually supported.  That is, an OPTIONS request to an api url will always return that all methods are supported, even if the configuration restricts the methods for a given resource.  This may be addressed in a contribution to a future release of the CORS plugin.


##JSON-as-xml
The plugin supports a 'cheap' method of supporting both json and xml representation, while only requiring JSON marshallers/extractors to be created.
To enable it for a representation, set jsonAsXml = true and specify the default jsonAsXml marshaller and extractor:

    representation {
        mediaType = "application/xml"
        jsonAsXml = true
        addMarshaller {
            marshaller = new net.hedtech.restfulapi.marshallers.xml.JSONObjectMarshaller()
            priority = 200
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

##Logging
Errors encountered while servicing a request are logged at error level to the log target 'RestfulApiController_messageLog'.  This is so errors occuring from the requests (which will typically be errors caused by invalid input, etc) can be separated from errors in the controller.

##Testing
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













