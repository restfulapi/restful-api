/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.spock

import org.springframework.http.ResponseEntity

class RestResponse {

    @Delegate ResponseEntity responseEntity

    @Lazy String text = {
        def body = responseEntity.body
        if (body) {
            return body.toString()
        }

        responseEntity.statusCode.reasonPhrase
    }()

    @Lazy String contentType = {
        responseEntity?.headers?.getContentType().toString().split(';')[0]
    }()


    int getStatus() {
        responseEntity?.statusCode?.value() ?: 200
    }


}