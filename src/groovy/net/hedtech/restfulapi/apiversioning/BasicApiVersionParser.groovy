/* ***************************************************************************
 * Copyright 2018 Ellucian Company L.P. and its affiliates.
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

package net.hedtech.restfulapi.apiversioning

import net.hedtech.restfulapi.ApiVersionParser

public class BasicApiVersionParser implements ApiVersionParser {

    // ==========================================================================
    // Methods
    // ==========================================================================

    /**
     * Returns the specified API version from the given resource and media type.
     *
     * RESTful APIs use custom media types (previously known as 'MIME types') for versioning.
     *
     * APIs that have implemented versioning will have Accept headers like application/vnd.hedtech.v1+json,
     * application/vnd.hedtech.v2+json so on.
     *
     * Accept header can also contain generic media types like application/vnd.hedtech+json or application/json
     * that represent latest (current) version of the API. In such cases, this method does not return anything.
     *
     * Semantic versioning of resources is also supported. When using semantic versioning, the version will consist
     * of a 3-tuple of digits representing major, minor, and patch versions numbers. See https://semver.org for a
     * specification of semantic versioning. Backward compatibility with non-semantic versioning is also supported.
     *
     * If the media type is for extended version (deep marshalling) like application/vnd.hedtech.maximum.v1+json,
     * the API has to return response with some of the associations expanded as specified by maximum schema. The
     * schema is included in the returned API version.
     */
    public BasicApiVersion parseMediaType(String resourceName, String mediaType) {
        // parse the string representation of version from the media type
        String versionString = null
        int indexOfFirstSlash = mediaType.lastIndexOf("/")
        int indexOfDotBeforeVersion = mediaType.toLowerCase().lastIndexOf(".v")
        int indexOfPlus = mediaType.lastIndexOf("+")
        if (indexOfDotBeforeVersion != -1 && indexOfPlus != -1 && indexOfDotBeforeVersion + 1 < indexOfPlus) {
            versionString = mediaType.substring(indexOfDotBeforeVersion + 1, indexOfPlus)
            if (!versionString?.toLowerCase()?.startsWith("v")) {
                // May be generic Accept header like "application/vnd.hedtech.integration+json"
                versionString = null
            }
        }

        // parse the schema from the media type
        String schema = null
        if (indexOfFirstSlash != -1 && indexOfDotBeforeVersion != -1) {
            schema = mediaType.substring(indexOfFirstSlash + 1, indexOfDotBeforeVersion)
        } else if (indexOfFirstSlash != -1 && indexOfPlus != -1) {
            schema = mediaType.substring(indexOfFirstSlash + 1, indexOfPlus)
        }

        // version string is expected to start with a lowercase v character
        if (!versionString || !versionString.startsWith("v")) {
            return buildMissingVersion(resourceName, schema, mediaType)
        }

        // remove leading v character for remaining parse
        String version = versionString.substring(1)

        // handle non-semantic versioning (without decimal places) for non-semantic versioning
        if (version.indexOf(".") == -1) {
            return parseLegacyVersion(resourceName, version, schema, mediaType)
        }

        // build semantic version
        return parseSemanticVersion(resourceName, version, schema, mediaType)
    }

    private BasicApiVersion buildMissingVersion(String resourceName, String schema, String mediaType) {
        return new BasicApiVersion(resourceName, -1, -1, -1, schema, mediaType)
    }

    private BasicApiVersion parseLegacyVersion(String resourceName, String version, String schema, String mediaType) {
        try {
            // set minor and patch versions to -1 to differentiate with semantic version
            return new BasicApiVersion(resourceName, version.toInteger(), -1, -1, schema, mediaType)
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Wrong version format - expected VERSION - got ${version}")
        }
    }

    private BasicApiVersion parseSemanticVersion(String resourceName, String version, String schema, String mediaType) {
        // semantic version expects 3 version numbers (major, minor, and patch)
        def versionParts = version.tokenize('.')
        if (versionParts.size != 3) {
            throw new IllegalArgumentException("Wrong semantic version format - expected MAJOR.MINOR.PATCH - got ${version}")
        }

        try {
            // convert version numbers to an integer for comparisons
            int majorVersion = versionParts[0].toInteger()
            int minorVersion = versionParts[1].toInteger()
            int patchVersion = versionParts[2].toInteger()

            // validate that we have positive integers for semantic version
            if (majorVersion < 0 || minorVersion < 0 || patchVersion < 0) {
                throw new IllegalArgumentException("Wrong version format - expected positive values for MAJOR.MINOR.PATCH - got ${version}")
            }

            // return semantic version
            return new BasicApiVersion(resourceName, majorVersion, minorVersion, patchVersion, schema, mediaType)
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Wrong version format - expected MAJOR.MINOR.PATCH - got ${version}")
        }
    }

}
