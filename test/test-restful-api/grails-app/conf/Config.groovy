/* ****************************************************************************
 * Copyright 2013-2019 Ellucian Company L.P. and its affiliates.
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
 *****************************************************************************/

grails.mime.file.extensions   = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = true
grails.mime.types = [
    all:                   '*/*',
    atom:                  'application/atom+xml',
    css:                   'text/css',
    csv:                   'text/csv',
    form:                  'application/x-www-form-urlencoded',
    html:                  ['text/html','application/xhtml+xml'],
    js:                    'text/javascript',
    json:                  ['application/json', 'text/json'],
    multipartForm:         'multipart/form-data',
    rss:                   'application/rss+xml',
    text:                  'text/plain',
    xml:                   ['application/xml','text/xml']
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding  = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

//grails.converters.json.circular.reference.behaviour = "INSERT_NULL"

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']


// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

//throw validation exception during save by default
grails.gorm.failOnError = true

//grails.converters.default.pretty.print=true

environments {
    development {
        grails.logging.jul.usebridge = true
    }
    production {
        grails.logging.jul.usebridge = false
    }
}

// ******************************************************************************
//                             Logging Configuration
// ******************************************************************************
//
import org.apache.log4j.*
log4j = {

    error  'grails.app.controllers'
    error  'grails.app.services'
    error  'net.hedtech.restfulapi.marshallers'

    error  'org.codehaus.groovy.grails.web'
    error  'org.codehaus.groovy.grails.web.servlet',        // controllers
           'org.codehaus.groovy.grails.web.pages',          // GSP
           'org.codehaus.groovy.grails.web.sitemesh'        // layouts

    error  'org.codehaus.groovy.grails.web.mapping.filter'  // URL mapping
    error  'org.codehaus.groovy.grails.web.mapping'         // URL mapping

    error  'org.codehaus.groovy.grails.commons',            // core / classloading
           'org.codehaus.groovy.grails.plugins',            // plugins
           'org.codehaus.groovy.grails.orm.hibernate',      // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'

    fatal  'RestfulApiController_messageLog'
    error  'net.hedtech.restfulapi'

    appenders {
        appender new ConsoleAppender(name: "console",
            // threshold: Priority.WARN,
            layout: pattern(conversionPattern:'%d [%t] %-5p %c{2} %x - %m%n')
        )
    }

    root {
        error 'console', 'appFile'
        additivity = true
    }
}

// ******************************************************************************
//                          Date Format Configuration
// ******************************************************************************
// Note: This configuration is used by this test app's CustomPropertyEditorRegistrar
//
grails.date.formats = [ "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd HH:mm:ss-SSSS", "dd.MM.yyyy HH:mm:ss" ]

// ******************************************************************************
//                          Client Caching Configuration
// ******************************************************************************
//
//cache.headers.enabled = false // Uncomment to disable


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
// Note: Tests within this test app expect the default values that are shown below.
// These do not need to be configured (hence they are commented out) unless you
// want to override the naming. (The 'built-in' names are shown below.)
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
// The paging parameter names may be overriden if desired.
// The commented out settings show the 'built in' defaults (grails default)
//
//restfulApi.page.max    = 'max'
//restfulApi.page.offset = 'offset'


// ******************************************************************************
//             RESTful API deprecated response headers
// ******************************************************************************
// In the deprecatedHeaderMap:
//  - key is the current header
//  - value is a previous header that is now deprecated
//  - value may also be a list of deprecated headers if there is more than one
//
restfulApi.deprecatedHeaderMap = [
        'X-hedtech-Media-Type': 'X-Media-Type-old',
        'X-hedtech-totalCount': ['X-Total-Count-old1', 'X-Total-Count-old2']
]


// ******************************************************************************
//                       RESTful API Endpoint Configuration
// ******************************************************************************
//
restfulApiConfig = {

    jsonDomainMarshallerTemplates {
        template 'jsonDomainAffordance' config {
            additionalFields {map ->
                map['json'].property("_href", "/${map['resourceName']}/${map['resourceId']}" )
            }
        }
    }

    xmlDomainMarshallerTemplates {
        template 'xmlDomainAffordance' config {
            additionalFields {map ->
                def xml = map['xml']
                xml.startNode('_href')
                xml.convertAnother("/${map['resourceName']}/${map['resourceId']}")
                xml.end()
            }
        }
    }

    marshallerGroups {
        //marshallers included in all json representations
        group 'json' marshallers {
            marshaller {
                instance = new org.codehaus.groovy.grails.web.converters.marshaller.ClosureOjectMarshaller<grails.converters.JSON>(
                        java.util.Date, {return it?.format("yyyy-MM-dd'T'HH:mm:ssZ")})
            }
        }

        group 'json-date-closure' marshallers {
            marshaller {
                instance = new org.codehaus.groovy.grails.web.converters.marshaller.ClosureOjectMarshaller<grails.converters.JSON>(
                    java.util.Date, {return "customized-date:" + it?.format("yyyy-MM-dd'T'HH:mm:ssZ")})
            }
        }
    }

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

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallerFramework = 'json'
            marshallers {
                jsonDomainMarshaller {
                    inherits = ['jsonDomainAffordance']
                }
            }
            jsonExtractor {}
        }
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
        representation {
            mediaTypes = ["application/xml"]
            marshallers {
                xmlDomainMarshaller {
                    inherits = ['xmlDomainAffordance']
                    priority = 200
                }
            }
            extractor = new net.hedtech.restfulapi.extractors.xml.MapExtractor()
        }
        representation {
            mediaTypes = ['application/vnd.hedtech.v0+json']
            marshallers {
                xmlDomainMarshaller {
                    priority = 100
                }
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.json.ThingClassMarshaller(grailsApplication)
                    priority = 101
                }
            }
            jsonExtractor {}
        }
        representation {
            mediaTypes = ['application/vnd.hedtech.v0+xml']
            marshallers {
                xmlDomainMarshaller {
                    inherits = ['xmlDomainAffordance']
                    priority = 100
                }
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.xml.ThingClassMarshaller(grailsApplication)
                    priority = 101
                }
            }
            extractor = new net.hedtech.restfulapi.extractors.xml.MapExtractor()
        }
        representation {
            mediaTypes = ['application/vnd.hedtech.thing.v0+xml']
            marshallers {
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.xml.v0.ThingClassMarshaller()
                    priority = 101
                }
            }
            extractor = new net.hedtech.restfulapi.extractors.xml.v0.ThingExtractor()
        }
        representation {
            mediaTypes = ['application/vnd.hedtech.thing.v1+xml']
            marshallers {
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.xml.v1.ThingClassMarshaller()
                    priority = 101
                }
            }
            extractor = new net.hedtech.restfulapi.extractors.xml.v1.ThingExtractor()
        }
        representation {
            mediaTypes = ['application/vnd.hedtech.v1+json']
            marshallers {
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.json.BasicDomainClassMarshaller(app:grailsApplication)
                    priority = 101
                }
            }
            extractor = new net.hedtech.restfulapi.extractors.json.ThingDefaultDescriptionExtractor()
        }
        //same as v1, but demonstrate declarative extraction
        representation {
            mediaTypes = ['application/vnd.hedtech.v2+json']
            marshallers {
                jsonDomainMarshaller {
                    priority = 101
                }
            }
            jsonExtractor {
                property 'description' defaultValue 'Default description'
            }
        }
        representation {
            mediaTypes = ['application/vnd.hedtech.v1+xml']
            marshallers {
                xmlDomainMarshaller {
                    priority = 100
                }
            }
            extractor = new net.hedtech.restfulapi.extractors.xml.MapExtractor()
        }
        representation {
            mediaTypes = ['application/vnd.hedtech.additional.field.closure+json']
            marshallers {
                jsonDomainMarshaller {
                    priority = 100
                }
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    priority = 101
                    additionalFields { map ->
                        def json = map['json']
                        def beanWrapper = map['beanWrapper']
                        json.property("sha1", beanWrapper.getWrappedInstance().getSupplementalRestProperties()['sha1'])
                        json.property("tenant", beanWrapper.getWrappedInstance().getSupplementalRestProperties()['tenant'])
                        json.property("numParts", beanWrapper.getWrappedInstance().parts.size())
                    }
                }
            }
            jsonExtractor {}
        }
        representation {
            mediaTypes = ['application/vnd.hedtech.custom-framework+xml']
            marshallerFramework = 'customThingMarshallingService'
            xmlExtractor {}
        }
        representation {
            mediaTypes = ['application/vnd.hedtech.custom-framework+xml+zip']
            contentType = 'application/zip'
            marshallerFramework = 'compressedCustomThingMarshallingService'
        }
        representation {
            mediaTypes = ['application/vnd.hedtech.custom-framework+xml+stream']
            contentType = 'application/zip'
            marshallerFramework = 'streamCustomThingMarshallingService'
        }
        representation {
            mediaTypes = ['application/vnd.hedtech.custom-framework+xml+streamsize']
            contentType = 'application/zip'
            marshallerFramework = 'streamSizeCustomThingMarshallingService'
        }
    }

    resource 'thing-wrappers' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    priority = 100
                }
            }
            jsonExtractor {}
        }
    }

    resource 'complex-things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    priority = 100
                }
            }
            jsonExtractor {}
        }
    }

    resource 'part-of-things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits = ['jsonDomainAffordance']
                }
            }
            jsonExtractor {
                property 'thing' shortObject true flatObject true
            }
        }
    }

    //test overriding conventions for finding the service for a resource
    //we will map thingamabobs to thingService.  There is no thingamabob domain
    //object or service.
    //
    resource 'thingamabobs' config {
        serviceName = 'thingService'
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    priority = 100
                }
            }
            jsonExtractor {}
        }
    }

    //test overriding supported methods on a resource
    //allow show and update only
    //
    resource 'limitedthings' config {
        serviceName = 'thingService'
        methods = ['show','create','update']
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    priority = 100
                }
            }
            jsonExtractor {}
        }
    }

    //Test that when the resource name is not obtained from the
    //pluralized domain class name, that we can override
    //as needed for affordances.
    //Also test that we can override the resource name for asShortObject
    //
    resource 'special-things' config {
        serviceName = 'thingService'
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits = ['jsonDomainAffordance']
                    additionalFieldsMap = [resourceName:'special-things']
                    field 'parts' resource 'thing-parts'
                }
            }
            jsonExtractor {}
        }
    }

    //Testing using marshalling groups that use a closure to marshall a date object
    //
    resource 'closure-things' config {
        serviceName = 'thingService'
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits = ['jsonDomainAffordance']
                    additionalFieldsMap = [resourceName:'closure-things']
                }
                marshallerGroup 'json-date-closure'
            }
            jsonExtractor {}
        }

    }

    resource 'groovy-thing-wrappers' config {
        representation {
            mediaTypes = ["application/json"]
            serviceName = 'thingWrapperService'
            marshallers {
                jsonDomainMarshaller {
                    priority = 100
                }
                jsonBeanMarshaller {
                    supports net.hedtech.restfulapi.ThingWrapper
                    includesFields {
                        field 'things'
                        field 'complexCode'
                        field 'xlarge'
                    }
                }
            }
            jsonExtractor {}
        }
    }

    // This resource is used to test that a service-specific service adapter
    // may be used. See resources.groovy where a 'nothingServiceAdapter' is
    // configured.
    //
    resource 'nothings' config {
        serviceName = 'nothingService'
        serviceAdapterName = 'nothingServiceAdapter'
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    priority = 100
                }
            }
            jsonExtractor {}
        }
    }

    //created the resource to test representation service name functionality
    resource 'representation-things' config {
        serviceAdapterName = 'nothingServiceAdapter'
        representation {
            representationServiceName = 'nothingService'
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    priority = 100
                }
            }
            jsonExtractor {}
        }
    }
}
