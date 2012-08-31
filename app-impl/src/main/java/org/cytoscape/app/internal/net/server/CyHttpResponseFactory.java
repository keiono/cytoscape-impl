package org.cytoscape.app.internal.net.server;

/**
 * Creates response objects. Implementations of {@link CyHttpResponder},
 * {@link CyHttpBeforeResponse}, and {@link CyHttpAfterResponse}
 * will use an this interface to create response objects
 * as its return values.
 */
public interface CyHttpResponseFactory
{
    /**
     * Create a response with no content.
     * @param statusCode the http response status code; use a
     * static field in {@link java.net.HttpURLConnection} to fill this in.
     */
    CyHttpResponse createHttpResponse(int statusCode);

    /**
     * Create a response with content.
     * @param statusCode the http response status code; use a
     * static field in {@link java.net.HttpURLConnection} to fill this in.
     * @param content the text content to send back to the client
     * @param contentType the MIME type of {@code content}; examples of valid
     * MIME types: {@code text/html}, {@code application/json}, {@code application/xml}.
     */
    CyHttpResponse createHttpResponse(int statusCode, String content, String contentType);

    /**
     * Create a response with content with 200 OK.
     * @param content the text content to send back to the client
     * @param contentType the MIME type of {@code content}; examples of valid
     * MIME types: {@code text/html}, {@code application/json}, {@code application/xml}.
     */
    CyHttpResponse createHttpResponse(String content, String contentType);
}