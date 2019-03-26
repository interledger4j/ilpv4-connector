package com.sappenin.interledger.ilpv4.connector.server.spring.settings.blast;

import com.auth0.spring.security.api.JwtWebSecurityConfigurer;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.server.spring.auth.blast.BlastAuthenticationProvider;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.IlpHttpController;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport;

import java.util.Arrays;
import java.util.function.Supplier;

import static org.interledger.connector.link.blast.BlastHeaders.BLAST_AUDIENCE;

@Configuration
@EnableWebSecurity
@Import(SecurityProblemSupport.class)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Autowired
  Supplier<ConnectorSettings> connectorSettingsSupplier;

  @Autowired
  AccountManager accountManager;

  @Autowired
  private SecurityProblemSupport problemSupport;

  @Bean
  BlastAuthenticationProvider blastAuthenticationProvider() {
    return new BlastAuthenticationProvider(connectorSettingsSupplier, accountManager);
  }

  /**
   * Required for auto-injection of {@link org.springframework.security.core.Authentication} into controllers.
   *
   * @see "https://github.com/spring-projects/spring-security/issues/4011"
   */
  @Bean
  public SecurityContextHolderAwareRequestFilter securityContextHolderAwareRequestFilter() {
    return new SecurityContextHolderAwareRequestFilter();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    //configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
    configuration.setAllowedMethods(Arrays.asList("POST"));
    configuration.setAllowCredentials(true);
    configuration.addAllowedHeader("Authorization");
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Override
  public void configure(final HttpSecurity http) throws Exception {

    // Must come first in order to register properly due to 'denyAll' directive below.
    JwtWebSecurityConfigurer
      .forHS256(
        BLAST_AUDIENCE,
        connectorSettingsSupplier.get().getJwtTokenIssuer().toString(),
        blastAuthenticationProvider()
      )
      .configure(http)
      .authorizeRequests()
      .antMatchers(HttpMethod.HEAD, IlpHttpController.ILP_PATH).authenticated()
      .antMatchers(HttpMethod.POST, IlpHttpController.ILP_PATH).authenticated()
      .anyRequest().permitAll();
      //.antMatchers(HttpMethod.POST, IlpHttpController.ILP_PATH).hasAuthority("read:messages");
      //.antMatchers(HttpMethod.GET, "/actuator/**").permitAll()
      //.antMatchers(HttpMethod.POST, "/actuator/**").permitAll()
      //.anyRequest().denyAll();

    http
      .addFilter(securityContextHolderAwareRequestFilter())
      //.cors()
      //.and()
      .httpBasic().disable()
      .formLogin().disable()
      .logout().disable()
      //.anonymous().disable()
      .jee().disable()

      //.authorizeRequests().anyRequest().denyAll()
      .authorizeRequests()
      .antMatchers(HttpMethod.GET, "/actuator").permitAll()
      .antMatchers(HttpMethod.GET, "/actuator/**").permitAll()
      .antMatchers(HttpMethod.POST, "/actuator/**").permitAll()
      .antMatchers(HttpMethod.GET, "/config").permitAll()
      .antMatchers(HttpMethod.GET, "/config/**").permitAll()

      //.anyRequest().denyAll()
      .and()
      .csrf().disable()
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.NEVER)
      .enableSessionUrlRewriting(false)
      .and()
      .exceptionHandling()
      .authenticationEntryPoint(problemSupport)
      .accessDeniedHandler(problemSupport);
  }

}
