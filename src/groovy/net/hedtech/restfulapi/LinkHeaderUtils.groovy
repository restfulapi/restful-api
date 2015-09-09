/* ***************************************************************************
 * Copyright 2015 Ellucian Company L.P. and its affiliates.
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

class LinkHeaderUtils {

    static
    public String generate( String resourceName, int offset,
                            int limit, long totalCount ) {

        def links = [];
        def nextOffset = offset + limit;
        def previousOffset = offset - limit > 0 ? offset - limit : 0;
        def firstOffset = 0;
        def lastOffset = getLastOffset( offset, limit, totalCount );
        if (previousOffset > 0) {
            links << createLinkHeader(resourceName, firstOffset, limit, 'first');
            links << createLinkHeader(resourceName, previousOffset, limit, 'prev');
        }
        else if (previousOffset == firstOffset && offset != firstOffset) {
            links << createLinkHeader(resourceName, firstOffset, limit, 'first prev');
        }
        if (offset < lastOffset) {
            if (nextOffset == lastOffset) {
                links << createLinkHeader(resourceName, lastOffset, limit, 'next last');
            }
            else {
                links << createLinkHeader(resourceName, lastOffset, limit, 'last');
                if (nextOffset < totalCount) {
                    links << createLinkHeader(resourceName, nextOffset, limit, 'next');
                }
            }
        }
        String linkHeader = links.join(", \n    ");
        linkHeader;
    }

    static
    public Map parse( String linkHeader ) {
        def relUriMap = [:]
        String[] links = linkHeader.split(',');
        links.each { link ->
            def parts = link.split(';');
            if (parts.length != 2) {
              throw new RuntimeException("link header part could not be split on ';' - $link");
            }
            def uri = ''

            parts[0].replaceAll( /<(.*)>/ ) { all, uriText ->
                uri = uriText
            }
            parts[1].replaceAll( /rel="(.*)"/ ) { all, relText ->
                def refs = relText.split(' ')
                refs.each() { rel ->
                    relUriMap[rel] = uri;
                }
            }
        }
        relUriMap
    }

    static
    private int getLastOffset( final int offset, final int limit, final long totalCount ) {
       def remaining = totalCount - offset;
       def pagesRemaining = Math.floor(remaining / (double) limit);
       offset + (pagesRemaining * limit);
    }

    static
    private String createLinkHeader(final String resourceName, final int offset,
                                    final int limit, final String rel) {

        "</" + resourceName + "?offset=" + offset + "&limit=" + limit + ">; rel=\"" + rel + "\"";
    }
}
