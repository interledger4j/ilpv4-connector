package org.interledger.connector.server.spring.settings.web;

import static org.interledger.connector.server.wallet.controllers.SpspController.APPLICATION_SPSP_JSON_VALUE;
import static org.interledger.spsp.client.SpspClient.APPLICATION_SPSP4_JSON_VALUE;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.util.UrlPathHelper;

import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

/**
 * A {@link RequestMatcher} that allows Spring Security to match on the presence of a header in an HTTP request. Used
 * for SPSP. Requests match if the Accept header is the proper type, and the the URL path is correct.
 */
public class SpspRequestMatcher implements RequestMatcher {

  private final boolean spspEnabled;
  private final UrlPathHelper urlPathHelper;
  private final String spspUrlPath;

  public SpspRequestMatcher(final boolean spspEnabled, final String initialSpspUrlPath) {
    this.spspEnabled = spspEnabled;

    // Trim off any leading or trailing whitespace; leading slash; no trailing slash
    String spspUrlPath = initialSpspUrlPath.trim();
    if (StringUtils.isNotBlank(spspUrlPath) && !spspUrlPath.startsWith("/")) {
      spspUrlPath = "/" + initialSpspUrlPath;
    }
    if (StringUtils.isNotBlank(spspUrlPath) && spspUrlPath.endsWith("/")) {
      spspUrlPath = initialSpspUrlPath.substring(0, spspUrlPath.length() - 1);
    }

    this.spspUrlPath = Objects.requireNonNull(spspUrlPath);
    this.urlPathHelper = new UrlPathHelper();
  }

  @Override
  public boolean matches(final HttpServletRequest httpServletRequest) {
    //Define the matching logic here....

    // NOTE: SPSP Requests currently only support SPSP headers. If the wallet supports profile pages at some point,
    // then this check could be expanded to include `text/html`, but care should be take to not have usernames
    // collide with the admin API.

    return spspEnabled && hasCorrectSpspUrlPath(httpServletRequest) && hasCorrectAcceptHeader(httpServletRequest);
  }

  private boolean hasCorrectAcceptHeader(final HttpServletRequest httpServletRequest) {
    return Optional.ofNullable(httpServletRequest.getHeader("Accept"))
      .filter(StringUtils::isNotBlank)
      .filter(
        acceptHeader -> StringUtils.contains(acceptHeader, APPLICATION_SPSP4_JSON_VALUE)
          || StringUtils.contains(acceptHeader, APPLICATION_SPSP_JSON_VALUE)
      )
      .map(acceptHeader -> true) // If anything makes it past the filters
      .orElse(false); // empty/blank/null string.
  }

  private boolean hasCorrectSpspUrlPath(final HttpServletRequest httpServletRequest) {
    if (StringUtils.isNotBlank(spspUrlPath)) {
      final String urlPath = urlPathHelper.getPathWithinApplication(httpServletRequest);
      return Optional.ofNullable(urlPath)
        .filter(StringUtils::isNotBlank)
        .filter(path -> path.startsWith(spspUrlPath))
        .map(path -> true)
        .orElse(false);

    } else {
      // Here there's no path to filter on, so just assume we match all paths here.
      return true;
    }

  }
}
