package com.mannetroll.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.web.filter.ShallowEtagHeaderFilter;

public class HttpEtagFilter extends ShallowEtagHeaderFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        HttpCacheResponseWrapper responseWrapper = new HttpCacheResponseWrapper(response);
        super.doFilterInternal(request, responseWrapper, filterChain);
    }

    private static class HttpCacheResponseWrapper extends HttpServletResponseWrapper {

        public HttpCacheResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void flushBuffer() throws IOException {
            // NOOP
        }
    }
}