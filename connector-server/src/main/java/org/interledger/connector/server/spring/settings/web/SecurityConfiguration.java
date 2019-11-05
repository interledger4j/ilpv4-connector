package org.interledger.connector.server.spring.settings.web;

import static org.interledger.connector.server.spring.settings.metrics.MetricsConfiguration.METRICS_ENDPOINT_URL_PATH;

import org.interledger.connector.core.ConfigConstants;
import org.interledger.connector.links.LinkSettingsFactory;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.server.spring.auth.blast.AuthConstants;
import org.interledger.connector.server.spring.auth.blast.IlpOverHttpAuthenticationProvider;
import org.interledger.connector.server.spring.controllers.HealthController;
import org.interledger.connector.server.spring.controllers.IlpHttpController;
import org.interledger.connector.server.spring.controllers.PathConstants;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionService;

import com.auth0.spring.security.api.JwtWebSecurityConfigurer;
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
import java.util.Objects;
import java.util.function.Supplier;

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

  @Autowired
  LinkSettingsFactory linkSettingsFactory;

  @Autowired
  Decryptor decryptor;

  /**
   * Will be removed once a formal authentication mechanism is added for admin API calls.
   */
  @Deprecated
  @Value("${" + ConfigConstants.ADMIN_PASSWORD + "}")
  private String adminPassword;

  /////////////////
  // For Basic Auth
  /////////////////

  /**
   * Used only for BASIC auth at present. Will be moved to the AdminAPI configuration, and will be unused if the
   * admin-api is disabled.
   *
   * @deprecated
   */
  @Deprecated
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(11);
  }

  @Bean
  IlpOverHttpAuthenticationProvider ilpOverHttpAuthenticationProvider() {
    return new IlpOverHttpAuthenticationProvider(
        connectorSettingsSupplier, encryptionService, accountSettingsRepository, linkSettingsFactory
    );
  }

  /**
   * Will be removed once a formal authentication mechanism is added for admin API calls.
   */
  @Deprecated
  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

    // If the AdminPassword is stored in encrypted-format, we attempt to decrypt it first.
    final byte[] pwBytes;
    if (adminPassword != null && adminPassword.startsWith(EncryptedSecret.ENCODING_PREFIX)) {
      pwBytes = decryptor.decrypt(EncryptedSecret.fromEncodedValue(adminPassword));
    } else {
      pwBytes = Objects.requireNonNull(adminPassword).getBytes();
    }

    auth.inMemoryAuthentication()
        .withUser("admin").password(passwordEncoder().encode(new String(pwBytes)))
        .authorities("connector:admin", "user")
        .and()
        .withUser("user").password(passwordEncoder().encode(new String(pwBytes))).authorities("user");
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

  // TODO: FIXME
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
        // `audience` and `issuer` are not statically configured, but are instead specified in account-settings on a
        // per-account basis.
        .forHS256("n/a", "n/a", ilpOverHttpAuthenticationProvider())
        .configure(http)
        .authorizeRequests()

        //////
        // ILP-over-HTTP
        //////
        .antMatchers(HttpMethod.HEAD, IlpHttpController.ILP_PATH).authenticated()
        .antMatchers(HttpMethod.POST, IlpHttpController.ILP_PATH).authenticated()
        .antMatchers(HttpMethod.GET, HealthController.SLASH_AH_SLASH_HEALTH).permitAll() // permitAll if hidden by LB.
        .antMatchers(HttpMethod.GET, METRICS_ENDPOINT_URL_PATH).permitAll() // permitAll if hidden by LB.
    ;

    // WARNING: Don't add `denyAll` here...it's taken care of after the JWT security below. To verify, turn on debugging
    // for Spring Security (e.g.,  org.springframework.security: DEBUG) and look at the security filter chain).

    http
        .httpBasic()
        .and()
        .authorizeRequests()

        /////////////
        // Settlement
        /////////////
        // TODO: See https://github.com/sappenin/java-ilpv4-connector/issues/226
        // Once that's addressed, then these should be secured.
        .antMatchers(HttpMethod.POST,
            PathConstants.SLASH_ACCOUNTS + PathConstants.SLASH_ACCOUNT_ID + PathConstants.SLASH_SETTLEMENTS).permitAll()
        .antMatchers(HttpMethod.POST,
            PathConstants.SLASH_ACCOUNTS + PathConstants.SLASH_ACCOUNT_ID + PathConstants.SLASH_MESSAGES).permitAll()

        ////////
        // Admin API
        ////////
        .antMatchers(HttpMethod.POST, PathConstants.SLASH_ACCOUNTS)
        .hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN)
        .antMatchers(HttpMethod.GET, PathConstants.SLASH_ACCOUNTS)
        .hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN)
        .antMatchers(HttpMethod.GET, PathConstants.SLASH_ACCOUNTS + PathConstants.SLASH_ACCOUNT_ID).hasAuthority(
        AuthConstants.Authorities.CONNECTOR_ADMIN)
        .antMatchers(HttpMethod.PUT, PathConstants.SLASH_ACCOUNTS + PathConstants.SLASH_ACCOUNT_ID).hasAuthority(
        AuthConstants.Authorities.CONNECTOR_ADMIN)
        // Everything else...
        .anyRequest().denyAll()

        .and()
        .addFilter(securityContextHolderAwareRequestFilter())
        .cors()
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
