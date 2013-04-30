/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

class UnsupportedMethodException extends RuntimeException {
    String pluralizedResourceName
    String disallowedMethod
    def supportedMethods = []
}