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


    def list(def service, Map params) throws Throwable

    def count(def service, Map params) throws Throwable

    def show(def service, Map params) throws Throwable

    def create(def service, Map content, Map params) throws Throwable

    def update(def service, def id, Map content, Map params) throws Throwable

    void delete(def service, def id, Map content, Map params) throws Throwable

}