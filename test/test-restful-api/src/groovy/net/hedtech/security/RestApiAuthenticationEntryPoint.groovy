package net.hedtech.security

import java.io.IOException
import java.io.PrintWriter

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import net.hedtech.restfulapi.MediaType
import net.hedtech.restfulapi.MediaTypeParser

import org.springframework.http.HttpHeaders
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint


public class RestApiAuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

    MediaTypeParser  mediaTypeParser = new MediaTypeParser()

    @Override
    public void commence( HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException)
                throws IOException, ServletException {

        String contentType
        String content
        MediaType[] acceptedTypes = mediaTypeParser.parse(request.getHeader(HttpHeaders.ACCEPT))
        def type = acceptedTypes.size() > 0 ? acceptedTypes[0].name : ""

        switch(type) {
            case ~/.*xml.*/:
                contentType = 'application/xml'
                content = "<Errors><Error><Code>${HttpServletResponse.SC_UNAUTHORIZED}</Code></Error></Errors>"
                break
            case ~/.*json.*/:
                contentType = 'application/json'
                content = "{ \"errors\" : [ { \"code\":\"${HttpServletResponse.SC_UNAUTHORIZED}\" } ] }"
                break
            default:
                contentType = 'plain/text'
                content = HttpServletResponse.SC_UNAUTHORIZED + " - " + authException.getMessage()
                break
        }

        response.addHeader("Content-Type", contentType )
        response.addHeader("WWW-Authenticate", "Basic realm=\"" + getRealmName() + "\"")
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
        PrintWriter writer = response.getWriter()
        writer.println(content)
    }
}
