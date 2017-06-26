package net.hedtech.restfulapi

/**
 * Created by sdorfmei on 6/20/17.
 */
interface RepresentationResolver {

    /**
     * Return the media type to be used to extract the request body
     * @param pluralizedResourceName
     * @param request
     * @return
     * @throws Throwable
     */
    public String getRequestRepresentationMediaType(def pluralizedResourceName,def request) throws Throwable

    /**
     * Return the media type to be used to marshal the response body
     * @param pluralizedResourceName
     * @param request
     * @return
     * @throws Throwable
     */
    public String getResponseRepresentationMediaType(def pluralizedResourceName,def request) throws Throwable

    /**
     * Get the header name to include the representation in
     * @param pluralizedResourceName
     * @param request
     * @return
     * @throws Throwable
     */
    public String getResponseHeaderName(def pluralizedResourceName,def request) throws Throwable


}