/* ***************************************************************************
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

package net.hedtech.restfulapi

/**
 * Defines the methods resources managed by the restful api controller
 * understands.
 **/
class Methods {
    static final String LIST   = 'list'
    static final String SHOW   = 'show'
    static final String CREATE = 'create'
    static final String UPDATE = 'update'
    static final String DELETE = 'delete'

    private static final allMethods = [LIST,SHOW,CREATE,UPDATE,DELETE]

    private static methodGroups = [:]

    private static httpMap = [
        (Methods.LIST):'GET',
        (Methods.SHOW):'GET',
        (Methods.CREATE):'POST',
        (Methods.UPDATE):'PUT',
        (Methods.DELETE):'DELETE'
    ]

    static {
        //In order to produce the Allow header if returning a 405 response, we
        //need to know what other methods may be allowed for the same url.
        //Per the strategy, all the urls fall into two patterns: those
        //with an odd number of parts after the api prefix, which
        //are methods without an ID, i.e. list and save, and those
        //with an even number which have id, which are show, update, and
        //delete.  This holds true even for nested resources, as nesting
        //always adds two more parts to the url.
        //So, for example, if we are rejecting a show request, then we know that it
        //it must be for a url that is 'even' and therefore can, at most,
        //support show, update, and delete.  We can therefore group all methods into
        //disjoint sets: those that service urls with an odd number of parts
        //after the api prefix, and those with and even number.
        def odd = [LIST,CREATE]
        def even =[SHOW,UPDATE,DELETE]
        odd.each { methodGroups.put( it, odd ) }
        even.each { methodGroups.put( it, even ) }
    }

    static def getAllMethods() {
        return allMethods
    }

    /**
     * Methods can be grouped according to whether the are mapped to the same
     * url pattern.  That is, 'list' and 'save' always operate against the same
     * url pattern: one that ends with a pluralized resource name, but no id.
     **/
    static def getMethodGroup( String method ) {
        return methodGroups.get( method )
    }

    static def getHttpMethod( String method ) {
        return httpMap.get( method )
    }

}
