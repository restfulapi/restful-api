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

import net.hedtech.restfulapi.adapters.NothingServiceAdapter

//import net.hedtech.security.RestApiAuthenticationEntryPoint

//import org.springframework.security.web.access.ExceptionTranslationFilter
//import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

beans = {
    nothingServiceAdapter(NothingServiceAdapter)

//   restApiAuthenticationEntryPoint(RestApiAuthenticationEntryPoint) {
//       realmName = 'Example RESTful API Realm'
//   }
//
//   basicAuthenticationFilter(BasicAuthenticationFilter) {
//       authenticationManager = ref('authenticationManager')
//       authenticationEntryPoint = ref('restApiAuthenticationEntryPoint')
//   }
//
//   basicExceptionTranslationFilter(ExceptionTranslationFilter) {
//       authenticationEntryPoint = ref('restApiAuthenticationEntryPoint')
//       accessDeniedHandler = ref('accessDeniedHandler')
//   }
}
