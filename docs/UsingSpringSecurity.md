<!-- ***************************************************************************
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
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

#Securing RESTful APIs using Spring Security

The restful-api plugin does not itself address security requirements, nor does it mandate any particular approach or library (such as Spring Security).

This document is provided to show how the [Spring Security Core](http://grails.org/plugin/spring-security-core) (version 2.0-RC4) plugin and the [Spring Security REST](https://github.com/alvarosanchez/grails-spring-security-rest) (version 1.4.0) plugin can be used to protect API endpoints.

First, in [part 1](#part-1), we'll incorporate Spring Security to use Basic Authentication and cookies, which is a very common (and default) configuration for non-RESTful APIs.  This is **NOT the preferred approach** for RESTful APIs, however, as it requires either server-side state (a session) along with the passing of a cookie (to identify that session), or it requires us to re-authenticate on every API call (which might be costly).

In [part 2](#part-2), we'll configure a subset of our URLs (our stateless, RESTful API) to use a simple token-based approach.

_Note: Spring Security may be used to support Single Sign-On (SSO) based on OAuth 2, SAML 2, CAS, and more \-\- but this is beyond the scope of this guide (and there are many resources on the web that provide such guidance)._

IMPORTANT: The Spring Security Core Plugin requires Grails 2.3.x or later.  This version of the RESTful API plugin is configured to support Grails 2.2.x.  Consequently, as a prerequisite to following this guide you will first need to follow the instructions found in [docs/README-TESTING.md](https://github.com/restfulapi/restful-api/blob/master/docs/README-TESTING.md) to apply the simple (yet breaking) changes necessary to make the RESTful API plugin compatible with Grails 2.3.x or later. (I used Grails 2.3.9.)

This guide assumes you have an application that exposes a RESTful API using this plugin. You may use the test-restful-api application found under this plugin's test directory to follow along with this guide.

#<a name="part-1"></a>Part 1: Starting with the default cookie-based Authentication

###Install Spring Security Core Plugin

First, edit grails-app/conf/BuildConfig as described below (and also described at [http://grails.org/plugin/spring-security-core](http://grails.org/plugin/spring-security-core)) and run 'grails compile' to complete the installation.

Edit BuildConfig.groovy to add the following within the 'plugins' block:

```groovy
compile ':spring-security-core:2.0-RC4'
```

Then run 'grails compile' to complete the installation.

###Create User and Role classes

Create User and Role classes by using the 's2-quickstart' script that is provided by the Spring Security Core Plugin.

```bash
grails s2-quickstart {your-package} User Role
```

###Bootstrap Users and Roles

You will need to ensure users and roles exist.  For a simple test, you can modify BootStrap.groovy to instantiate a user and a role during initializaiton. For this guide, we'll create a single 'ROLE_API_USER' role and a single user having a username of 'api'.

Please add the following to the 'init' closure within BootStrap.groovy (except use the correct package names for your own User and Role classes).

```groovy
    def userRole = new Role(authority: 'ROLE_API_USER').save(flush: true)
    def testUser = new User(username: 'api', enabled: true, password: 'password')

    testUser.save(flush: true)
    UserRole.create testUser, userRole, true

    assert User.count() == 1
    assert Role.count() == 1
    assert UserRole.count() == 1
```
_(Don't forget to add 'import' statements to your User and Role class.)_

###Configure Spring Security

The Spring Security Core Plugin default configuration supports securing your controller methods using a '@Secured' annotation.  While this is great for controllers that are part of your application, this is less desirable when using controllers (such as the RestfulApiController) provided by a plugin.

Fortunately, the Spring Security Core Plugin makes it easy to secure such 'plugin' controllers using a 'staticRules' map (that is discussed below).

Note there is an (older) alternative to using the now-default 'annotation' based approach (along with the 'staticRules' map used to protect the RestfulApiController). Instead, you may use an 'interceptUrlMap'.  This alternative is also discussed below, but let's first continue our configuration based on 'annotations' and the 'staticRules'.

####Enable Basic Authentication

Regardless of whether you will use a 'staticRules' map or a 'interceptUrlMap' map, we'll need to make sure we don't pass any credentials using clear text. To protect against this, we'll use Basic Authentication. So, we'll first add the following to Config.groovy

```groovy
grails.plugin.springsecurity.useBasicAuth = true
grails.plugin.springsecurity.basic.realmName = "HTTP Basic Auth Demo"

grails.plugin.springsecurity.secureChannel.definition = [
   '/j_spring_security_check':   'REQUIRES_SECURE_CHANNEL',
   '/api/**':                    'REQUIRES_SECURE_CHANNEL'
]
```

####Using a 'staticRules' Map

Since using 'annotations' is the default approach, when you executed the 's2-quickstart' script above the following was automatically added to your Config.groovy:

```groovy
// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'net.hedtech.security.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'net.hedtech.security.UserRole'
grails.plugin.springsecurity.authority.className = 'net.hedtech.security.Role'
grails.plugin.springsecurity.controllerAnnotations.staticRules = [
    '/':                              ['permitAll'],
    '/index':                         ['permitAll'],
    '/index.gsp':                     ['permitAll'],
    '/assets/**':                     ['permitAll'],
    '/**/js/**':                      ['permitAll'],
    '/**/css/**':                     ['permitAll'],
    '/**/images/**':                  ['permitAll'],
    '/**/favicon.ico':                ['permitAll']
]
```

_(Note the above reflects the package I used ('net.hedtech.security') when creating the User and Role classes; yours will likely be different.)_

The 'staticRules' map is used to configure security for static assets, but can also be used to configure security in controllers where you do not want to add a @Secured annotation.  Since the RestfulApiPlugin is provided by a plugin, we'll protect our API using the above map.

Add the following to the staticRules map:

```groovy
    '/restfulapi/**':                 ['ROLE_API_USER']
```

Note that instead of 'api' (the URL) we need to use the lower-case controller name (that was used in our UrlMapping.groovy when mapping to the 'api' URL).

####Alternative approach: Using an 'interceptUrlMap' Map

If instead of the staticRules map (and annotations within your other controllers) you would prefer to use an interceptUrlMap, please delete the staticRules map that was added by the s2-quickstart script and instead add the following to your Config.groovy file:

```groovy
import grails.plugins.springsecurity.SecurityConfigType
grails.plugins.springsecurity.securityConfigType = SecurityConfigType.InterceptUrlMap

grails.plugins.springsecurity.interceptUrlMap = [
    '/api/**':    ['ROLE_API_USER'],
]
```

Unlike the 'staticRules' map, the 'interceptUrlMap' uses the 'URL', so we use 'api' instead of 'restfulapi' as we did before.

####Test

Since we have configured our API so that HTTPS is required, we would normally ensure we have a valid certificate.  For this testing, we'll have Grails create one automatically (by using the -https option) and we'll instruct curl to not validate it (by using the -k switch).

Since I am using the test-restful-api application as an example, I'll start the server using:

```
grails -DseedThings=true run-app -https
```

Note the -DseedThings=true is used within Bootstrap.groovy as a signal to create some 'things' within the in-memory database.  The '-https', again, tells Grails to create an self signed certificate and run using SSL.

We have now protected our API endpoint, so if we hit an API without providing correct credentials we will get a '401 Unauthorized' response.

```html
$ curl -k -i --noproxy localhost -H "Accept: application/json" https://localhost:8443/test-restful-api/api/things?max=10
HTTP/1.1 401 Unauthorized
Server: Apache-Coyote/1.1
WWW-Authenticate: Basic realm="HTTP Basic Auth Demo"
Content-Type: text/html;charset=utf-8
Content-Length: 1061
Date: Wed, 29 Oct 2014 22:22:58 GMT

(HTML content not shown)
```

Unfortunately, the above Content-Type is 'text/html' and the login page markup was returned. This isn't what we want, so we'll fix that later.  But before we do, let's make sure that we can in fact access this endpoint when we have valid credentials.

Like other Spring Security based applications, we'll need to interact with the j_spring_security_check endpoint to perform the authentication. To do this, we'll submit our user and password (that we seeded in Bootstrap.groovy) and store the results of this authentication in a 'cookie-jar' named 'cookies.txt'.

```
curl -k --data "j_username=api&j_password=password" https://localhost:8443/test-restful-api/j_spring_security_check --cookie-jar cookie.txt
```
We'll then use this cookie in all subsequent calls.

```
curl -k -i --noproxy localhost -H "Accept: application/json" https://localhost:8443/test-restful-api/api/things?max=10 --cookie cookie.txt
[{"id":1,"version":0,"_href":"/things/1","code":"AA","dateManufactured":"2014-10-29T18:48:42-0400","description":"Thing with code AA.",....(Remaining JSON Array content not shown)
```
Success!  So, now let's fix our authentication failures so that it is reported using JSON and not by returning the login page!

####Create a Basic Authentication Entry Point

We really want a basic authentication entry point that can support content negotiation, so that if JSON is requested we respond with a Content-Type of application/json, and if XML is requested we respond with a Content-Type of application/xml.

To do this, create a src/groovy/net/hedtech/api/security/RestApiAuthenticationEntryPoint.groovy file with the following content:

```groovy
package net.hedtech.security

import java.io.IOException
import java.io.PrintWriter

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import net.hedtech.restfulapi.MediaType
import net.hedtech.restfulapi.MediaTypeParser

import org.springframework.http.HttpHeaders
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint


public class RestApiAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

    MediaTypeParser  mediaTypeParser = new MediaTypeParser()

    @Override
    public void commence( HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException)
                throws IOException, ServletException {

        String contentType
        String content
        MediaType[] acceptedTypes = mediaTypeParser.parse(request.getHeader(HttpHeaders.ACCEPT))
        def type = acceptedTypes.size() > 0 ? acceptedTypes[0].name : ""

        switch(type) {
            case ~/.*xml.*/:
                contentType = 'application/xml'
                content = "<Errors><Error><Code>${HttpServletResponse.SC_UNAUTHORIZED}</Code></Error></Errors>"
                break
            case ~/.*json.*/:
                contentType = 'application/json'
                content = "{ \"errors\" : [ { \"code\":\"${HttpServletResponse.SC_UNAUTHORIZED}\" } ] }"
                break
            default:
                contentType = 'plain/text'
                content = HttpServletResponse.SC_UNAUTHORIZED + " - " + authException.getMessage()
                break
        }

        response.addHeader("Content-Type", contentType )
        response.addHeader("WWW-Authenticate", "Basic realm=\"" + getRealmName() + "\"")
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        PrintWriter writer = response.getWriter()
        writer.println(content)
    }
}
```

Next we need to tell Spring Security to use our custom entry point for Basic Authentication. We'll register the new RestApiAuthenticationEntryPoint bean within the application context, by editing the resources.groovy file:

```groovy
import net.hedtech.security.RestApiAuthenticationEntryPoint

import org.springframework.security.web.access.ExceptionTranslationFilter
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
```

Add include the following beans:

```groovy
restApiAuthenticationEntryPoint(RestApiAuthenticationEntryPoint) {
    realmName = 'Example RESTful API Realm'
}

basicAuthenticationFilter(BasicAuthenticationFilter) {
    authenticationManager = ref('authenticationManager')
    authenticationEntryPoint = ref('restApiAuthenticationEntryPoint')
}

basicExceptionTranslationFilter(ExceptionTranslationFilter) {
    authenticationEntryPoint = ref('restApiAuthenticationEntryPoint')
    accessDeniedHandler = ref('accessDeniedHandler')
}
```

Invoking our endpoint now, without credentials or with bad credentials, will now result in the following:

```
$ curl -k -i --noproxy localhost -H "Accept: application/json" https://localhost:8443/test-restful-api/api/things?max=10
HTTP/1.1 401 Unauthorized
Server: Apache-Coyote/1.1
Set-Cookie: JSESSIONID=8AF006D1968E6E508BF302D32C1DA005; Path=/test-restful-api/; HttpOnly
WWW-Authenticate: Basic realm="Example RESTful API Realm"
Content-Type: application/json;charset=ISO-8859-1
Content-Length: 36
Date: Wed, 29 Oct 2014 23:09:18 GMT

{ "errors" : [ { "code":"401" } ] }
```

Much better :-)

#<a name="part-2"></a>Part 2: Securing RESTful APIs using a Token

Part 2 shows how to protect our stateless, RESTful APIs using a simple token-based approach while continuing to allow other stateful endpoints (e.g., that expose a UI) to continue using the above (default) cookie-based approach.  (This builds upon [part 1](#part-1), so please ensure cookie-based authentication is working before continuing.)

To support our simple token based approach we'll use the very nice [Spring Security REST](https://github.com/alvarosanchez/grails-spring-security-rest) plugin.

###Install and Configure the Spring Security REST Plugin

Note: The Spring Security REST plugin's [documentation](http://alvarosanchez.github.io/grails-spring-security-rest/docs/guide/introduction.html) is well written and you may want to read it before continuing.

Add to the 'repositories' block in BuildConfig.groovy:

```
mavenRepo 'http://repo.spring.io/milestone'
```

From part 1, you should already have this line in the 'plugins' block:

```
 'compile ":spring-security-core:2.0-RC4"')
```

So now also add this:

```
compile ":spring-security-rest:1.4.1.RC1", {
    excludes: 'spring-security-core'
}
```

In Config.groovy, we'll refine the filter map used for URLs.  Specifically, we'll 'remove' any filters that are not compatible with a 'stateless' endpoint for our 'api' URLs.

```
grails.plugin.springsecurity.filterChain.chainMap = [
   '/api/**': 'JOINED_FILTERS,-exceptionTranslationFilter,-authenticationProcessingFilter,-securityContextPersistenceFilter,-                                  rememberMeAuthenticationFilter',  // Stateless chain
   '/**': 'JOINED_FILTERS,-restTokenValidationFilter,-restExceptionTranslationFilter'  // Traditional chain
]
```

Next we'll configure GORM as our storage for our tokens.  We'll identify a token domain class, using it's fully qualified name.  (We'll implement this domain class in the next step.)  Also note that GORM is just one approach the Spring Security REST plugin supports, and is used here for simplicity.

```
grails.plugin.springsecurity.rest.token.storage.useGorm = true
grails.plugin.springsecurity.rest.token.storage.gorm.tokenDomainClassName = 'your.package.name.AuthenticationToken'
grails.plugin.springsecurity.rest.token.storage.gorm.tokenValuePropertyName = 'tokenValue'
grails.plugin.springsecurity.rest.token.storage.gorm.usernamePropertyName = 'username'
```

Now we'll implement the AuthenticationToken domain class configured above.  We'll create this class under 'grails-app/domain' (using the approprate directory structure for your package):

```groovy
package net.hedtech.security

class AuthenticationToken {

    String tokenValue
    String username

    static constraints = {
        username blank: false, unique: true
        tokenValue blank: false
    }

    static mapping = {
    }
}
```

####Test

As we did to test our 'cookie-based' authentication, we'll start the server as follows:

```
grails -DseedThings=true run-app -https
```

If we hit an API without providing correct credentials we will get a '401 Unauthorized' response.

```html
$ curl -k -i --noproxy localhost -H "Accept: application/json" https://localhost:8443/test-restful-api/api/things?max=10
HTTP/1.1 401 Unauthorized
Server: Apache-Coyote/1.1
WWW-Authenticate: Bearer error="invalid_token"
Content-Length: 0
Date: Fri, 31 Oct 2014 02:22:53 GMT
```

To authenticate, we'll ask for a token by POSTing to the 'api/login' URL.  Making sure to use HTTPS, we'll pass our credentials. We'll pipe this into node so we can parse the response and extract the token, which we'll capture in a 'token' file for use in subsequent invocations.

```
curl -X POST -s -k -d '{"username":"api","password":"password"}' -H 'Content-Type: application/json' https://localhost:8443/test-restful-api/api/login | node -pe "JSON.parse(require('fs').readFileSync('/dev/stdin').toString()).access_token" > token
```

We'll then use this token in all subsequent calls.

```
curl -k -i --noproxy localhost -H "Authorization: Bearer $(cat token)" -H "Accept: application/json" https://localhost:8443/test-restful-api/api/things?max=10
HTTP/1.1 200 OK
Server: Apache-Coyote/1.1
ETag: "d6dde9c9361e40b96f0ce611e091c76a9559423a"
Last-Modified: Fri, 31 Oct 2014 01:42:35 GMT
X-hedtech-Media-Type: application/json
X-hedtech-totalCount: 676
X-hedtech-pageOffset: 0
X-hedtech-pageMaxSize: 10
X-Request-ID: e85ccee3-4089-4268-9d71-38d0b00f87e0
X-hedtech-message: List of thing resources
Content-Type: application/json;charset=utf-8
Transfer-Encoding: chunked
Date: Fri, 31 Oct 2014 02:24:34 GMT

[{"id":1,"version":0,"_href":"/things/1","code":"AA"....(Remaining JSON Array content not shown)
```

Success!  _Please open an issue if you find any errors in this guide. Thank you._
