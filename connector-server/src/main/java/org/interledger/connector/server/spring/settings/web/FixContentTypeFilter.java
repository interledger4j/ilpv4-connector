package org.interledger.connector.server.spring.settings.web;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Old versions of the JS connector sent content-type: application/x-url-form-encoded instead of application/octet-stream
 * This filter will intercept ILP requests with the old content-type and replace it with the correct content-type
 */
public class FixContentTypeFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    if (!request.getServletContext().getContextPath().contains("/ilp")) {
      chain.doFilter(request, response);
      return;
    }

    String originalContentType = request.getContentType();
    final String fixedContentType = needsFixin(originalContentType) ?
      APPLICATION_OCTET_STREAM_VALUE : originalContentType;
    ServletInputStream inputStream = request.getInputStream();
    ServletRequestWrapper requestWrapper = new HttpServletRequestWrapper((HttpServletRequest) request) {

      @Override
      public ServletInputStream getInputStream() {
        return inputStream;
      }

      @Override
      public String getContentType() {
        return fixedContentType;
      }

      @Override
      public String getHeader(String name) {
        if (name.toLowerCase().equals("content-type")) {
          return this.getContentType();
        }
        return super.getHeader(name);
      }

      @Override
      public Enumeration<String> getHeaders(String name) {
        if (name.toLowerCase().equals("content-type")) {
          return Iterators.asEnumeration(Lists.newArrayList(fixedContentType).iterator());
        }
        return super.getHeaders(name);
      }
    };
    chain.doFilter(requestWrapper, response);
  }

  private boolean needsFixin(String originalContentType) {
    return originalContentType != null && originalContentType.toLowerCase().startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
  }

}
