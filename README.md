<!-- ***************************************************************************
 * Copyright 2013 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************************************************************** -->

#RESTful API plugin documentation

##Status
Production quality, although subsequent changes may not be backward compatible.

##Overview

An introduction to the plugin is provided within the accompanying "[Introducing the RESTful API Grails Plugin](http://restfulapi.github.io/intro-restful-api-slides/)" presentation. _(Please use Chrome, Firefox or Safari to view this presentation.)_

Key features of this plugin include:

* A DSL-based configuration that is used to expose resources
* Use of custom media types to identify representations (and versioning)
* Declarative marshaling and extraction of versioned representations (JSON and XML)
 * Include, omit, and rename properties
 * Group and reuse marshalers
 * Add affordances (support HATEOAS)
 * Ability to configure custom marshallers and extractors when declarative configuration is not sufficient (e.g., PDF, iCalendar, binary formats)
* Route API requests to a single controller that delegates to transactional services based on naming convention or configuration
* An ability to configure service adapters to accomodate services exposing a different contract
* Consistent use of HTTP status codes and headers, including caching headers and CORS
* Ability to 'query by POST' (to allow query criteria to be provided within the request body)
* Support for developing functional tests using Spock
* Extensive regression tests to provide confidence when adopting this plugin

The restful-api plugin is designed to facilitate exposing RESTful API endpoints that conform to our [API Strategy](https://github.com/restfulapi/api-strategy/blob/master/README.md).  _Please note, however, it is not intended to implement all optional features discussed within the strategy  nor is it intended to provide support for multiple ways to accomplish the same requirements; in general, it implements the recommended approaches specified in the strategy._

##Installation and quickstart

###1. Install the plugin

This plugin should be installed from the official Grails Central Plugin Repository ([http://grails.org/plugins/restful-api](http://grails.org/plugins/restful-api)) by setting the following dependency:

```
    compile ":restful-api:1.0.1"
```

_Note: It may sometimes be useful to install this plugin as a Git submodule instead (e.g., if you are actively contributing to the plugin). To add the plugin as a Git submodule under a 'plugins' directory:_

        your_app (master)$ git submodule add https://github.com/restfulapi/restful-api.git plugins/restful-api.git
        Cloning into 'plugins/restful-api.git'...
        remote: Counting objects: 1585, done.
        remote: Compressing objects: 100% (925/925), done.
        remote: Total 1585 (delta 545), reused 309 (delta 72)
        Receiving objects: 100% (1585/1585), 294.45 KiB | 215 KiB/s, done.
        Resolving deltas: 100% (545/545), done.

_Then add the in-place plugin definition to BuildConfig.groovy:_

        grails.plugin.location.'restful-api' = "plugins/restful-api.git"

_Adding the plugin this way will use the latest commit on the master branch at the time you ran the submodule command.  If you want to use an official release instead, go to the plugin directory and checkout a specific version, e.g.:_

    cd plugins/restful-api.git
    git checkout 1.0.1

_Lastly, don't forget to go back to your project root and commit the change this will make to your git submodules file._


###2. Configure plugin dependencies
Irrespective of the method used to install the RESTful plugin, the following changes must be made to include the plugin dependencies.  The plugin depends on inflector, cache-headers, and spock plugins.  (The spock dependency is for the RestSpecification testing class, that you may use to [test your API](#api-testing).

For Grails 2.2.x:

In the dependencies section of BuildConfig.groovy add:

    test "org.spockframework:spock-grails-support:0.7-groovy-2.0"

In the plugins section of BuildConfig.groovy add:

        compile ":inflector:0.2"
        compile ":cache-headers:1.1.5"

        test(":spock:0.7") {
          exclude "spock-grails-support"
        }

        test ":funky-spock:0.2.1"

For Grails 2.3.x, you do not need the spock dependencies:

In the plugins section of BuildConfig.groovy add:

        compile ":inflector:0.2"
        compile ":cache-headers:1.1.7"

        test ":funky-spock:0.2.1"

###3. Configure the UrlMappings to use the controller
Edit the UrlMappings.groovy to look similar to the following defaults.  Your application map already have url mappings defined; if so, add the mappings for /api and /qapi as appropriate.

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

        // We can also expose 'query-with-POST' URLs by using a different prefix:
        //
        "/qapi/$pluralizedResourceName"(controller:'restfulApi') {
            action = [GET: "list", POST: "list"]
            parseRequest = false
        }

        "/"(view:"/index")
        "500"(view:'/error')
    }

###4. Configure default settings for the controller
    Add the following to the Config.groovy:

    // ******************************************************************************
    //                              CORS Configuration
    // ******************************************************************************
    // Note: If changing custom header names, remember to reflect them here.
    //
    cors.url.pattern        = '/api/*'
    cors.allow.origin.regex ='.*'
    cors.expose.headers     ='content-type,X-hedtech-totalCount,X-hedtech-pageOffset,X-hedtech-pageMaxSize,X-hedtech-message,X-hedtech-Media-Type,X-Request-ID'


    // ******************************************************************************
    //             RESTful API Custom Response Header Name Configuration
    // ******************************************************************************
    // Uncomment and change to override.
    //
    //restfulApi.header.totalCount  = 'X-hedtech-totalCount'
    //restfulApi.header.pageOffset  = 'X-hedtech-pageOffset'
    //restfulApi.header.pageMaxSize = 'X-hedtech-pageMaxSize'
    //restfulApi.header.message     = 'X-hedtech-message'
    //restfulApi.header.mediaType   = 'X-hedtech-Media-Type'

    //restfulApi.header.requestId   = 'X-Request-ID'

    // ******************************************************************************
    //             RESTful API 'Paging' Query Parameter Name Configuration
    // ******************************************************************************
    // Uncomment and change to override.
    //
    //restfulApi.page.max    = 'max'
    //restfulApi.page.offset = 'offset'

    // ******************************************************************************
    //             RESTful API deprecated response headers
    // ******************************************************************************
    // Uncomment and change to override.
    //
    // In the deprecatedHeaderMap:
    //  - key is the current header
    //  - value is a previous header that is now deprecated
    //  - value may also be a list of deprecated headers if there is more than one
    //
    //restfulApi.deprecatedHeaderMap = [
    //        'X-hedtech-Media-Type': 'X-Media-Type-old',
    //        'X-hedtech-totalCount': ['X-Total-Count-old1', 'X-Total-Count-old2']
    //]

    // ******************************************************************************
    //                       RESTful API Endpoint Configuration
    // ******************************************************************************
    //
    restfulApiConfig = {
      //handle any pluralized resource name by mapping it to the singularized service name,
      //e.g. persons is handled by personService.
      //Dynamic marshallers/extractors are used.
      //If you want to whitelist only, remove the anyResource block and replace with
      //definitions for specific resources.
      anyResource {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    priority = 101
                }
                jsonBeanMarshaller {
                    priority = 100
                }
            }
            jsonExtractor {}
        }
      }
    }

###5. Configure logging
Add

    fatal  'RestfulApiController_messageLog'

to the log4j configuration (in Config.groovy).  By default, errors that originate with the services the restful-api controller calls are logged to the RestfulApiController_messageLog.  You can control whether such errors (which may be normal errors resulting from invalid input, etc) show up in the logs by adjusting the settings for RestfulApiController_messageLog.

At this point, the plugin is configured to dynamically attempt to handle any request sent to /api or /qapi, assuming that your services match the [contract](#service-layer-contract) or you have an appropriate [adapter](#service-layer-adapter) configured.

The rest of this document goes into details of how to configure the plugin to support features such as [whitelisting](#configuration) resources, configuring [declarative marshalling](#declarative-marshalling) to support versioned APIs, and how to use the RestSpecification to provide functional testing for your APIs.

##Testing the Plugin
The plugin contains a test application that uses Spock to test the plugin. To run all tests from the plugin's root directory run:
```bash
grails clean && (cd test/test-restful-api && grails clean && grails test-app)
```

##Details

###Use of conventions
The plugin relies heavily on convention-over-configuration.  A request to a resource named 'things' will be delegated to the bean registered as 'thingService'.  De-pluralization of the resource name happens automatically; it is also assumed that there is a unique bean registered as thingService that can be looked up in the grails application.

You can override this convention by specifying a different service name in the configuration.

###Url Mapping
To use the restful plugin, you need to configure your UrlMappings.groovy to route the requests to the controller.  The URLs are expected to following normal REST conventions.

| HTTP Method | Purpose |
|:---------:|:----------|
| GET | Read a resource or list of resources |
| POST | Create a new resource (when the key is not known a-priori) \* See note. |
| PUT | Update an existing resource or create one if the key is pre-defined |
| DELETE | Remove a resource |

\* _Note: In addition, a URL mapping using a different prefix (e.g., 'qapi') can be used to allow the use of a POST for querying a resource. This is helpful when query criteria cannot easily be represented as query parameters or when query criteria includes personally identifiable information (PII) that should not be allowed to be written into server access logs._

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

        // We can also expose 'query-with-POST' URLs by using a different prefix:
        //
        "/qapi/$pluralizedResourceName"(controller:'restfulApi') {
            action = [GET: "list", POST: "list"]
            parseRequest = false
        }

        "/"(view:"/index")
        "500"(view:'/error')
    }

Note that in order to conform to the strategy and for support for limiting methods exposed by a resource to operate correctly, all the api urls must conform to the pattern above.  That is, 'show', 'update', and 'delete' methods are mapped to urls that have an even number of parts after the api prefix; 'list' and 'create' methods map to urls that have an odd number of parts after the api prefix.

###Use of custom media types
The plugin relies on custom media types to specify resource representations.  For requests, the media type in the Content-Type header is used to identify the resource representation in the request body, in order to extract parameters from it to be passed to the resource's service.

For media types representing json or xml content, it is strongly recommended that they end with either 'json' or 'xml' (e.g. 'application/json' or 'application/vnd.hedtech.v0+xml').  Media types ending with 'json' or 'xml' will automatically have a Content-Type on responses of 'application/json' or 'application/xml'

###Media type of responses
As a convenience to viewing responses in browsers or other tools without having to configure them for all custom media types used by an API, the controller defaults to setting the Content-Type header of responses to 'application/json', 'application/xml', or 'text/plain'.  You can think of it as the Content-Type of the response identifies the format of the response body, but not the specific type of the representation contained within.  The controller will choose the content type based on the media type of the response representation: 'application/json' for media types ending in 'json', 'application/xml' for types ending in 'xml', and 'text/plain' for anything else.  You can explicity set what the content type header in a response containing a specific representation with the contentType option (see Configuring Resources).

Successful responses generated by the plugin will always include a 'X-hedtech-Media-Type' header specifying the (versioned) media-type of the representation, e.g., 'application/vnd.hedtech.v0+json' or 'application/vnd.hedtech.minimal-thing.v0+xml'.  Consumers of the API can use the X-hedtech-Media-Type header to determine how to perform data binding on the resource representation returned.

###Media type of error responses
If a request results in an error response, the Media Type will always be 'application/json' or 'application/xml', based on whether the Accept header in the request specified an xml or json format.  If the Accept header could not be understood to request json or xml, then 'application/json' will be used for any data in the return body.

Errors are not considered part of the resource representation, and do not have versioned or custom representations.

###Media type of requests
The plugin uses the Content-Type header to determine the resource representation sent in a request body.  It uses the Accept header to determine the type of resource representation that should be used in the response.  The Content-Type and Accept headers on a request are not required to match.

If your request body contains content in a particular charset, specify that in the Content-Type header.  For example, if sending json using the UTF-8 charset, you may use a Content-Type header of "application/json; charset=UTF-8".  If the charset is not specified, the default character set as determined by grails will apply.

###Content negotiation on requests
The plugin performs content negotiation for a request by parsing the Accept Header, taking any q-values into account.  The plugin will then use the best known representation for the accepted type(s).

For exmaple, for a request with an Accept Header of "application/xml;q=0.9,application/vnd.hedtech.v0+xml;q=1.0", the plugin would attempt to return the representation for 'application/vnd.hedtech.v0+xml', if one is configured.  If not, it would attempt to return the representation for 'application/xml'.

The controller also handles '*/*' values in the Accept Header.  See [Media Type Range](#media-type-range) for details.

Note that at present, the selection of representation is done based only on what representations are configured.  If an error occurs when marshalling a response to that representation, the plugin does not fall back to the next best representation as specified by the Accept Header.  This may change in future releases.

#####JSON Array CSRF Protection
JSON representations intended for internal use (e.g., using AJAX) may be configured with a 'jsonArrayPrefix' that will be added to the front of JSON Array content in the response.

This prefix may be used to protect against [CSRF](https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)) attacks that are possible when using old browsers which allow redefining of the JavaScript Array constructor. Please see [http://haacked.com/archive/2008/11/20/anatomy-of-a-subtle-json-vulnerability.aspx/](http://haacked.com/archive/2008/11/20/anatomy-of-a-subtle-json-vulnerability.aspx/).

Common prefixes include 'while(1);' and 'for(;;);'.  A configured prefix is only used when returning a JSON Array and is ignored in all other situations.  Following is an example configuration used to protect an 'internal' representation.  The client will need to strip off the prefix before parsing the JSON.

```
    representation {
        mediaTypes = ["application/vnd.hedtech.internal.v0+json"]
        jsonArrayPrefix = 'while(1);'
        marshallerFramework = 'json'
        marshallers {
            jsonDomainMarshaller {
                inherits = ['jsonDomainAffordance']
            }
        }
        jsonExtractor {}
    }
```

Note that if jsonArrayPrefix is set, it only applies to representations being marshalled with the 'json' framework.  Marshalling via the 'xml' framework, or custom marshalling services is not affected.  If you are marshalling to json via a custom service (by setting the value of marshallerFramework to the name of a bean), you must honor the value of the jsonArrayPrefix in your service yourself (the call to the service will provide the representation configuration.)

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

| Header | Purpose |
|:------:|:--------|
| X-hedtech-totalCount | Returned with list responses to indicate the total count of resources. |
| X-hedtech-pageOffset |  Returned with list responses to identify the current page. |
| X-hedtech-pageMaxSize | Returned with list responses to indicate the max page size. |
| X-hedtech-Media-Type | Returned with all (success) responses, and contains the exact type of the response. |
| X-hedtech-message | May optionally be returned with any response.  Contains a localized message for the response. |
| X-Status-Reason | Optionally returned with a 400 response to provide additional information on why the request could not be understood |

_NOTE: The names used for these custom response headers must be configured within Config.groovy, as discussed in the '[configuration](#configuration)' section below._

##Cache Headers
The plugin supports client caching (unless disabled within Config.groovy).  When caching support is enabled, the plugin will include both 'ETag' and 'Last-Modified' HTTP Headers within responses to GET requests and will support conditional GET requests containing either an 'If-None-Match' or 'If-Modified-Since' header.  This support is provided by the [cache-headers](https://github.com/Grailsrocks/grails-cache-headers) plugin.

Specifically, a GET request (for either a resource or collection of resources) will include an ETag header (whose value is a SHA1 specifically calculated for the resource representation) and a 'Last-Modified' header (whose value is based upon a 'lastUpdated' or 'lastModified' property when available).

A subsequent conditional GET request containing an 'If-None-Match' header (whose value is an ETag value) will result in a '304 Not Modified' if processing the request results in a newly calculated ETag that is unchanged.  (Note this does not necessarily reduce server processing, but it does preclude sending unchanged content over the network.)  Similarly, when a GET request includes an 'If-Modified-Since' header, a 304 will be returned if the requested resource has not changed.  When the GET is for a collection, the latest 'lastUpdated' (or 'lastModified') date is used.

Note that at this time, conditional PUT requests are not supported.  Although a conditional PUT request is not supported, optimistic lock violations will be reported (so the end result is that a client will receive a 409 'conflict' versus a 412 'precondition failure' when using a conditional PUT request).

##<a id="service-layer-contract"></a>Service layer contract
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

It is more efficient if the list method can also return the total count.  If the list method returns an instance of grails.orm.PagedResultList, then the controller will obtain the the totalCount from the PagedResultList, and will not invoke the count method.

If you are not returning an instance of grails.orm.PagedResultList, but your implementation can provide the total count from the list method, you can return an instance of net.hedtech.restfulapi.PagedResultList.  In this case, the controller will obtain the total count from the PagedResultList, and will not invoke the count method.  The plugin contains a default implementation based on an ArrayList, net.hedtech.restfulapi.PagedResultArrayList.

###show method
The show method will be passed the request parameters object directly.
It must return an object.  This object will be rendered as a resource representation via the configured marshallers for the resource.  For example, show may simply return a domain instance.

###create method
The create method is passed a content Map extracted from the request as well as the Grails params Map.  (The 'content' map is generated by the Extractor registered for the resource and format.)
The create method is responsible for using the map to create a new instance of the resource.  For domain services, it is recommended that the extractor produce a map that will work with data binding, e.g., the create method is passed a content Map containing the extracted content as well as the Grails params Map. It can create a new object as:

`new Thing( content )`

The create method must return an object representing the resource that was created.  This object will be rendered as a resource representation via the configured marshallers for the resource.

###update method
The update method is passed the a content Map extracted from the request, and the Grails params Map.  (The 'content' map is generated by the Extractor registered for the resource and format.)

The update method is responsible for using the map to update an instance of the resource.  The id of the resource will be contained in the params Map, per the UrlMappings.

For domain services, it is recommended that the extractor produce a map that will work with data binding, e.g., the object can be updated by:

`thing.properties = content`

The update method must return an object representing the resource that was updated.  This object will be rendered as a resource representation via the configured marshallers for the resource.

Before invoking the service method, the controller will first check that if the extracted map contains an 'id' property, that when converted to a string, it matches the resource id passed in the url.  (That is, the controller assumes that an 'id' property extracted from the resource reprentation is the resource id).  The controller will raise an exception if this assumption is violated.

If you do not want the controller to enforce this, you can override it on a per-resource basis by setting the idMatchEnforced on the resource to false, e.g.

```groovy
    restfulApiConfig = {
        resource 'things' config {
            idMatchEnforced = false
        }
    }
```

The update method will then be responsible for ensuring that any id value in the content matches the id extracted from the url.

###delete method
The delete method is passed an optional content Map representing the content extracted from the request (if any), and the Grails params map.  For example, a resource representation may be included as part of a delete request to convey optimistic locking information if deemed appropriate.  The id of the resource will be contained in the params Map, per the UrlMappings.

The delete method returns void.

By default, the delete method will ignore both the body of the request, and the Content-Type header, and send an empty content map to the service.  This makes it easier to work with UI frameworks that may not set the Content-Type or Content-Length headers on delete requests when sending empty bodies.

If you want the controller to check the Content-Type header and extract a content map, you can override the default behavior by setting bodyExtractedOnDelete to true in the resource configuration.  For example:

```groovy
    restfulApiConfig = {
        resource 'things' config {
            bodyExtractedOnDelete = true
        }
    }
```

Before invoking the service method, the controller will first check that if the extracted map contains an 'id' property, that when converted to a string, it matches the resource id passed in the url.  (That is, the controller assumes that an 'id' property extracted from the resource reprentation is the resource id).  The controller will raise an exception if this assumption is violated.

If you do not want the controller to enforce this, you can override it on a per-resource basis by setting the idMatchEnforced on the resource to false, e.g.

```groovy
    restfulApiConfig = {
        resource 'things' config {
            idMatchEnforced = false
        }
    }
```

The delete method will then be responsible for ensuring that any id value in the content matches the id extracted from the url.

##<a id="service-layer-adapter"></a>Adapting an Existing Service
To support services that have a contract different than the service contract described above, one or more 'RestfulServiceAdapter' implementations may be registered in the Spring application context. (Please see the [src/groovy/net/hedtech/restfulapi/RestfulServiceAdapter](https://raw.github.com/restfulapi/restful-api/master/src/groovy/net/hedtech/restfulapi/RestfulServiceAdapter.groovy) interface.)

A single shared adapter may be configured separately using a bean name of 'restfulServiceAdapter' as in the following example:


    beans = {
        restfulServiceAdapter( my.RestfulApiServiceAdapterImpl )
    }


If you need to use different adapters for different services, you can register multiple adapters and assign them to specific resources.  For example:


    beans = {
        fooAdapter( my.FooAdapter )
        barAdapter( my.BarAdapter )
    }


In the DSL you could then define


    restfulApiConfig = {
        resource 'foos' config {
            serviceAdapterName = 'fooAdapter'
            representation {
                mediaTypes = ['application/json']
                marshallers {
                    marshallerGroup 'student'
                }
            }
        }
        resource 'bars' config {
            serviceAdapterName = 'barAdapter'
            representation {
                mediaTypes = ['application/json']
                marshallers {
                    marshallerGroup 'student'
                }
            }
        }
    }

See [specifying a service adapter](#specifying-service-adapter).


The controller chooses an adapter to use as follows:

* If serviceAdapterName is defined on the resource, then a bean with that name is used.  If the bean is missing, it is treated as a a misconfigured resource, and a 404 is returned.
* If serviceAdapterName is not defined, and a bean with the name of restfulServiceAdapter exists, then it is used.
* Otherwise, a built-in pass-through adapter is used.

Configuring a service adapter within Spring is only required when delegating to services whose contract differs from that expected by this plugin.

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

The HQLBuilder performs some basic parsing of dates if the type of a filter is set to 'date'.  If the value can be parsed as a Long, then it is treated as the value for a date, e.g.

```
new Date(Long.valueOf(filterValue))
```

Otherwise, the HQLBuilder will attempt to parse it as a subset of ISO 8601.  For example "2013-10-29T15:35:00Z"

##Query-with-POST
If a /qapi url mapping has been defined to support querying with POST, the controller will attempt to parse the body of a POST to a resource using the qapi prefix by using the pseudo-resource name 'query-filters'.  In order for this to work, add a 'query-filters' resource to the restfulApiConfig:

    // This pseudo resource is used when issuing a query using a POST. Such a POST is made
    // against the actual resource being queried, but using a different URL prefix (e.g., qapi)
    // so the request is routed to the 'list' method (versus the normal 'create' method).
    resource 'query-filters' config {
        representation {
            mediaTypes = ["application/json"]
            jsonExtractor {}
        }
        representation {
            mediaTypes = ["application/xml"]
            xmlExtractor {}
        }
    }

Remove the json or xml representation as necessary.

To pass query filters by POST, use a map where the keys are the names of the filter parameters.  For example, in json:

    {
        "filter[0][field]": "code",
        "filter[0][operator]": "eq",
        "filter[0][value]": "BB",
        "max":"50"
    }

Or in xml, using the declarative extractor as defined above:

    <filters map="true">
        <entry key="filter[0][field]">code</entry>
        <entry key="filter[0][operator]">eq</entry>
        <entry key="filter[0][value]">BB</entry>
        <entry key="max">1</entry>
    </filters>

You can, of course, assign custom extractors to the query-filters representation.  Any such extractor must return a map where the keys are valid parameter names for the filtering support.

##Supplemental data/affordances/HATEOS
Since the plugin does not use an envelope in the message body, any affordances must be present in the resource representations themselves.

The exact method of support is still being researched.  Current thinking is that supplemental data will be provided to the marshallers via meta-programming on the instance returned from a service method.  For example, consider a 'Thing' service, which needs to provide supplemental data on an object by computing a sha1 hash of several fields:

`MessageDigest digest = MessageDigest.getInstance("SHA1")
digest.update("code:${thing.getCode()}".getBytes("UTF-8"))
digest.update("description${thing.getDescription()}".getBytes("UTF-8"))
def properties = [sha1:new BigInteger(1,digest.digest()).toString(16).padLeft(40,'0')]
thing.metaClass.getSupplementalRestProperties << {-> properties }`

Note that the getSupplementalRestProperties method is being added only to the single object instance, not the entire class.  A marshaller can check to see whether the instance it is marshalling supports the method, and if so, extract data from it to generate affordances.

##Exception handling
When an exception is encountered while servicing a request, the controller will delegate to an instance of net.hedtech.restfulapi.ExceptionHandler that is responsible for generating an error response.

The handler selection process is similar to how a Grails converter selects a marshaller for an object instance.  ExceptionHandler instances may be registered with the controller, along with a priority.  Each ExceptionHandler has a supports method that returns true or false depending on whether the handler can produce a response for a given Throwable instance.

When the controller encounters an exception, it consults its registered handlers, in priority order.  The first handler that supports the instance of the exception is used to produce the error response.

The controller automatically registers handlers for the following types of exceptions:

* any exception that can be identified as an [ApplicationException](#application-exception) via duck typing.
* instances of net.hedtech.restfulapi.IdMismatchException and its subclasses
* instances of org.springframework.dao.OptimisticLockingFailureException and its subclasses
* instances of net.hedtech.restfulapi.UnsupportedMethodException and its subclasses
* instances of net.hedtech.restfulapi.UnsupportedRequestRepresentationException and its subclasses
* instances of net.hedtech.restfulapi.UnsupportedResourceException and its subclasses
* instances of net.hedtech.restfulapi.UnsupportedResponseRepresentationException and its subclasses
* instances of grails.validation.ValidationException and its subclasses

Finally, the controller registers a default exception handler that takes care of any exception not handled by any other handler, and that returns a 500 status code.

The controller uses negative priorities for all of the handlers it registers, so any application registered handlers at the default level (0) or higher are consulted first.


###Customizing Exception Handling
You can customize exception handling in two ways.  You can create (or use metaprogramming on existing classes) exceptions that meet the duck typing requirements for an [ApplicationException](#application-exception), or you can register custom handlers.  In general, registering custom handlers is a cleaner approach, as it isolates the details of how to map application and framework exceptions to RESTful responses in your api configuration.

To create an exception handler, implement the net.hedtech.restfulapi.ExceptionHandler interface.  For example:
```groovy
package my.app.exceptionhandlers

import net.hedtech.restfulapi.ErrorResponse
import net.hedtech.restfulapi.ExceptionHandler
import net.hedtech.restfulapi.ExceptionHandlerContext
import net.hedtech.restfulapi.Inflector

class OptimisticLockExceptionHandler implements ExceptionHandler {

    boolean supports(Throwable e) {
        (e instanceof my.app.OptimisticLockException)
    }

    ErrorResponse handle(Throwable e, ExceptionHandlerContext context) {
        new ErrorResponse(
            httpStatusCode: 409,
            message: context.localizer.message(
                code: "default.optimistic.locking.failure",
                args: [ Inflector.singularize( context.pluralizedResourceName ) ] ),
            content: ['originatingErrorMessage':e.getMessage()]
        )
    }
}
```

The supports method returns true if the controller should use this handler to generate the response.  (Remember, the controller will consult its handlers in priority order, using the first one that supports the exception.)

The handle method is passed the Throwable instance, and a context instance.  The context contains two fields:

* pluralizedResourceName - the name of the resource that encountered the exception
* localizer - a net.hedtech.restful.api.Localizer instance that can be used to lookup messages

The method must return a net.hedtech.restfulapi.ErrorResponse instance containing the following:

* httpStatusCode: The http status code to return in the response.  This is the only value that is required.
* message: An optional message to include in the message header
* headers: Optional map of headers to include with the response
* content: An optional content map to render in the response body

Exception handlers can be registered in the configuration:

```groovy
restApiConfig = {
    exceptionHandlers {
        handler {
            instance = new my.app.exceptionhandlers.OptimisticLockExceptionHandler()
            priority = 5
        }
        handler {
            instance = new my.app.exceptionhandler.SomeExceptionHandler()
            priority = 6
        }
    }
}
```

Note that the implementation of custom handlers should conform to the Ellucian REST strategy.  For example, a handler that supports validation exceptions should return a 400 status code, and should also return an 'X-Status-Reason:Validation failed' header.

You cannot remove the exception handlers that the controller automatically registers, but you can override them by registering handlers for the same conditions.

If multiple handlers are registered with the same priority, they are consulted in reverse order in which they were registered; that is, the last one registered is consulted first.

This can be useful if you wish to override an existing handler, without registering a handler at a higher priority.

For example, the default exception handler is registered with a priority of Integer.MIN_VALUE.  To override its behavior and add additional header fields:

```groovy
package my.app.exceptionhandlers

import net.hedtech.restfulapi.ErrorResponse
import net.hedtech.restfulapi.ExceptionHandler
import net.hedtech.restfulapi.ExceptionHandlerContext

/**
 * Default handler that treats any exception as a 500 response.
 **/
class DefaultExceptionHandler implements ExceptionHandler {

    boolean supports(Throwable t) {
        true
    }

    ErrorResponse handle(Throwable e, ExceptionHandlerContext context) {
        new ErrorResponse(
            httpStatusCode: 500,
            message: context.localizer.message(
                code: "default.rest.general.errors.message",
                args: [ context.pluralizedResourceName ]),
            headers:['Custom-app-header':'some value'],
            content: [
                errors: [
                    [
                        type: "general",
                        errorMessage: e.message
                    ]
                ]
            ]
        )
    }
}
```

```
restApiConfig = {
    exceptionHandlers {
        handler {
            instance = new my.app.exceptionhandlers.DefaultExceptionHandler()
            priority = Integer.MIN_VALUE
        }
    }
}
```

Even though the controller automatically registers a default handler at the same priority, the custom one will be consulted first, because all handlers registered via configuration are added after the controller's.


###<a id="application-exception"></a>ApplicationException
The controller automatically registers an exception handler for 'application exceptions'.  This allows applications using the plugin to customize how exceptions generate error responses directly in the exception itself.  In general, it recommended to implement and register custom exception handlers instead, to eliminate direct coupling between application exception hierarchies and the api layer.

An ApplicationException is not determined by inheritance; instead duck typing is used.  The controller registers an exception handler that supports any exception that responds to 'getHttpStatusCode' (it has a method getHttStatusCode()) and has a property named 'returnMap' that is an instance of Closure.  The handler extracts data from it as follow:

* getHttpStatusCode() will be invoked to obtain the http status code that should be returned
* the returnMap closure will be invoked, passing in a localizer so that localized messages can be contructed.  The closure must return a Map; entries in the map will be used as follows:
    * if the map contains a 'headers' key, the value is expected to be a map of header names/header values to return in the error response
    * if the map contains a 'message' key, the value is expected to be a (localized) string to be returned in the message header.
    * if the map contains an 'errors' key, the value is expected to be a map that is to be rendered as JSON or xml in the response body

This definition of ApplicationException allows any application to customize error handling without extending or overriding any controller methods.  However, the implementation of any application exceptions must take responsibility for conforming to the Ellucian REST strategy.  For example, if an application exception represents a validation exception, it needs to return a 400 status code, and should also return an 'X-Status-Reason:Validation failed' header.

##Configuring resources
The overall processing of a request proceeds as follows:

* The plugin is responsible for content negotiation, parsing the Accept and Content-Type headers
* For request with bodies to process, the controller media type from the Content-Type header to identify an extractor for the resource.  If no extractor is configured for that resource representation, the representation is treated as unsupported (415 status)
* The controller parses the request body as either JSON or xml, and passes the JSONObject or GPathResult to the appropriate extractor.
* The controller passes the Map returned from the extractor to the appropriate service method.
* The controller takes the map returned by the service method, and selects a resource representation based on the best match for the Accept header  to marshall the response into JSON or xml.  If none of the media types in the Accept header identify a supported representation of the resource, a 406 status will be returned.
* The controller renders the response along with appropriate associated headers and status code.
* If at any point, an exception is thrown, it is rendered according to the rules in Exception handling (see above).

##<a id="configuration"></a>Configuration
The restful-api plugin configuration allows configuration of custom HTTP header names, 'paging' query parameter names, and resource representation marshallers and extractors. The custom header names and paging query parameter names need not be configured unless you want to override the defaults.

Following is an example configuration of HTTP custom header names and paging query parameter names:

```groovy
restfulApi.header.totalCount  = 'X-example-totalCount'
restfulApi.header.pageOffset  = 'X-example-pageOffset'
restfulApi.header.pageMaxSize = 'X-example-pageMaxSize'
restfulApi.header.message     = 'X-example-message'
restfulApi.header.mediaType   = 'X-example-Media-Type'
```
```groovy
restfulApi.page.max    = 'pageSize'
restfulApi.page.offset = 'page'
```

The restful-api plugin supports copying of response headers to one or more deprecated headers. This feature is to support existing REST contracts even after your organiztaion may change the names of response headers. It is accomplished through the creation of a deprecatedHeaderMap, where the map key is the current header, and the value is a previous header that is now deprecated. The value may also be a list of deprecated headers if there is more than one.

```groovy
restfulApi.deprecatedHeaderMap = [
    'X-example-Media-Type': 'X-Media-Type-old',
    'X-example-totalCount': ['X-Total-Count-old1', 'X-Total-Count-old2']
]
```

The restful-api plugin supports use of the 'X-Request-ID' Header, which is an emerging best practice as it helps correlate log files. Ideally, this header will be set by middleware (e.g., a router), but if it is not a UUID will be generated and used to populate this Header in the response. This value is also captured as a request attribute for use during the request handling (e.g., in your service). While 'X-Request-ID' appears to be an emerging 'standard', the header used for this purpose is configurable.

```
// while configurable, 'X-Request-ID' seems to be the emerging standard
restfulApi.header.requestId = 'X-MyRequest-ID'
```

Most of the restful-api configuration pertains to configuring the support for resource representation.  This configuration is performed by assigning a closure to the restfulApiConfig property in the grails configuration.

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
        }
    }

This declares that a resources 'things' is to be exposed via the plugin.  Resource 'things' has two available representations, one for json, and one for xml.  In the case of the json representation, a named configuration for the JSON converter will be created (based on the resource name and media type), and the BasicDomainClassMarshaller will be registed under that configuration, and that JSON converter configuration will be used to marshall any thing representation for 'application/json'.  Any requests for things with a Content-Type of 'application/json' will use the DefaultJSONExtractor to convert the resource representation to a map to be passed to the backing service.

Note that each resource representation gets its own isolated set of marshallers, allowing for complete control of how any objects are marshalled for that representation.  In particular, this allows for versioned representations.

The restful-api plugin leverages the grails converter mechanism to marshall responses, and extractor classes to convert resource representations in requests to Map instances that are passed to the services.

As shown, 'grailsApplication' can be used within the restfulApiConfig closure (and references the grailsApplication instance as expected).

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

###<a id="whitelisting"></a>Whitelisting vs dynamic exposure of resources
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
                    jsonBeanMarshaller {
                        priority = 100
                    }
                }
                jsonExtractor {}
            }
        }
    }

The configuration in the anyResource block applies to any resource that isn't explicitly listed in the configuration.  anyResource would be primarily used when you want to dynamically expose resources, and are not using versioned representations; e.g., you have a 'simple' system in which the resource representations can map directly to the objects they represent and the json or xml can be dynamically generated.  The above example uses the declarative domain and bean marshallers to handle all resources.

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
                    jsonBeanMarshaller {
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

###Default representations
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

###<a id="media-type-range"></a>Media Type Range
By default, an Accept-Header value of '\*/\*' is mapped to the first media type in the first representation for a resource.  For example:

    restApiConfig = {
        resource 'things' config {
            representation {
                mediaTypes = ["application/vnd.net.hedteach.v0+json", "application/json"]
                marshallerFramework = 'json'
                marshallers {
                    jsonDomainMarshaller {
                    }
                }
                jsonExtractor {}
            }
            representation {
                mediaTypes = ["application/xml"]
                marshallers {
                    xmlDomainMarshaller {
                    }
                }
                xmlExtractor {}
            }
        }
    }

A request for resource 'things' with an Accept Header of '\*/\*' with the above configuration will cause the controller to choose the  media-type of 'application/vnd.net.hedteach.v0+json' as the representation to return, and that is the type that will be returned in the X-hedtech-Media-Type header.

You can explicitly define what media type will be used for a '\*/\*' in the Accept-Header with the anyMediaType attribute on a resource:

    restApiConfig = {
        resource 'things' config {
            anyMediaType = 'application/xml'
            representation {
                mediaTypes = ["application/vnd.net.hedteach.v0+json", "application/json"]
                marshallerFramework = 'json'
                marshallers {
                    jsonDomainMarshaller {
                    }
                }
                jsonExtractor {}
            }
            representation {
                mediaTypes = ["application/xml"]
                marshallers {
                    xmlDomainMarshaller {
                    }
                }
                xmlExtractor {}
            }
        }
    }

With the above configuration, a request for resource 'things' with an Accept Header of '*/*' will return the "application/xml" representation.

Support for '*/*' in the Accept Header is generally of use for APIs having representations in PDF or text/calendar, where you wish to return content to requests from browsers.  The support for '*/*' allows the Accept-Header sent by most browsers to be mapped to supported media type.

Note that if you specify a value for anyMediaType that is not assigned to a representation for that resource, the controller will throw a validation exception during initialization.

###Overriding Content-Type on responses
Sucessful responses will have a Content-Type header of 'appliction/json', 'application/xml', or 'text/plain', depending on whether the media type for the representation used ends in 'json', 'xml', or neither.  You can explicitly control the Content-Type header by specifying contentType on the representation:

    restfulApiConfig = {
        resource 'colleges' config {
            representation {
                mediaTypes = ["application/vnd.hedtech.v0"]
                contentType = 'application/json'
                marshallers {
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.json.v0.CollegeMarshaller(grailsApplication)
                        priority = 100
                    }
                }
                extractor = new net.hedtech.restfulapi.extractors.json.v0.CollegeExtractor()
            }
        }
    }

In this case, we are using a custom media type that does not end in json or xml, but is producing json content.  Specifying contentType will ensure that the Content-Type header is set to 'application/json' when returning this representation in a response.

Note that this feature is intended for use mainly with formats other than xml or json (e.g., text/calendar).  For json or xml representations, it is strongly recommended that you use a media type ending with 'json' or 'xml' so the controller can automatically select the appropriate Content-Type header.


###Marshaller groups
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

###Default marshaller groups
If you have marshallers that you want to be used automatically for all representations (e.g., a format for dates), you can define them in a marshaller group named 'json' or 'xml'.  Any marshallers defined in the 'json' group will automatically be added to the beginning of the marshaller list for all representations using the 'json' marshalling framework.  Similarly, all marshallers defined in an 'xml' group will be added to all representations using the 'xml' marshalling framework.  For example:

    restfulApiConfig = {
        marshallerGroups {
            group 'json' marshallers {
            marshaller {
                instance = new org.codehaus.groovy.grails.web.converters.marshaller.ClosureOjectMarshaller<grails.converters.JSON>(
                        java.util.Date, {return it?.format("yyyy-MM-dd'T'HH:mm:ssZ")})
            }
        }

        resource 'students' config {
            representation {
                mediaTypes = ['application/json']
                marshallers {
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                        priority = 100
                    }
                }
            }
        }
    }

Will cause as Date fields in a student to be rendered using the "yyyy-MM-dd'T'HH:mm:ssZ".  The same will apply to every json representation defined for every resource.  The above configuration is functionally equivalent to:

    restfulApiConfig = {
        resource 'students' config {
            representation {
                mediaTypes = ['application/json']
                marshallers {
                    marshaller {
                        instance = new org.codehaus.groovy.grails.web.converters.marshaller.ClosureOjectMarshaller<grails.converters.JSON>(
                                java.util.Date, {return it?.format("yyyy-MM-dd'T'HH:mm:ssZ")})
                    }
                    marshaller {
                        instance = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                        priority = 100
                    }
                }
            }
        }
    }

The default marshaller groups also apply to the anyResource block.

###Ordering
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

###Overriding the service used for a resource.
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

###<a id="specifying-service-adapter"></a>Specifying a service adapter.
You can specify, on a per-resource basis a service adapter to use.  First, ensure you have a valid [adapter](#service-layer-adapter) registered as a spring bean, then specify the name of the bean in the configuration.  For example:

    restfulApiConfig = {
        marshallerGroup {
            name = 'student'
            marshaller {
                instance = new net.hedtech.restfulapi.marshallers.json.StudentMarshaller(grailsApplication)
                priority = 100
            }
        }
        resource 'students' config {
            serviceAdapterName = 'studentServiceAdapter'
            representation {
                mediaTypes = ['application/json']
                marshallers {
                    marshallerGroup 'student'
                }
            }
        }
    }

At runtime, the controller will use the bean registered under the name 'studentServiceAdapter' to adapt requests for the studentService.


###Limiting methods for a resource
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

##Marshalling and extracting objects
In order to service a request, the controller needs to be able to process a resource representation contained in the body of request, and marshal an object (or list of objects) returned by a service into the appropriate resource representation.

The plugin achieves this by leveraging marshalling frameworks, and extractors.  By default, it uses the grails JSON and XML converters, but this can be overridden to use other frameworks (JAXB, XMLBeans, google-gson, etc).

The plugin will automatically use the grails json converters for any media type ending in 'json', and the grails xml converters for any media type ending in 'xml'.  To explicitly set the marshalling framework for a representation, see [Configuring the marshalling framework](#configure-marshalling-framework).

###Using grails converters
To fully take advantage of marshalling, you should understand how the grails converters for JSON and xml work.  The plugin takes advantage of named converter configurations.  Each resource/representation in the configuration using grails converters for marshalling results in a new named converter configuration for JSON or XML.  When marshalling an object, the restful-api controller will use that named configuration to marshall the object.  It does this by asking each marshaller in the named configuration (marshallers with higher priority first) if the marshaller supports the object.  This means that the marshallers registered for a resource/representation are used only for that representation, and is what allows the plugin to support different representations for the same resource.

However, you should be aware that these named configurations will fallback to the default converter configurations provided by grails (e.g., marshallers for Collections) if they cannot locate a marshaller within the named config.

##<a id="declarative-marshalling"></a>Declarative Marshalling of Domain classes to JSON
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

By default, if an included field does not exist in the object being marshalled, it will be ignored.  If you want an included field that is not present in the object being marshalled to result in an exception, then you can specify

    requiresIncludedFields true

For example

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    requiresIncludedFields true
                    includesFields {
                        field 'code' name 'productCode'
                        field 'description'
                    }
                }
            }
        }
    }

Now if the Thing class does not have a persistent field 'description', the marshaller will throw an exception.

#Marshalling only non-null fields.
There may be times when a representation should only include a field if it has a non-null value.

For a representation, you can specify that any null fields should not be marshalled with the marshallsNullFields setting:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    marshallsNullFields false
                    includesFields {
                        field 'code' name 'productCode'
                        field 'description'
                    }
                }
            }
        }
    }

In this example, if code or description are null, they will be omitted when the object is marshalled.  The marshallsNullFields setting controls the default behavior for all fields (marshallsNullFields is true by default).

Whether or not to marshall a null field can also be specified on a per-field basis.  For example:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    field 'description' marshallsNull false
                }
            }
        }
    }

All fields of Thing will be marshalled, but if description is null, it will be omitted.

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

###Deep marshalling associations
The declarative marshallers can be instructed to deep marshall associations; that is, instead of marshalling an association as a short-object reference, the associated object or collection is passed along to another marshaller that supports its class.  Deep marshalling can be set as the default on a declarative marshaller with the deepMarshallsAssociations setting:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    deepMarshallsAssociations true
                }
            }
        }
    }

With that setting, any fields representing an association will be deep marshalled by passing the value of the field into the marshalling framework.

The deepMarshallsAssociations can also be used in declarative marshaller templates.

Deep marshalling for association fields can also be specified at the field level, overriding the value of deepMarshallsAssociations.  For example:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    deepMarshallsAssociations true
                    includesFields {
                        field 'parts' deep false
                        field 'subPart'
                    }
                }
            }
        }
    }

Assuming both parts and subPart represent associated objects, the parts field will be marshalled as a short-object representation, while the subPart field will be deep marshalled (because the default behavior for the marshaller is deep-marshalling, per the deepMarshallsAssociations setting).

The 'deep' setting can be used anywhere a field can be defined; however, it has no effect on fields that do not represent associations on a domain object.

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
    deepMarshallsAssociations <true|false>
    marshallsNullFields <true|false>
    requiresIncludedFields <true|false>
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

    field 'name' [name 'output-name'] [resource 'resource-name'] [deep <true|false>] [marshallsNull <true|false>]

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

##Declarative Marshalling of Beans to JSON
The plugin contains a BeanMarshaller and a DeclarativeBeanMarshaller, designed to simplify marshalling of beans to a json representation.  The functioning of the marshallers is similar to the domain class marshallers, but operate against bean instances, intead of domain objects.  (Of course, where a domain object can be treated as a bean, the bean marshallers can also be used.)  The options these marshallers support are very similar to those of the domain marshallers, except that the bean marshallers do not have support for recognizing object associations.

The BeanMarshaller will marshall properties, and public non-static/non-transient fields of a bean.  The properties 'class', 'metaClass', and 'pasword' are automatically excluded from being marshalled.

Use of the BeanMarshaller requires new subclasses to be created to customize marshalling behavior.  Marshalling of beans can be customized without resorting to writing custom marshallers with the DeclarativeBeanMarshaller.

The DeclarativeBeanMarshaller is a marshaller that can be used to customize json representations without code.

By default, the DeclarativeBeanMarshaller behaves the same as the BeanMarshaller; however, it can be configured to include or exclude fields, add custom affordances or other fields, and rename fields.

To use the marshaller directly, see the javadocs for the class.

The preferred way to utilize the class is to use the built-in support for the class in the configuration DSL.

Anywhere you can add a marshaller (in a marshaller group or a representation), you can configure and add a json declarative bean marshaller with

    jsonBeanMarshaller {}

The closure specifies how to configure the marshaller.  Specifying no information will register a marshaller that behaves identically to BeanMarshaller; that is, it will marshall all but the default excluded fields.

The best way to describe the use of the marshaller is by examples.

###Limiting the marshaller to a class (and it's subclasses)
By default, a declarative bean marshaller will support any object that is an instance of GroovyObject.  If you are including or excluding fields however, it is likely that you want an instance of the marshaller for a particular class (or subclasses).  You can control this with the supports option:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonBeanMarshaller {
                    supports net.hedtech.restfulapi.Thing
                }
            }
        }
    }


This will register a declarative bean marshaller that will support the Thing class, and any subclasses of Thing.  Note that it is your responsibility to ensure that Thing can be treated as a bean - if it is not, you should register a different type of marshaller for it.

###Excluding specific fields from a representation
By default, the json bean marshaller marshals all properties, and any public non-static, non-transient field.  You can exclude additional fields or properties with the excludedFields block:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonBeanMarshaller {
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
                jsonBeanMarshaller {
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
                jsonBeanMarshaller {
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
                jsonBeanMarshaller {
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

By default, if an included field does not exist in the object being marshalled, it will be ignored.  If you want an included field that is not present in the object being marshalled to result in an exception, then you can specify

    requiresIncludedFields true

For example

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonBeanMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    requiresIncludedFields true
                    includesFields {
                        field 'code' name 'productCode'
                        field 'description'
                    }
                }
            }
        }
    }

Now if the Thing class does not have a field or property 'description', the marshaller will throw an exception.

#Marshalling only non-null fields.
There may be times when a representation should only include a field if it has a non-null value.

For a representation, you can specify that any null fields should not be marshalled with the marshallsNullFields setting:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonBeanMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    marshallsNullFields false
                    includesFields {
                        field 'code' name 'productCode'
                        field 'description'
                    }
                }
            }
        }
    }

In this example, if code or description are null, they will be omitted when the object is marshalled.  The marshallsNullFields setting controls the default behavior for all fields (marshallsNullFields is true by default).

Whether or not to marshall a null field can also be specified on a per-field basis.  For example:

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonBeanMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    field 'description' marshallsNull false
                }
            }
        }
    }

All fields of Thing will be marshalled, but if description is null, it will be omitted.

###Adding additional fields
You can add additional fields not directly present in a bean to its marshalled representation.

The declarative bean marshaller allows any number of closures to be added to marshall additional content.  For example, let's say we want to add affordances to all of our json representations.  We will define a marshaller template containing the closure for the affordance, then add it to the marshallers:

    jsonBeanMarshallerTemplates {
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
                jsonBeanMarshaller {
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
* resourceId is the id (if available) of the bean

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
                jsonBeanMarshaller {
                    supports net.hedtech.restfulapi.Customer
                    additionalFields { Map m -> //some content}
                    additionalFields { Map m -> //some more content}
                }
            }
        }
    }

###Full list of configuration elements for a json bean marshaller
The configuration block for the marshaller can contain the following in any order:

    inherits = <array of json bean marshaller template names>
    supports <class>
    marshallsNullFields <true|false>
    <field-block>*
    includesFields {
        <field-block>*
    }
    excludesFields {
        <fieldsBlock>*
    }
    additionalFields <closure>

Where \<field-block\>* is any number of field-blocks, and a field-block is

    field 'name' [name 'output-name'] [marshallsNull <true|false>]

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

Note that any time a field is defined, any previous settings for the same field name are reset.  For example:

    field 'foo' name 'bar' deep true
    includesFields {
        field 'foo'
    }

Is equivalent to

    includesFields {
        field 'foo'
    }

The last definition of a field in any context is what is used.

###Bean marshaller templates
JSON bean marshaller templates are configuration blocks that do not directly create a marshaller.  The 'config' block accepts any configuration that the jsonBeanMarshaller block does (including the inherits option).  When a jsonBeanMarshaller directive contains an 'inherits' element, the templates referenced will be merged with the configuration for the marshaller in a depth-first manner.  Elements that represent collections or maps are merged together (later templates overriding previous ones, if there is a conflict), with the configuration in the jsonBeanMarshaller block itself overriding any previous values.

In general, templates are useful for defining affordances and other behavior that need to be applied across many representations.

###Template inheritance order.
When a json bean marshaller declaration includes an inherits directive, then the configuration of each template is merged with the declaration itself in depth-first order.  For example, consider the use of nested configuration:

    jsonBeanMarshallerTemplates {
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
                jsonBeanMarshaller {
                    inherits = ['three','four']
                    supports net.hedtech.restfulapi.Customer
                }
            }
        }
    }

The domain marshaller will be configured with the results of merging the configuration blocks in the following order: 'one', 'two', 'three', 'four' and the contents of the jsonBeanMarshaller block itself.

###Configuration merging
The order in which configurations are merged is significant.  When two configurations, first and second are merged, boolean values, or single valued options that are set in the second config override the first.  Collection or map values are combined.

##Marshalling enumerations
Marshalling an enum value with the default grails behavior will result in a json object containing an enumType and name field.

For example, a simple bean with a name field and a field that holds and enum value would yield:

```json
{
    "enumValue":
    {
        "enumType":"net.hedtech.restfulapi.beans.SimpleEnum",
        "name":"VALUE2"
    },
    "name":"foo"
}
```

It is likely that when marshalling an enum, that the value of the enum as a string is desired, instead of a json object.  The net.hedtech.restfulapi.marshallers.json.EnumMarshaller can be used to obtain this behavior.

For example,
```groovy
    marshallerGroups {
        //marshallers included in all json representations
        group 'json' marshallers {
            marshaller {
                instance = new net.hedtech.restfulapi.marshallers.json.EnumMarshaller()
            }
        }
    }
```

For the same bean as above, this would yield:

```json
{
    "enumValue":"VALUE2",
    "name":"foo"
}
```



##Extracting content
Create, update and (optionally) delete operations will have a request body which ultimately needs to be parsed and acted upon by the service layer.  The plugin provides a number of ways to interact with this data.

For any create, update, or delete operation, the controller will identify an _extractor_ instance.  An extractor is responsible for taking the incoming content and converting it to a map that will ultimately be passed on to the service.  An extractor must implement exactly one of 3 interfaces:

    net.hedtech.restfulapi.extractors.JSONExtractor
    net.hedtech.restfulapi.extractors.XMLExtractor
    net.hedtech.restfulapi.extractors.RequestExtractor

Note that an extractor should implement only one of these interfaces.  The interface implemented defines what the controller needs to pass to the extractor.

For extractors that implement JSONExtractor, the controller will parse a org.codehaus.groovy.grails.web.json.JSONObject from the incoming request and pass it to the extractors extract method.  For XMLExtractor, it will pass a groovy.util.slurpersupport.GPathResult.

In cases where you want to bypass grails converters entirely and use a different framework, such as JAXB, google-gson, etc, an extractor implementing RequestExtractor can be used.  A RequestExtractor is passed the HttpServletRequest directly.  For example, such an extractor could place the request body into the map to be passed on to the service where a non-grails framework could be used to bind the body to business objects.

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

###Parsing dates.
You can specify paths that should be parsed into java.util.Date instances:

    resource 'purchase-orders' config {
        representation {
            mediaTypes = ["application/json"]
            jsonExtractor {
                property 'signupDate' date true
            }
        }
    }

This can simplifying conversion of strings to Dates without dealing with Grails data-binding and custom property editors.

By default, the extractor will use a default java.text.SimpleDateFormat to parse date fields, with lenient parsing set to false.  You can specify which formats to use:

    resource 'purchase-orders' config {
        representation {
            mediaTypes = ["application/json"]
            jsonExtractor {
                property 'signupDate' date true
                dateFormats = ["yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyyy.MMMMM.dd GGG hh:mm aaa"]
            }
        }
    }

Each date format will be tried in order, until one sucessfully parses the date string.  If no format is capable of parsing the string, a 400 response status will be returned.

You can configure date formats in a single location by placing them on an [extractor template](#json-extractor-templates) and then inheriting them.

If you want to allow the SimpleDateFormatter to use lenient parsing, where heuristics are used to allow a date such as 1999-99-99 to be sucessfully parsed, you can set lenientDates = true.  For example:

    resource 'purchase-orders' config {
        representation {
            mediaTypes = ["application/json"]
            jsonExtractor {
                property 'signupDate' date true
                dateFormats = ["yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyyy.MMMMM.dd GGG hh:mm aaa"]
                lenientDates = true
            }
        }
    }

Note that both the dateFormats and lenientDates settings apply the same to all properties identified as dates in a jsonExtractor.

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
                    //two possibilities.  Either this map represents an object,
                    //or it is a map of objects.  If the map has a id property, assume it's
                    //a single object
                    if (value.containsKey('id')) {
                        def v = value['id']
                        return [id:v.substring(v.lastIndexOf('/')+1)]
                    } else {
                        //assume map of short-objects
                        def result = [:]
                        value.entrySet().each { Map.Entry entry ->
                            def v = entry.value['id']
                            result.put(entry.key, [id:v.substring(v.lastIndexOf('/')+1)] )
                        }
                        return result
                    }
                }
            }
        }
    }

Which would result in the map:

    ['orderID':12345, 'customers': [ '123', '456' ] ]

Note that the closure must be able to handle single short-object references, collections of them, or maps of them.  In general, if you are overriding the short-object behavior, you would want to override it for all representations.  This is possible by using templates; see below for more details on how to do so.

###Flattening the final map
If you intend to use the orginal grails data binding (that used the Spring data binder) prior to Grails 2.3 to bind the output of a declarative extractor to grails domain objects or POGOs, then you may need to flatten parts of the map that represent sub-objects.  This is because the data binding is designed to work with parameters submitted from web forms, so when dealing with nested objects, it expects key names to describe the associations, rather than nested maps.  For example it expects

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

Which is suitable for grails data binding prior to Grails 2.3, or if using legacy data binding in Grails 2.3 or later.  If using the new grails data binding introduced in 2.3, you should not need to use flatObject.

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

###<a id="json-extractor-templates"></a>JSON extractor templates
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

##Declarative Marshalling of Domain classes to XML
The plugin supports marshalling of domain classes to XML with the net.hedtech.restfulapi.marshallers.xml.BasicDomainClassMarshaller and net.hedtech.restfulapi.marshallers.xml.DeclarativeDomainClassMarshaller.

The BasicDomainClassMarshaller exposes methods which can be overridden to define what fields to include, etc.  It functions similarly to the json marshalling, but produces xml output instead.  The DeclarativeDomainClassMarshaller supports the configuration DSL.

Both marshallers generate xml in a format similar to JSON semantics.  For lack of a better term, we will refere to it in this document as json-xml.  The format follows these rules

* An element with one or more children represents a map.  The name of each child element is a key in a map, and the value of the element is the value.
* The value of an element with no children is the textual value of the element.
* The value of an element with the attribute null set to "true" is null.
* The value of an element with the attribute array set to "true" is an array.  The values of the array are the values of each child element.
* The value of an element with the attribute map set to "true" is a map.  The children of such an element must each have a 'key' attribute set to the value of the key.  For each child, an entry is placed in the map using the value of the 'key' attribute as the key, and the value of the element as the value.

For example, suppose we have a Person class:

    class Person {
        String firstName
        String lastName
    }

An instance of Person

    new Person(firstName:'John', lastName:'Smith')

would have an xml representation under the above rules:

    <person>
        <firstName>John</firstName>
        <lastName>Smith</lastName>
    </person>

Notice the similarity to JSON:

    {"firstName":"John", "lastName":"Smith"}

In the case of the xml format, the \<person\> element is a wrapper designating the beginning of an object/map (and the name is actuall not important).  The child elements represent name/value pairs in the map.

A more complex example:

    class Person {
        String firstName
        String lastName
        Collection addresses = []
        Map employeeNotes = [:] //map of employee full name to an array of string
    }

An instance of Person having two addresses, and multiple notes for two employees would have a structure like this:

    <person>
        <firstName>John</firstName>
        <lastName>Smith</lastName>
        <addresses array="true">
            <address>
                <line1>1 First Street</line1>
                <city>somewhere</city>
                <state>PA</state>
            </address>
            <address>
                <line1>2 Second Street</line1>
                <city>nowhere</city>
                <state>PA</state>
            </address>
        </addresses>
        <employeeNotes map="true">
            <entry key="Hank Adams">
                <notes array="true">
                    <string>This is a note</string>
                    <string>This is another note</string>
                </notes>
            </entry>
            <entry key="Jane Smith">
                <notes array="true">
                    <string>note1</string>
                    <string>note2</string>
                </notes>
            <entry>
        </employeeNotes>
    </person>

###Configuring declarative XML marshalling
Anywhere you can define a marshaller, you can define a declarative xml marshaller with

    xmlDomainMarshaller {
    }

With options specified in the closure.  The xml domain marshaller supports a superset of the same options as the JSON version.  In the interest of brevity, options that are the same will not be repeated here.  You can configure templates for xml domain marshallers in a

    xmlDomainMarshallerTemplates {
    }

block, just as you can for JSON domain marshallers.  The only differences are in the maps passed to short object and additional fields closures - see the sections on additional fields and customzing short object behavior for xml for more details.

###Customizing element name for XML marshalling
By default, the declarative marshaller for xml will use the classname of the instance being rendered to choose a name for the element.  For example, a Person class will get rendered as a

    <person>

element.  (The classname is treated as a property name.)  You can override this behavior by providing an elementName explicitly:

    xmlDomainMarshaller {
        supports Person
        elementName 'APerson'
    }

A Person instance would have an xml representation

    <APerson>

using that configuration.

###Customizing short-object behavior for XML marshalling
You can override how a declarative xml marshaller renders short-objects by specifying a closure that generates the content.  The marshaller will automatically pass the closure a Map containing the following keys:

        grailsApplication
        property
        refObject
        xml
        resourceId
        resourceName

where:

* grailsApplication is a reference to the grailsApplication context
* property is a GrailsDomainClassProperty instance for the field being marshalled
* refObject is the associated object to generate a short-object representation for
* xml is the XML converter that should be used to generate content
* resourceId is the id of the refObject
* resourceName is the resource name that the refObject is exposed as by the API.  If a field declaration for the field being rendered contains a 'resource' option, that value is passed in resourceName.  Otherwise, the pluralized, hyphenated domain name will be passed.

If the configuration specifies a resource name for the field being marshalled as a short object, that will be passed as a resource name, otherwise, resource will be the pluralized and hyphenated version of the associated class (per convention).

For example, if you wanted the short-object representation to contain a "link" field, as well as separate fields for id and resource name, you could override the default behavior this way:

    resource 'things' config {
        representation {
            mediaTypes = ["application/xml"]
            marshallers {
                xmlDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    includesFields {
                        field 'code'
                        field 'description'
                        shortObject { Map map ->
                            def xml = map['xml']
                            def resource = map['resourceName']
                            def id = map['resourceId']
                            xml.startNode("shortObject")
                            xml.startNode("link")
                            xml.convertAnother("/$resource/$id")
                            xml.end()
                            xml.startNode("resource")
                            xml.converAnother(resource)
                            xml.end()
                            xml.startNode("id")
                            xml.convertAnother(id)
                            xml.end()
                            xml.end()
                        }
                    }
                }
            }
        }
    }

Note that to produce a representation that can be extracted declaratively (more on this later), care must be taken to produce xml content that conforms to the declarative xml format.  In the case above, the short object closure produces a short-object value that looks like

    <shortObject>
        <link>/customers/123</link>
        <resource>customers</resource>
        <id>123</id>
    </shortObject>

Which in terms of the format is an object/map with 3 key/value pairs.  Notice it is structurally similar to JSON:

    {"link":"/customers/123", "resource":"customers", "id":"123"}

###Adding additional fields for XML marshalling
The declarative domain marshaller allows any number of closures to be added to marshall additional content.  For example, let's say we want to add affordances to all of the xml representations.  Define a marshaller template containing the closure for the affordance, then add it to the marshallers:

    xmlDomainMarshallerTemplates {
        template 'xml-affordance' config {
            additionalFields {Map map ->
                xml.startNode("_href")
                xml.convertAnother("/${map['resourceName']}/${map['resourceId']}" )
                xml.end()
            }
        }
    }

    resource 'things' config {
        representation {
            mediaTypes = ["application/xml"]
            marshallers {
                xmlDomainMarshaller {
                    inherits=['xml-affordance']
                    supports net.hedtech.restfulapi.Thing
                }
            }
        }
    }

    resource 'customers' config {
        representation {
            mediaTypes = ["application/xml"]
            marshallers {
                jsonDomainMarshaller {
                    inherits=['xml-affordance']
                    supports net.hedtech.restfulapi.Customer
                }
            }
        }
    }

Like with overriding short-object behavior, the closure receives a map.  The map contains the following keys:

    grailsApplication
    beanWrapper
    xml
    resourceName
    resourceId

where

* grailsApplication is a reference to the grailsApplication context
* beanWrapper is a BeanWrapper instance wrapping the object being marshalled
* xml is the XML converter that should be used to generate content
* resourceName is the resource name obtained as the pluralized, hyphenated version of the domain class
* resourceId is the id of the domain object

All the options for additional fields that apply to json marshalling also apply to xml marshalling.  The only difference is the closure will be passed an instance of the XML converter, instead of JSON, and care must be taken that additional fields must be added in such a way as to conform to the declarative xml format if the representation is intended to be extracted declaratively.

##Declarative Marshalling of Beans to XML
The plugin contains net.hedtech.restfulapi.marshallers.xml.BeanMarshaller and net.hedtech.restfulapi.marshallers.xml.DeclarativeBeanMarshaller, which are counterparts to their json versions.

Anywhere you can add a marshaller (in a marshaller group or representation), you can configure and add an xml declarative bean marshaller with

    xmlBeanMarshaller {}

The closure specifies how to configure the marshaller.  The options available are a superset of those for the json marshaller, except for the values passed when defining additional fields.

As with json marshallers, you can define templates with re-usable configuration:

    xmlBeanMarshallerTemplates {}

###Customizing element name for XML marshalling
By default, the declarative marshaller for xml will use the classname of the instance being rendered to choose a name for the element.  For example, a Person class will get rendered as a

    <person>

element.  (The classname is treated as a property name.)  You can override this behavior by providing an elementName explicitly:

    xmlBeanMarshaller {
        supports Person
        elementName 'APerson'
    }

A Person instance would have an xml representation

    <APerson>

using that configuration.

###Adding additional fields for XML marshalling
You can add additional fields not directly present in a bean to its marshalled representation.

The declarative xml bean marshaller allows any number of closures to be added to marshall additional content.  For example, let's say we want to add affordances to all of our xml representations.  We will define a marshaller template containing the closure for the affordance, then add it to the marshallers:

    jsonBeanMarshallerTemplates {
        template 'xml-bean-affordance' config {
            additionalFields {Map map ->
                def xml = map['xml']
                xml.startNode("_href")
                xml.convertAnother("/${map['resourceName']}/${map['resourceId']}")
                xml.end()
            }
        }
    }

    resource 'things' config {
        representation {
            mediaTypes = ["application/xml"]
            marshallers {
                jsonBeanMarshaller {
                    inherits=['xml-bean-affordance']
                    supports net.hedtech.restfulapi.Thing
                }
            }
        }
    }

the closure receives a map.  The map contains the following keys:

    grailsApplication
    beanWrapper
    xml
    resourceName
    resourceId (optional)

where

* grailsApplication is a reference to the grailsApplication context
* beanWrapper is a BeanWrapper instance wrapping the object being marshalled
* xml is the XML converter that should be used to generate content
* resourceName is the resource name obtained as the pluralized, hyphenated version of the domain class
* resourceId is the id (if available) of the bean

Note that if resourceName is specified in the additionalFieldsMap (see below), then that value is passed instead of the derived name.

Note that resourceId may not be present in the map. The marshaller attempts to provide a value for resourceId as follows:

* if the bean's metaClass has an id property, it's value is used
* if the bean's metaClass responds to a 'getId' method taking zero-arguments, the value the method returns is used
* if the bean has an 'id' property, the value of the property is used
* if the bean has a public, non-transient, non-static id field, the value of the id field is used

If none of the above conditions apply, then resourceId will not be passed in the map.

##Marshalling enumerations to XML
Marshalling an enum value with the default grails behavior will result in an xml element containing an enumType attribute with the value as the text of the element.

For example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<simpleBeanWithEnum>
    <enumValue enumType="net.hedtech.restfulapi.beans.SimpleEnum">VALUE2</enumValue>
    <name>foo</name>
</simpleBeanWithEnum>
```

If you want the string value of the enum as the element content, with no attributes added, use the net.hedtech.restfulapi.marshallers.xml.EnumMarshaller.

For example,
```groovy
    marshallerGroups {
        //marshallers included in all json representations
        group 'xml' marshallers {
            marshaller {
                instance = new net.hedtech.restfulapi.marshallers.xml.EnumMarshaller()
            }
        }
    }
```

Rendering the same bean, this would instead yield:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<simpleBeanWithEnum>
    <enumValue>VALUE2</enumValue>
    <name>foo</name>
</simpleBeanWithEnum>
```

##Declarative extraction of XML content
Just as with json, you can declaratively configure how to extract content from xml.  Anywhere you can define an extractor, you can declaratively define one with

    xmlExtractor {}

The xmlExtractor is configured the same as its json counterpart.  Just as with json, you can define templates that contain reusable configuration:

    xmlExtractorTemplates {
        template 'xml-extraction' config {
            //some config
        }
    }

However, declarative XML extraction will not work for arbitrary xml.  It only works against the json-xml format produced by the declarative marshallers.

Declarative XML extraction works in two phases.  First, the json-xml format content is parsed and extracted into maps and collections.  That is, it is essentially converted into a format that structurally matches the equivalent json representation.  Then, the rules for modifying that representation are applied to rename fields, convert short-object values, provide default values, etc as if json content had been recieved.

For example, consider the following content

    <order>
        <orderID>123</orderID>
        <customer>
            <name>Smith</name>
            <id>456</id>
            <phone-number>555-555-5555</phone-number>
        </customer>
    </order>

    Suppose that the customer name property needs to be extracted as lastName:

    resource 'purchase-orders' config {
        representation {
            mediaTypes = ["application/xml"]
            jsonExtractor {
                property 'customer.name' name 'lastName'
            }
        }
    }

The declarative extractor first parses the xml and converts it into a Map structurally similar to json:

    [orderID:"123",
     customer:[
        name:'Smith',
        id:'456'
        phone-number:'555-555-5555'
     ]
    ]

Then the extraction rules are applied to the map, in the same way they would be for json content, resulting in a final map:

    [orderID:"123",
     customer:[
        lastName:'Smith',
        id:'456'
        phone-number:'555-555-5555'
     ]
    ]

###Cautions when using xml extraction
While the json-xml format that the declarative extraction understands is structurally similar to json, there are some important differences.  The most obvious is that all primitive values are represented as text, whereas json content can distinguish between numbers, text, etc.  This means that maps declaratively extracted from the same content in json and xml formats are subtly different.  For example consider the same resource represented in json and json-xml format:

    {id:123}

    <object>
        <id>123<id>
    </object>

If the declarative extractors are used, the json content produces the map

    [id:123]

while the xml content produces the map

    [id:'123'].

That is, the value of id in the first map is a number, while in the second map, it is a string - type information for primitives is lost in the json-xml format.  It is therefore the responsibility of the service layer that binds the extracted map to objects to convert/coerce values appropriately.

##<a id="configure-marshalling-framework"></a>Configuring the marshalling framework.
The framework to use for marshalling can be specified on a per-representation basis with the marshallerFramework attribute.  For example:

    restfulApiConfig = {
        resource 'students' config {
            representation {
                mediaTypes = ['application/json']
                marshallerFramework = 'json'
            }
        }
    }

The value of the marshallerFramework must be one of the following:

    * 'none'
    * 'json'
    * 'xml'
    * the name of a bean

A value of 'none' indicates that this representation cannot be marshalled to.  It is used if a representation should only be supported in the body of a request, but never made available in a response.

A value of 'json' indicates that the grails json converter framework should be used to marshal any response.

A value of 'xml' indicates that the grails xml converter framework should be used to marshal any response.

Any value other than 'none', 'json', or 'xml' is treated as the name of a bean available in the grails application context that provides custom marshalling for the object.  It must implement a marshalObject method taking as arguments an Object and an instance of net.hedtech.restfulapi.config.RepresentationConfig.

If a value for the marshallerFramework is not explicitly set, the controller will infer the framework from the mediaType of the representation.  A media type ending in 'json' is assumed to use the 'json' framework; a type ending in 'xml' is assumed to use 'xml'.

###Marshalling Services
Any service implementing implementing the method

    Object marshalObject(Object o,RepresentationConfig config)

may be set as the value of the marshallerFramework for a representation.

The method may return any one of

* a String
* a byte array (byte[])
* an InputStream
* an instance of net.hedtech.restfulapi.marshallers.StreamWrapper

The value returned will be written to the response's output stream.  For implementations that return byte[] or StreamWrapper, the controller is also able to set the Content-Length header.

For example, suppose you want to marshal a resource 'students' represented by a Student class with groovy's MarkupBuilder, instead of the grails converter.  You can create a service to marshall it:

    class StudentMarshallingService {

        @Override
        String marshalObject(Object o, RepresentationConfig config) {
            def writer = new StringWriter()
            def xml = new MarkupBuilder(writer)

            if (o instanceof Collection) {
                Collection list = (Collection) o
                xml.studentList() {
                    list.each {
                        student() {
                            lastName(it.lastName)
                            firstName(it.firstName)
                        }
                    }
                }
            } else {
                xml.student() {
                    lastName(it.lastName)
                    firstName(it.firstName)
                }
            }
            return writer.toString()
        }
    }

Note that the marshalling service has to handle both the object, and collections of the object.

And then configure the representation to use the service:

    restfulApiConfig = {
        resource 'students' config {
            representation {
                mediaTypes = ['application/xml']
                marshallerFramework = 'studentMarshallingService'
            }
        }
    }

This is a simple example.  In general, a marshalling service implementation would be written to handle multiple classes by delegating to another framework (JAXB, etc) to accomplish marshalling.

Implementations that return bbyte[] or an InputStream can be used to support marshalling for resources that have binary representations, such as PDF.

##Restricting representations to responses or requests only.
You may have situations in which a resource representation is only intended to be used in a response or request, but not both.  For example, you may have a complex representation that fully renders nested objects that is appropriate to return in list and show operations, but that you don't want to have to consume for create or update operations.  You can (indirectly) control this, by controlling whether the representation has an extractor or marshalling framework defined.

To prevent a representation from being used in a create, update, or delete operation, do not specify an extractor instance (or set it to null):

    restfulApiConfig = {
        resource 'students' config {
            representation {
                mediaTypes = ['application/json']
                marshallers {
                    jsonDomainMarshaller {}
                }
                extractor = null
            }
        }
    }

When the controller encounters a representation in a request body that has no extractor, it will respond correctly with a 415 status code.

To prevent a representation from being returned as the response to any request, specify 'none' for the marshallerFramework:

    restfulApiConfig = {
        resource 'students' config {
            representation {
                mediaTypes = ['application/json']
                marshallerFramework = 'none'
                jsonExtractor {}
            }
        }
    }

Any request requiring the representation will receive a 406 status code.

##Logging
Errors encountered while servicing a request are logged at error level to the log target 'RestfulApiController_messageLog'.  This is so errors occuring from the requests (which will typically be errors caused by invalid input, etc) can be separated from errors in the controller.

##<a id="api-testing"></a>Testing an API
The plugin contains the class net.hedtech.restfulapi.spock.RestSpecification which may be extended to write functional spock tests.  Spock is a testing and specification framework that is very expressive.  It is strongly recommended that you use spock to test your APIs.

The RestSpecification class contains a number of convenience methods for making calls to Restful APIs as part of the functional test phase, such as get and post methods that take closures in which you can define headers, body content, etc.

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

The RestSpecification can also print out the details of the request/response for each test, including the headers.  To enable this, pass "-DRestSpecification.display=true --echoOut" when running the test.

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

###JSON-P
The plugin does not support JSON-P.



