/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.marshallers

import net.hedtech.restfulapi.config.RepresentationConfig

/**
 * A wrapper for InputStreams that can convey the total number
 * of bytes that will be returned by the stream.
 * Can be returned by a marshalling service in order to set
 * the Content-Length header.
 **/
class StreamWrapper {

    InputStream stream
    int totalSize
}