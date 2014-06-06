/* ****************************************************************************
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
 *****************************************************************************/

import org.codehaus.groovy.grails.web.converters.marshaller.ClosureObjectMarshaller

// locations to search for config files that get merged into the main config;
// config files can be ConfigSlurper scripts, Java properties files, or classes
// in the classpath in ConfigSlurper format

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination

// The ACCEPT header will not be used for content negotiation for user agents containing the following strings (defaults to the 4 major rendering engines)
grails.mime.disable.accept.header.userAgents = ['Gecko', 'WebKit', 'Presto', 'Trident']
grails.mime.types = [ // the first one is the default format
    all:           '*/*', // 'all' maps to '*' or the first available format in withFormat
    atom:          'application/atom+xml',
    css:           'text/css',
    csv:           'text/csv',
    form:          'application/x-www-form-urlencoded',
    html:          ['text/html','application/xhtml+xml'],
    js:            'text/javascript',
    json:          ['application/json', 'text/json'],
    multipartForm: 'multipart/form-data',
    rss:           'application/rss+xml',
    text:          'text/plain',
    hal:           ['application/hal+json','application/hal+xml'],
    xml:           ['text/xml', 'application/xml']
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// Legacy setting for codec used to encode data with ${}
grails.views.default.codec = "html"

// The default scope for controllers. May be prototype, session or singleton.
// If unspecified, controllers are prototype scoped.
grails.controllers.defaultScope = 'singleton'

// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside ${}
                scriptlet = 'html' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        // filteringCodecForContentType.'text/html' = 'html'
    }
}


grails.converters.encoding = "UTF-8"
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

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

// configure passing transaction's read-only attribute to Hibernate session, queries and criterias
// set "singleSession = false" OSIV mode in hibernate configuration after enabling
grails.hibernate.pass.readonly = false
// configure passing read-only to OSIV session by default, requires "singleSession = false" OSIV mode
grails.hibernate.osiv.readonly = false

environments {
    development {
        grails.logging.jul.usebridge = true
    }
    production {
        grails.logging.jul.usebridge = false
        // TODO: grails.serverURL = "http://www.changeme.com"
    }
}


// ******************************************************************************
//                             Logging Configuration
// ******************************************************************************
//
import org.apache.log4j.*
log4j.main = {

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
                instance = new org.codehaus.groovy.grails.web.converters.marshaller.ClosureObjectMarshaller<grails.converters.JSON>(
                        java.util.Date, {return it?.format("yyyy-MM-dd'T'HH:mm:ssZ")})
            }
        }

        group 'json-date-closure' marshallers {
            marshaller {
                instance = new org.codehaus.groovy.grails.web.converters.marshaller.ClosureObjectMarshaller<grails.converters.JSON>(
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
                property 'thing' shortObject true flatObject false
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
}
