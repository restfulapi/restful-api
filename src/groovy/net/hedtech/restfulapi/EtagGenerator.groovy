/* ***************************************************************************
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/

package net.hedtech.restfulapi

import java.security.*

import static java.util.UUID.randomUUID


class EtagGenerator {

    /**
     * Returns an etag value for the specified resource model and
     * media type, automatically choosing the best available change indicators
     * for the resource.
     * Will return a SHA1 for a random UUID if no valid change indicator can be identified.
     **/
    String shaFor(resourceModel, String mediaType) {
        MessageDigest digest = MessageDigest.getInstance( 'SHA1' )
        digest.update(mediaType.getBytes('UTF-8'))
        update(digest, resourceModel)
        return "\"${new BigInteger( 1, digest.digest() ).toString( 16 ).padLeft( 40,'0' )}\""
    }

    /**
     * Returns an etag value for the specified resource model and
     * media type, automatically choosing the best available change indicators
     * for each resource in the collection.  The media type and totalCount of resources
     * is also included when computing the SHA1.
     **/
    String shaFor(Collection resourceModels, long totalCount, String mediaType) {
        MessageDigest digest = MessageDigest.getInstance('SHA1')
        def values = update(digest, resourceModels, totalCount, mediaType)

        return "\"${new BigInteger( 1, digest.digest() ).toString( 16 ).padLeft( 40,'0' )}\""
    }

    /**
     * Returns a collection of strings representing values that can be hashed for collection
     * to generate an etag.
     * resourceModels: a collection of resource models
     * totalCount: the total number of resources available
     * mediaType: the media type for the representation of resources
     **/
    protected void update(MessageDigest digest, Collection resourceModels, totalCount, String mediaType) {
        def values = []
        digest.update(mediaType.getBytes('UTF-8'))
        digest.update("${resourceModels.size()}".getBytes('UTF-8'))
        digest.update("${totalCount}".getBytes('UTF-8'))
        resourceModels.each {
            update(digest,it)
        }
    }


    /**
     * Returns an array of strings that can be hased to generate an etag for a resource.
     **/
    protected void update(MessageDigest digest, resourceModel) {
        if (resourceModel.getMetaClass().respondsTo( resourceModel, "getEtag" )) {
            log.trace "Will create ETag based upon a model's 'getEtag()' method"
            digest.update(resourceModel.getEtag().getBytes('UTF-8'))
            return
        }

        if (!hasProperty( resourceModel, "id" )) {
            log.trace "Cannot create ETag using a resource's identity, returning a UUID"
            digest.update("${randomUUID()}".getBytes('UTF-8'))
            return
        }

        digest.update("${resourceModel.id}".getBytes('UTF-8'))

        // we'll require either version, lastUpdated, or lastModified
        boolean changeIndictorFound = false
        if (hasProperty( resourceModel, "version") ) {
            changeIndictorFound = true
            digest.update("${resourceModel.version}".getBytes('UTF-8'))
        } else if (hasProperty( resourceModel, "lastUpdated" )) {
            changeIndictorFound = true
            digest.update("${resourceModel.lastUpdated}".getBytes('UTF-8'))
        } else if (hasProperty( resourceModel, "lastModified" )) {
            changeIndictorFound = true
            digest.update("${resourceModel.lastModified}".getBytes('UTF-8'))
        }

        if (changeIndictorFound) {
            log.trace "Returning an ETag based on id and a known change indicator"
            return
        } else {
            // Note: we don't return empty ETags as doing so may cause some caching
            //       infrastructure to reset connections.
            log.trace "Cannot create ETag using a resource's change indicator, returning a UUID"
            digest.update("${randomUUID()}".getBytes('UTF-8'))
            return
        }

    }


    protected boolean hasProperty( Object obj, String name ) {
        obj.getMetaClass().hasProperty(obj, "$name") && obj."$name"
    }

}
