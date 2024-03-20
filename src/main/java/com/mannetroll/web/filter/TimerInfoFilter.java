package com.mannetroll.web.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.mannetroll.metrics.statistics.AbstractTimerInfoStats;
import com.mannetroll.metrics.statistics.TimerInfoStats;
import com.mannetroll.metrics.util.LogKeys;

/**
 * @author mannetroll
 */
public class TimerInfoFilter implements Filter {
	private static final Logger logger = LogManager.getLogger(TimerInfoFilter.class);
	private static final AbstractTimerInfoStats statistics = TimerInfoStats.getInstance("kpis");

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws ServletException, IOException {
		//
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		long timestamp = System.currentTimeMillis();
		StatusHttpServletResponseWrapper responseWrapper = new StatusHttpServletResponseWrapper(response);
		try {
			// Do Filter Chain
			filterChain.doFilter(servletRequest, responseWrapper);
		} catch (ServletException e) {
			String msg = "X-ServletException: ";
			Throwable rootCause = e.getRootCause();
			if (rootCause != null) {
				msg += rootCause.getClass().getName() + ": ";
			}
			msg += getUriKey(request.getRequestURI());
			statistics.addCall(msg, System.currentTimeMillis() - timestamp, System.currentTimeMillis());
			throw e;
		} catch (IOException e) {
			String msg = "X-IOException: " + getUriKey(request.getRequestURI());
			statistics.addCall(msg, System.currentTimeMillis() - timestamp, System.currentTimeMillis());
			throw e;
		} catch (RuntimeException e) {
			String msg = "X-" + e.getClass().getName() + ": " + getUriKey(request.getRequestURI());
			statistics.addCall(msg, System.currentTimeMillis() - timestamp, System.currentTimeMillis());
			throw e;
		}
		long filterTime = System.currentTimeMillis() - timestamp;
		String key = responseWrapper.getStatus() + getUriKey(request.getRequestURI());
		int bytes = responseWrapper.getLength();

		long now = System.currentTimeMillis();
		statistics.addCall(key, filterTime, bytes, now);
		statistics.addTotalTime(filterTime, bytes, now);
		statistics.addStatusCode(key);
		if (logger.isDebugEnabled()) {
			logger.log(Level.DEBUG, "key: " + key);
		}
	}

	private String getUriKey(String uriKey) {
		int len = 60;
		String metrics = ThreadContext.get(LogKeys.METRICS_NAME);
		if (metrics != null) {
			return metrics;
		} else if (uriKey != null && uriKey.length() > len) {
			return uriKey.substring(0, len) + "..." + uriKey.length();
		}
		return uriKey;
	}

	public void init(FilterConfig filterConfig) throws ServletException {
	}

	public void destroy() {
	}

	private static class StatusHttpServletResponseWrapper extends HttpServletResponseWrapper {
		private int status;
		private int length;

		public StatusHttpServletResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void setStatus(int sc) {
			super.setStatus(sc);
			this.status = sc;
		}

		@SuppressWarnings("deprecation")
		@Override
		public void setStatus(int sc, String sm) {
			super.setStatus(sc, sm);
			this.status = sc;
		}

		public int getStatus() {
			return status;
		}

		@Override
		public void setContentLength(int len) {
			super.setContentLength(len);
			this.length = len;
		}

		public int getLength() {
			return length;
		}
	}

}