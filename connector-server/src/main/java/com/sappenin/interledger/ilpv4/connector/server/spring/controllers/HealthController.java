package com.sappenin.interledger.ilpv4.connector.server.spring.controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

/**
 * A RESTful controller for handling health-check requests.
 */
@RestController
public class HealthController {

  public static final String SLASH_AH_SLASH_HEALTH = "/_ah/health";

  /**
   * Return a 200 health check if health; otherwise a problem JSON.
   */
  @RequestMapping(
    value = SLASH_AH_SLASH_HEALTH, method = {RequestMethod.GET},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public void getHealth() {
    // Checkhealth.

    // TODO: Check Redis connection.
    // TODO: Check Settlement Engine Connection.
  }

}