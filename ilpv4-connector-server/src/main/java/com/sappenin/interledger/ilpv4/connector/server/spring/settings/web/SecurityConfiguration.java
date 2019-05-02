package com.sappenin.interledger.ilpv4.connector.server.spring.settings.web;

import com.auth0.spring.security.api.JwtWebSecurityConfigurer;
import com.sappenin.interledger.ilpv4.connector.server.spring.auth.blast.BlastAuthenticationProvider;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.HealthController;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.IlpHttpController;
import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.admin.AccountsController;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.crypto.EncryptionService;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport;

import java.util.Arrays;
import java.util.function.Supplier;

import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.admin.AccountsController.SLASH_ACCOUNT_ID;

@Configuration
@EnableWebSecurity
@Import(SecurityProblemSupport.class)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Autowired
  Supplier<ConnectorSettings> connectorSettingsSupplier;

  @Autowired
  SecurityProblemSupport problemSupport;

  @Autowired
  AccountSettingsRepository accountSettingsRepository;

  @Autowired
  EncryptionService encryptionService;
  /**
   * Will be removed once a formal authentication mechanism is added for admin API calls.
   */
  @Deprecated
  @Value("${ilpv4.connector.admin_password}")
  private String adminPassword;

  /////////////////
  // For Basic Auth
  /////////////////

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  BlastAuthenticationProvider blastAuthenticationProvider() {
    return new BlastAuthenticationProvider(connectorSettingsSupplier, encryptionService, accountSettingsRepository);
  }

  /**
   * Will be removed once a formal authentication mechanism is added for admin API calls.
   */
  @Deprecated
  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    auth.inMemoryAuthentication()
      .withUser("admin").password(passwordEncoder().encode(adminPassword))
      .authorities("connector:admin");
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
      // `audience` and `issuer` are unused.
      .forHS256("n/a", "n/a", blastAuthenticationProvider())
      .configure(http)
      .authorizeRequests()

      //////
      // ILP-over-HTTP
      //////
      .antMatchers(HttpMethod.HEAD, IlpHttpController.ILP_PATH).authenticated()
      .antMatchers(HttpMethod.POST, IlpHttpController.ILP_PATH).authenticated()
      .antMatchers(HttpMethod.GET, HealthController.SLASH_AH_SLASH_HEALTH).permitAll()

      ////////
      // Admin API
      ////////
      .antMatchers(HttpMethod.POST, AccountsController.SLASH_ACCOUNTS).authenticated()
      .antMatchers(HttpMethod.GET, AccountsController.SLASH_ACCOUNTS).authenticated()
      .antMatchers(HttpMethod.GET, AccountsController.SLASH_ACCOUNTS + SLASH_ACCOUNT_ID).authenticated()
      .antMatchers(HttpMethod.PUT, AccountsController.SLASH_ACCOUNTS + SLASH_ACCOUNT_ID).authenticated()

      // Everything else...
      .anyRequest().denyAll();

    http
      .addFilter(securityContextHolderAwareRequestFilter())
      .cors()
      .and()
      .httpBasic()
      .and()
      .formLogin().disable()
      .logout().disable()
      //.anonymous().disable()
      .jee().disable()
      .authorizeRequests()
      //.antMatchers(HttpMethod.GET, HealthController.SLASH_AH_SLASH_HEALTH).permitAll()
      .anyRequest().denyAll()
      .and()
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.NEVER)
      .enableSessionUrlRewriting(false)
      .and()
      .exceptionHandling()
      .authenticationEntryPoint(problemSupport)
      .accessDeniedHandler(problemSupport);
  }

}
