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

#Securing RESTful APIs using a Token

The restful-api plugin does not itself address security requirements, nor does it mandate any particular approach or library (such as Spring Security).

This document is provided to show how the [Spring Security REST](https://github.com/alvarosanchez/grails-spring-security-rest) plugin v1.4 can be used to protect API endpoints using a token-based authentication approach.

A token-based approach, as discussed here, is preferrable to the cookie based approach discussed in '[docs/Using_Cookie_Based_Authentication.md](https://github.com/restfulapi/restful-api/blob/master/docs/Using_Cookie_Based_Authentication.md)'.

We will use that document ('[docs/Using_Cookie_Based_Authentication.md](https://github.com/restfulapi/restful-api/blob/master/docs/Using_Cookie_Based_Authentication.md)') as a starting point though, and then we'll continue with this document to complete the token-based implementation.

##Install and Configure the Grails Spring Security REST Plugin

###Follow the 'Using Cookie Based Authentication' Guide

Before we implement our token-based authentication, we need to install and configure the Spring Security Core plugin.  Please follow the steps in [docs/Using_Cookie_Based_Authentication.md](https://github.com/restfulapi/restful-api/blob/master/docs/Using_Cookie_Based_Authentication.md) and when that is functional continue with the steps below.

###Install and Configure the Spring Security REST Plugin

The Spring Security REST plugin [documentation](http://alvarosanchez.github.io/grails-spring-security-rest/docs/guide/introduction.html) is well written and should be read.

Add to the 'repositories' block in BuildConfig.groovy:

```
mavenRepo 'http://repo.spring.io/milestone'
```

You should already have this line in the 'plugins' block:

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

Next we'll configure GORM as our storage for our tokens.  We'll identify a token domain class, using the fully qualified name.  We'll implement that class in the next step.

```
grails.plugin.springsecurity.rest.token.storage.useGorm = true
grails.plugin.springsecurity.rest.token.storage.gorm.tokenDomainClassName = 'your.package.name.AuthenticationToken'
grails.plugin.springsecurity.rest.token.storage.gorm.tokenValuePropertyName = 'tokenValue'
grails.plugin.springsecurity.rest.token.storage.gorm.usernamePropertyName = 'username'
```

Now we'll implement the AuthenticationToken domain class configured above.  We'll create this under 'grails-app/domain' (using the approprate directory structure for your package):

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

To authenticate, we'll ask for a token by POSTing to the api/login URL.  Making sure to use HTTPS, we'll pass our credentials. We'll pipe this into node so we can parse the response and extract the token, which we'll capture in a 'token' file for use in subsequent invocations.

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

Success!
