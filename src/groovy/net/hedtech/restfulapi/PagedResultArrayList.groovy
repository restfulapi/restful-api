/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

/**
 * ArrayList based implementation of PagedResultList.
 **/
class PagedResultArrayList extends ArrayList implements PagedResultList {

    private long totalCount

    PagedResultArrayList(Collection c, long totalCount) {
        super(c)
        this.totalCount = totalCount
    }

    long getTotalCount() {
        totalCount
    }
}