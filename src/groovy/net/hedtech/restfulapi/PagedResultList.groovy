/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

/**
 * Interface for paged result lists that can be returned by
 * the list method of services.
 * The controller will obtain the total count from the paged result
 * instance instead of invoking the count method on the service.
 **/
interface PagedResultList extends List {

    long getTotalCount()

}