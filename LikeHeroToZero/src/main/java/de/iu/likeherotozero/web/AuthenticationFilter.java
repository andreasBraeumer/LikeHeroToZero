package de.iu.likeherotozero.web;

import de.iu.likeherotozero.controller.UserSession;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/backend/*")
public class AuthenticationFilter implements Filter {

    private static final String AJAX_REQUEST_HEADER_VALUE = "partial/ajax";

    @Inject
    private UserSession userSession;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        boolean sessionExists = request.getSession(false) != null;
        if (sessionExists && userSession.isLoggedIn()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String loginUrl = request.getContextPath() + "/login.xhtml";
        boolean isAjaxRequest = AJAX_REQUEST_HEADER_VALUE.equals(request.getHeader("Faces-Request"));
        if (isAjaxRequest) {
            response.setContentType("text/xml");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().printf(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?><partial-response><redirect url=\"%s\"/></partial-response>",
                    loginUrl);
        } else {
            response.sendRedirect(loginUrl);
        }
    }
}
