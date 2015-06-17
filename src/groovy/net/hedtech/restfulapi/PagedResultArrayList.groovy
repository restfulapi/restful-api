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
 * ArrayList based implementation of PagedResultList.
 **/
class PagedResultArrayList extends ArrayList implements PagedResultList {

    private long totalCount
    private Map httpResponseHeaders

    PagedResultArrayList(Collection c, long totalCount, Map httpResponseHeaders = null) {
        super(c)
        this.totalCount = totalCount
        this.httpResponseHeaders = httpResponseHeaders
    }

    long getTotalCount() {
        totalCount
    }

    Map getHttpResponseHeaders() {
        httpResponseHeaders
    }
}
