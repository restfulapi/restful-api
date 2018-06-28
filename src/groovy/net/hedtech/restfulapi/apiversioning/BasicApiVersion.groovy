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

import net.hedtech.restfulapi.ApiVersion

public class BasicApiVersion implements ApiVersion {

    // ==========================================================================
    // Attributes
    // ==========================================================================

    private String resourceName

    private int majorVersion
    private int minorVersion
    private int patchVersion

    private String schema

    private String mediaType

    private String version

    // ==========================================================================
    // Constructors
    // ==========================================================================

    public BasicApiVersion(String resourceName, int majorVersion, int minorVersion, int patchVersion, String schema) {
        this(resourceName, majorVersion, minorVersion, patchVersion, schema, null)
    }

    public BasicApiVersion(String resourceName, int majorVersion, int minorVersion, int patchVersion, String schema, String mediaType) {
        this.resourceName = resourceName
        this.majorVersion = majorVersion
        this.minorVersion = minorVersion
        this.patchVersion = patchVersion
        this.schema = schema
        this.mediaType = mediaType
        if (this.majorVersion == -1) {
            this.version = null
        } else if (this.majorVersion != -1 && (this.minorVersion == -1 || this.patchVersion == -1)) {
            this.version = "v${this.majorVersion}"
        } else {
            this.version = "v${this.majorVersion}.${this.minorVersion}.${this.patchVersion}"
        }
    }

    // ==========================================================================
    // Methods
    // ==========================================================================

    public String getResourceName() {
        return resourceName
    }

    public String getVersion() {
        return version
    }

    public String getSchema() {
        return schema
    }

    public String getMediaType() {
        return mediaType
    }

    @Override
    public String toString() {
        "BasicApiVersion[resourceName=$resourceName, version=$version, majorVersion=$majorVersion, minorVersion=$minorVersion, patchVersion=$patchVersion, schema=$schema, mediaType=$mediaType]"
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true
        if (!(object instanceof BasicApiVersion)) return false

        BasicApiVersion that = (BasicApiVersion) object

        if (this.resourceName != that.resourceName) return false
        if (this.majorVersion != that.majorVersion) return false
        if (this.minorVersion != that.minorVersion) return false
        if (this.patchVersion != that.patchVersion) return false
        if (this.schema != that.schema) return false
        return true
    }

    @Override
    public int hashCode() {
        int result = (resourceName != null ? resourceName.hashCode() : 0)
        result = 31 * result + (majorVersion != null ? majorVersion.hashCode() : 0)
        result = 31 * result + (minorVersion != null ? minorVersion.hashCode() : 0)
        result = 31 * result + (patchVersion != null ? patchVersion.hashCode() : 0)
        result = 31 * result + (schema != null ? schema.hashCode() : 0)
        return result
    }

    @Override
    public int compareTo(Object object) {
        BasicApiVersion that = (BasicApiVersion) object

        // resource name wins
        int compare = this.resourceName <=> that.resourceName
        if (compare != 0) {
            return compare
        }

        // then major version
        compare = Integer.compare(this.majorVersion, that.majorVersion)
        if (compare != 0) {
            return compare
        }

        // then minor version
        compare = Integer.compare(this.minorVersion, that.minorVersion)
        if (compare != 0) {
            return compare
        }

        // then patch version
        compare = Integer.compare(this.patchVersion, that.patchVersion)
        if (compare != 0) {
            return compare
        }

        // finally compare the schema length (shorter length schema wins - null is longest length)
        int thisSchemaLength = (this.schema != null ? this.schema.length() : Integer.MAX_VALUE)
        int thatSchemaLength = (that.schema != null ? that.schema.length() : Integer.MAX_VALUE)
        compare = Integer.compare(thatSchemaLength, thisSchemaLength)
        return compare
    }

}
