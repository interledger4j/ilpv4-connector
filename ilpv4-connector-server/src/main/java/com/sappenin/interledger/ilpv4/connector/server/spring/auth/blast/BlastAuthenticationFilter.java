package com.sappenin.interledger.ilpv4.connector.server.spring.auth.blast;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TODO: Remove if unused.
 */
public class BlastAuthenticationFilter extends OncePerRequestFilter {

  public static final String EXPIRY_CLAIM = "urn:ilpv4:expiry";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {


    //    request.getHeader()
    //
    //        final DecodedJWT jwt = JWT.decode(token);
    //
    //
    //        Verification token = JWT.require(Algorithm.HMAC256(secret));


  }
}
