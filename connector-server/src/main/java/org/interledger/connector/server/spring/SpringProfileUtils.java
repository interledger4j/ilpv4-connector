package org.interledger.connector.server.spring;

import org.springframework.core.env.Environment;

import java.util.Arrays;

public class SpringProfileUtils {

  /**
   * Checks if a given profile name is an active profile in the environment.
   * @param environment environment containing profiles
   * @param profile profile to check
   * @return true if profile is active
   */
  public static boolean isProfileActive(Environment environment, String profile) {
    return Arrays.asList(environment.getActiveProfiles())
        .stream()
        .anyMatch(active -> active.trim().equalsIgnoreCase(profile.trim()));
  }

}
