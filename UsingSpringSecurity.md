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

#Securing RESTful APIs using Basic Authentication

The restful-api plugin does not itself address security requirements. This document is provided to show how the Spring Security Core Plugin may be used to protect RESTful API endpoints using Basic Authentication.

###Install Spring Security Core Plugin

First, install the [Spring Security Core Plugin](http://grails.org/plugin/spring-security-core) by editing grails-app/conf/BuildConfig as described at [http://grails.org/plugin/spring-security-core]()

Next, we'll perform steps that are similar to those in [chapter 23](http://grails-plugins.github.io/grails-spring-security-core/docs/manual/guide/23%20Tutorials.html) of the Spring Security Core Plugin reference documentation (which is a tutorial).

We'll, however, assume we already have an existing application for which the restful-api plugin is being used to expose RESTful endpoints.

So, we will start out by creating User and Role classes by using the s2-quickstart script (as described in step 3 of the above-mentioned tutorial).

```bash
grails s2-quickstart {your-package} User Role
```

Next we'll modify BootStrap.groovy to instantiate a user and a role during initializaiton (similarly to step 7 of the tutorial). We'll create a single 'ROLE_API_USER' role instead of the two roles created in the tutorial.

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

The Spring Security Core Plugin tutorial proceeds to create a Controller secured using a '@Secured' annotation.  We'll assume you'd rather not modify the RestfulAPiController controller (as it's in the restful-api plugin), and instead we'll configure an interceptUrlMap.

Please add the following to your Config.groovy file:

```groovy
import grails.plugins.springsecurity.SecurityConfigType
grails.plugins.springsecurity.securityConfigType = SecurityConfigType.InterceptUrlMap
grails.plugins.springsecurity.rejectIfNoRule = true

grails.plugins.springsecurity.interceptUrlMap = [
    '/api/**':    ['ROLE_API_USER'],
]
```

####Enable Basic Authentication

Add the following to Config.groovy

```groovy
grails.plugins.springsecurity.useBasicAuth = true
grails.plugins.springsecurity.basic.realmName = "HTTP Basic Auth Demo"
```

We have now protected the API endpoint, so if we hit an API without providing correct credentials we will get a '401 Unauthorized' response.

```html
$ curl -i --noproxy localhost -H "Accept: application/json" http://localhost:8080/restfulapi-tutorial/api/foos?max=10
HTTP/1.1 401 Unauthorized
Server: Apache-Coyote/1.1
Set-Cookie: JSESSIONID=EE7CBBD50B45147546C048B901C61CF2; Path=/restfulapi-tutorial/; HttpOnly
WWW-Authenticate: Basic realm="Example RESTful API Realm"
Content-Type: text/html;charset=utf-8
Content-Length: 1061
Date: Thu, 26 Sep 2013 22:37:34 GMT

(content not shown)
```

Unfortunately, the above Content-Type is 'text/html' and the login page markup was returned. This isn't what we want, so we'll fix that below.

####Create a Basic Authentication Entry Point

We really want a basic authentication entry point that can support content negotiation, so that if JSON is requested we respond with a Content-Type of application/json, and if XML is requested we respond with a Content-Type of application/xml.

To do this, create a src/groovy/net/hedtech/api/security/RestApiAuthenticationEntryPoint.groovy file with the following content:

```groovy
package net.hedtech.api.security

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
import net.hedtech.api.security.RestApiAuthenticationEntryPoint

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
$ curl -i --noproxy localhost -H "Accept: application/json" http://localhost:8080/restfulapi-tutorial/api/foos?max=10
HTTP/1.1 401 Unauthorized
Server: Apache-Coyote/1.1
Set-Cookie: JSESSIONID=83E8F414B0CE2968180E8A4438663F15; Path=/restfulapi-tutorial/; HttpOnly
WWW-Authenticate: Basic realm="Example RESTful API Realm"
Content-Type: application/json;charset=ISO-8859-1
Content-Length: 36
Date: Thu, 26 Sep 2013 22:58:33 GMT

{ "errors" : [ { "code":"401" } ] }
```

Much better :-)
