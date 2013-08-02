/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi


/**
 * An service adapter implementation supporting Ellucian's Banner XE services.
 * These services extend 'ServiceBase' (from the internal 'banner-core' plugin).
 * This adapter is provided as a convenience to support Banner XE, however
 * NO DEPENDENCIES have been introduced within this plugin to either Banner XE
 * or the 'banner-core' plugin.
 * Specifically, this adapter supports a slightly different contract for
 * 'update' and 'delete' methods, as ServiceBase does not support passing
 * the 'id' separate from the 'content' map.
 **/
class RestfulServiceBaseAdapter implements RestfulServiceAdapter {


    def list(def service, Map params) {
        service.list(params)
    }

    def count(def service, Map params) {
        if (service.metaClass.respondsTo(service, "count", Map)) {
            service.count(params)
        } else {
            service.count()
        }
    }

    def show(def service, Map params) {
        service.show(params)
    }

    def create(def service, Map content, Map params) {
        service.create(content)
    }

    // adapts update() since ServiceBase expects only a Map
    def update(def service, def id, Map content, Map params) {
        if (!content.id) content.id = id
        service.update(content)
    }

    // adapts delete() since ServiceBase expects only a Map
    void delete(def service, def id, Map content, Map params) {
        if (!content.id) content.id = id
        service.delete(content)
    }

}
