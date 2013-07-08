/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.marshallers

import net.hedtech.restfulapi.config.RepresentationConfig

/**
 * Interface for a service implementation that marshalls objects to
 * to string representation.
 * The resulting string will be used directly as the response body.
 **/
interface MarshallingService {

    /**
     * Marshalls the object to a string representation.
     * @param o the object to marshal
     * @param config the RepresentationConfig representing the
     *        representation the object is to be marshalled to.
     **/
    String marshalObject(Object o,RepresentationConfig config)
}