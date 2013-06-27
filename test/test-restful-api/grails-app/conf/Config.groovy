/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

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
        // TODO: grails.serverURL = "http://www.changeme.com"
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
//
cors.url.pattern = '/api/*'
cors.allow.origin.regex='.*'
cors.expose.headers='content-type,X-hedtech-totalCount,X-hedtech-pageOffset,X-hedtech-pageMaxSize,X-hedtech-message,X-hedtech-Media-Type'

// ******************************************************************************
//                       RESTful API Endpoint Configuration
// ******************************************************************************
//
restfulApiConfig = {

    jsonDomainMarshallerTemplates {
        template 'domainAffordance' config {
            additionalFields {map ->
                map['json'].property("_href", "/${map['resourceName']}/${map['resourceId']}" )
            }
        }
    }

    marshallerGroups {
        group 'json-date-closure' marshallers {
            marshaller {
                instance = new org.codehaus.groovy.grails.web.converters.marshaller.ClosureOjectMarshaller<grails.converters.JSON>(
                    java.util.Date, {return "customized-date:" + it?.format("yyyy-MM-dd'T'HH:mm:ssZ")})
            }
        }
    }

    resource 'things' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits = ['domainAffordance']
                }
            }
            jsonExtractor {}
        }
        representation {
            mediaTypes = ["application/xml"]
            marshallers {
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.xml.AffordanceDomainClassMarshaller(app:grailsApplication)
                    priority = 200
                }
            }
            extractor = new net.hedtech.restfulapi.extractors.xml.MapExtractor()
        }
        representation {
            mediaTypes =  ['application/vnd.hedtech.v0+json']
            marshallers {
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.json.BasicDomainClassMarshaller(app:grailsApplication)
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
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.xml.AffordanceDomainClassMarshaller(app:grailsApplication)
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
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.json.BasicDomainClassMarshaller(app:grailsApplication)
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
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.xml.BasicDomainClassMarshaller(app:grailsApplication)
                    priority = 100
                }
            }
            extractor = new net.hedtech.restfulapi.extractors.xml.MapExtractor()
        }
        representation {
            mediaTypes = ['application/vnd.hedtech.additional.field.closure+json']
            marshallers {
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.json.BasicDomainClassMarshaller(app:grailsApplication)
                    priority = 100
                }
                jsonDomainMarshaller {
                    supports net.hedtech.restfulapi.Thing
                    priority = 101
                    additionalFields { map ->
                        def json = map['json']
                        def beanWrapper = map['beanWrapper']
                        json.property("sha1", beanWrapper.getWrappedInstance().getSupplementalRestProperties()['sha1'])
                        json.property("numParts", beanWrapper.getWrappedInstance().parts.size())
                    }
                }
            }
            jsonExtractor {}
        }
    }

    resource 'thing-wrappers' config {
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.json.BasicDomainClassMarshaller(app:grailsApplication)
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
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.json.BasicDomainClassMarshaller(app:grailsApplication)
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
                    inherits = ['domainAffordance']
                }
            }
            jsonExtractor {}
        }
    }

    //test overriding conventions for finding the service for a resource
    //we will map thingamabobs to thingService.  There is no thingamabob domain
    //object or service.
    resource 'thingamabobs' config {
        serviceName = 'thingService'
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.json.BasicDomainClassMarshaller(app:grailsApplication)
                    priority = 100
                }
            }
            jsonExtractor {}
        }
    }

    //test overriding supported methods on a resource
    //allow show and update only
    resource 'limitedthings' config {
        serviceName = 'thingService'
        methods = ['show','create','update']
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                marshaller {
                    instance = new net.hedtech.restfulapi.marshallers.json.BasicDomainClassMarshaller(app:grailsApplication)
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
    resource 'special-things' config {
        serviceName = 'thingService'
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits = ['domainAffordance']
                    additionalFieldsMap = [resourceName:'special-things']
                    field 'parts' resource 'thing-parts'
                }
            }
            jsonExtractor {}
        }
    }

    //Testing using marshalling groups that use a closure to marshall a date object
    resource 'closure-things' config {
        serviceName = 'thingService'
        representation {
            mediaTypes = ["application/json"]
            marshallers {
                jsonDomainMarshaller {
                    inherits = ['domainAffordance']
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
                jsonGroovyBeanMarshaller {
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

}
