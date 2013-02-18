/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

class ResponseHolder {
    Object data
    def headers = [:]
    def message

    void addHeader( String name, Object value ) {
        if (!headers[name]) {
            headers[name] = []
        }
        headers[name].add value?.toString()
    }
}