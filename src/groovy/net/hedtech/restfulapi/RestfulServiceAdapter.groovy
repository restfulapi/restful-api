/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi


/**
 * An interface for services and or adapters.
 * This interface is admittedly very loose, as it uses duck typing ('def').
 * Please see README.md for a full explanation.
 **/
interface RestfulServiceAdapter {


    def list(def service, Map params)

    def count(def service)

    def show(def service, Map params)

    def create(def service, Map content)

    def update(def service, def id, Map content)

    void delete(def service, def id, Map content)

}