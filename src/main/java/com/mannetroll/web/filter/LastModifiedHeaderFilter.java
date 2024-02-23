package com.mannetroll.web.filter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author drtobbe
 */
public class LastModifiedHeaderFilter extends OncePerRequestFilter {
    private static final String GMT = "GMT";
    private static final String HEADER_LAST_MODIFIED = "Last-Modified";
    private static SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    static {
        format.setTimeZone(TimeZone.getTimeZone(GMT));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
        response.setHeader(HEADER_LAST_MODIFIED, format.format(new Date()));
    }

}