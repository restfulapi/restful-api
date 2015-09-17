import grails.util.BuildSettings
import grails.util.Environment

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d [%t] %-5p %c{2} %x - %m%n"
    }
}

root(ERROR, ['STDOUT'])

logger('grails.app.controllers', ERROR)
logger('grails.app.services', ERROR)
logger('net.hedtech.restfulapi.marshallers', ERROR)

logger('.grails.web', ERROR)
logger('grails.web.servlet', ERROR)
logger('grails.web.pages', ERROR)          // GSP
logger('grails.web.sitemesh', ERROR)        // layouts

logger('grails.web.mapping.filter', ERROR)  // URL mapping
logger('grails.web.mapping', ERROR)         // URL mapping

logger('grails.commons', ERROR)            // core / classloading
logger('grails.plugins', ERROR)            // plugins
logger('org.grails.orm.hibernate', ERROR)      // hibernate integration
logger('org.springframework', ERROR)
logger('org.hibernate', ERROR)
logger('net.sf.ehcache.hibernate', ERROR)

logger('RestfulApiController_messageLog', FATAL)
logger( 'net.hedtech.restfulapi', ERROR)

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode() && targetDir) {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "${targetDir}/stacktrace.log"
        append = true
        encoder(PatternLayoutEncoder) {
            pattern = "%level %logger - %msg%n"
        }
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
}
