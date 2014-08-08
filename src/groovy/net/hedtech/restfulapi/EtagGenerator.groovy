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
        def values = getValuesFor(resourceModel, mediaType)
        values.each {
            digest.update(it.getBytes('UTF-8'))
        }
        return "\"${new BigInteger( 1, digest.digest() ).toString( 16 ).padLeft( 40,'0' )}\""
    }

    /**
     * Returns an etag value for the specified resource model and
     * media type, automatically choosing the best available change indicators
     * for each resource in the collection.  The media type and totalCount of resources
     * is also included when computing the SHA1.
     **/
    String shaFor( Collection resourceModels, long totalCount, String mediaType ) {

        if (!(resourceModels && totalCount)) return ''
        MessageDigest digest = MessageDigest.getInstance('SHA1')

        def values = getValuesFor(resourceModels, totalCount, mediaType)
        values.each {
            digest.update(it.getBytes('UTF-8'))
        }

        return "\"${new BigInteger( 1, digest.digest() ).toString( 16 ).padLeft( 40,'0' )}\""
    }

    /**
     * Returns a collection of strings representing values that can be hashed for collection
     * to generate an etag.
     * resourceModels: a collection of resource models
     * totalCount: the total number of resources available
     * mediaType: the media type for the representation of resources
     **/
    String[] getValuesFor(Collection resourceModels, totalCount, String mediaType) {
        def values = []
        values.add(mediaType)
        values.add("${resourceModels.size()}")
        values.add("${totalCount}")
        resourceModels.each {
            values.addAll(getValuesFor(it))
        }
        return values

    }


    /**
     * Returns an array of strings that can be used to generate an etag for
     * the resource in the specified media type.
     **/
    String[] getValuesFor(resourceModel, String requestedMediaType) {
        def values = [requestedMediaType]
        values.addAll(getValuesFor(resourceModel))
        return values
    }

    /**
     * Returns an array of strings that can be hased to generate an etag for a resource.
     **/
    String[] getValuesFor(resourceModel) {
        def values = []
        if (resourceModel.getMetaClass().respondsTo( resourceModel, "getEtag" )) {
            log.trace "Will create ETag based upon a model's 'getEtag()' method"
            values.add(resourceModel.getEtag())
            return values
        }

        if (!hasProperty( resourceModel, "id" )) {
            log.trace "Cannot create ETag using a resource's identity, returning a UUID"
            values.add(randomUUID())
            return values
        }

        values.add(resourceModel.id)

        // we'll require either version, lastModified, or (worst case) all properties
        boolean changeIndictorFound = false
        if (hasProperty( resourceModel, "version") ) {
            changeIndictorFound = true
            values.add(resourceModel.version)
        } else if (hasProperty( resourceModel, "lastUpdated" )) {
            changeIndictorFound = true
            values.add(resourceModel.lastUpdated)
        } else if (hasProperty( resourceModel, "lastModified" )) {
            changeIndictorFound = true
            values.add(resourceModel.lastModified)
        }

        if (changeIndictorFound) {
            log.trace "Returning an ETag based on id and a known change indicator"
            return values
        } else {
            // Note: we don't return empty ETags as doing so may cause some caching
            //       infrastructure to reset connections.
            log.trace "Cannot create ETag using a resource's change indicator, returning a UUID"
            return [randomUUID() as String]
        }

    }

    protected boolean hasProperty( Object obj, String name ) {
        obj.getMetaClass().hasProperty(obj, "$name") && obj."$name"
    }

}
