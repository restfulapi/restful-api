/* ***************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

class RepresentationConfig {

    String mediaType
    String marshallerFramework
    String contentType
    def marshallers = []
    def extractor


    /**
     * Returns the marshalling framework for the representation.
     * If not explicitly specified, will attempt to determine
     * it based on the mediatype.
     **/
    public String resolveMarshallerFramework() {
        if ('none' == marshallerFramework) {
            return null
        }

        if (null != marshallerFramework) {
            return marshallerFramework
        }

        switch(mediaType) {
            case ~/.*json$/:
                return 'json'
                break
            case ~/.*xml$/:
                return 'xml'
                break
            default:
                return null
                break
        }
    }
}
