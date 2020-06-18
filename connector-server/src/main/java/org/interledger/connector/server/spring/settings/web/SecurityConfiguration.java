package org.interledger.connector.server.spring.settings.web;

import static org.interledger.connector.core.ConfigConstants.DOT;
import static org.interledger.connector.core.ConfigConstants.SPSP_ENABLED;
import static org.interledger.connector.server.spring.settings.metrics.MetricsConfiguration.METRICS_ENDPOINT_URL_PATH;

import org.interledger.connector.accounts.AccessTokenManager;
import org.interledger.connector.core.ConfigConstants;
import org.interledger.connector.links.LinkSettingsFactory;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.server.spring.auth.ilpoverhttp.AuthConstants;
import org.interledger.connector.server.spring.auth.ilpoverhttp.BearerTokenSecurityContextRepository;
import org.interledger.connector.server.spring.auth.ilpoverhttp.IlpOverHttpAuthenticationProvider;
import org.interledger.connector.server.spring.controllers.PathConstants;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.crypto.ByteArrayUtils;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionService;
import org.interledger.openpayments.config.OpenPaymentsPathConstants;

import com.auth0.spring.security.api.JwtAuthenticationEntryPoint;
import com.google.common.eventbus.EventBus;
import io.prometheus.client.cache.caffeine.CacheMetricsCollector;
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
import org.springframework.security.web.util.matcher.RequestMatcher;
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
  CacheMetricsCollector cacheMetricsCollector;

  @Autowired
  Decryptor decryptor;

  @Autowired
  private AccessTokenManager accessTokenManager;

  @Autowired
  private EventBus eventBus;

  /**
   * Will be removed once a formal authentication mechanism is added for admin API calls.
   */
  @Deprecated
  @Value("${" + ConfigConstants.ADMIN_PASSWORD + "}")
  private String adminPassword;

  /**
   * Determines the path that the SPSP server operates under. E.g., https://connector/{spspUrlPath}/bob
   */
  @Value("${" + ConfigConstants.SPSP__URL_PATH + ":}")
  private String spspUrlPath;

  @Value("${" + ConfigConstants.ENABLED_PROTOCOLS + DOT + SPSP_ENABLED + ":false}")
  private boolean spspEnabled;

  /////////////////
  // For Basic Auth
  /////////////////

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(11);
  }

  @Bean
  IlpOverHttpAuthenticationProvider ilpOverHttpAuthenticationProvider() {
    IlpOverHttpAuthenticationProvider provider = new IlpOverHttpAuthenticationProvider(
      connectorSettingsSupplier, encryptionService, accountSettingsRepository, linkSettingsFactory,
      cacheMetricsCollector,
      accessTokenManager);
    eventBus.register(provider);
    return provider;
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
    configuration.setAllowedOrigins(Arrays.asList("*"));
    configuration.setAllowedMethods(Arrays.asList("POST", "GET"));
    configuration.setAllowCredentials(true);
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Override
  public void configure(final HttpSecurity http) throws Exception {

    final RequestMatcher spspRequestMatcher = new SpspRequestMatcher(spspEnabled, spspUrlPath);
    byte[] ephemeralBytes = ByteArrayUtils.generate32RandomBytes();

    // Must come first in order to register properly due to 'denyAll' directive below.
    configureBearerTokenSecurity(http, ephemeralBytes)
      .authorizeRequests()
      //////
      // ILP-over-HTTP
      //////
      .antMatchers(HttpMethod.HEAD, PathConstants.SLASH_ACCOUNTS_ILP_PATH).authenticated()
      .antMatchers(HttpMethod.POST, PathConstants.SLASH_ACCOUNTS_ILP_PATH).authenticated()
      .antMatchers(HttpMethod.GET, PathConstants.SLASH_ACCOUNTS_BALANCE_PATH).authenticated()
      .antMatchers(HttpMethod.GET, PathConstants.SLASH_ACCOUNTS_TOKENS_PATH + "/**").authenticated()
      .antMatchers(HttpMethod.POST, PathConstants.SLASH_ACCOUNTS_TOKENS_PATH).authenticated()
      .antMatchers(HttpMethod.DELETE, PathConstants.SLASH_ACCOUNTS_TOKENS_PATH + "/**").authenticated()
      .antMatchers(HttpMethod.GET, PathConstants.SLASH_ACCOUNTS_PAYMENTS_PATH + "/**").authenticated()
      .antMatchers(HttpMethod.POST, PathConstants.SLASH_ACCOUNTS_PAYMENTS_PATH).authenticated()

      .antMatchers(HttpMethod.GET, METRICS_ENDPOINT_URL_PATH).permitAll() // permitAll if hidden by LB.
      // SPSP (if enabled)
      .requestMatchers(spspRequestMatcher).permitAll()
    ;

    // WARNING: Don't add `denyAll` here...it's taken care of after the JWT security below. To verify, turn on debugging
    // for Spring Security (e.g.,  org.springframework.security: DEBUG) and look at the security filter chain).

    http
      // See https://docs.spring.io/spring-security/site/docs/5.3.2.BUILD-SNAPSHOT/reference/html5/#servlet-headers-csp
      // See https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP
      .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
        "default-src 'self'" // --> No reporting.
        //"default-src 'self'; report-uri /_/csp-reports/" --> To enable reporting.
      )))
      .httpBasic()
      .and()
      .authorizeRequests()
      .antMatchers(HttpMethod.GET, PathConstants.SLASH).permitAll()
      // @formatter:off

      /////////////
      // Settlement
      /////////////
      // TODO: See https://github.com/interledger4j/ilpv4-connector/issues/226
      // Once that's addressed, then these should be secured.
      .antMatchers(HttpMethod.POST, PathConstants.SLASH_ACCOUNTS + PathConstants.SLASH_ACCOUNT_ID + PathConstants.SLASH_SETTLEMENTS).permitAll()
      .antMatchers(HttpMethod.POST, PathConstants.SLASH_ACCOUNTS + PathConstants.SLASH_ACCOUNT_ID + PathConstants.SLASH_MESSAGES).permitAll()
      .antMatchers(HttpMethod.POST, PathConstants.WEBHOOKS + "/**").permitAll()

      ////////
      // Admin API
      ////////

      // Actuator URLs
      .antMatchers(HttpMethod.GET, PathConstants.SLASH_MANAGE).permitAll()
      .antMatchers(HttpMethod.GET, PathConstants.SLASH_MANAGE + PathConstants.SLASH_HEALTH).permitAll()
      .antMatchers(HttpMethod.GET, PathConstants.SLASH_MANAGE + PathConstants.SLASH_INFO).permitAll()
      .antMatchers(HttpMethod.GET, PathConstants.SLASH_MANAGE + "/**").hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN)

      // /accounts
      .antMatchers(HttpMethod.POST, PathConstants.SLASH_ACCOUNTS).hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN)
      .antMatchers(HttpMethod.GET, PathConstants.SLASH_ACCOUNTS).hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN)
      .antMatchers(HttpMethod.GET, PathConstants.SLASH_ACCOUNTS + PathConstants.SLASH_ACCOUNT_ID).hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN)
      .antMatchers(HttpMethod.PUT, PathConstants.SLASH_ACCOUNTS + PathConstants.SLASH_ACCOUNT_ID).hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN)
      .antMatchers(HttpMethod.DELETE, PathConstants.SLASH_ACCOUNTS + PathConstants.SLASH_ACCOUNT_ID).hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN)
      // /routes
      .antMatchers(HttpMethod.GET, PathConstants.SLASH_ROUTES).hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN)
      .antMatchers(HttpMethod.GET, PathConstants.SLASH_ROUTES_STATIC).hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN)
      .antMatchers(HttpMethod.PUT, PathConstants.SLASH_ROUTES_STATIC_PREFIX).hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN)
      .antMatchers(HttpMethod.DELETE, PathConstants.SLASH_ROUTES_STATIC_PREFIX).hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN)

      // encrypted
      .antMatchers(HttpMethod.POST, PathConstants.SLASH_ENCRYPTION + "/**").hasAuthority(AuthConstants.Authorities.CONNECTOR_ADMIN);


    if (connectorSettingsSupplier.get().enabledProtocols().isOpenPaymentsEnabled()) {
      // Open Payments Invoices.  All open for now.
      http
        .authorizeRequests()
        //      .antMatchers(HttpMethod.POST, OpenPaymentsPathConstants.SLASH_ACCOUNTS_OPA_PAY).authenticated()
        .antMatchers(HttpMethod.GET, OpenPaymentsPathConstants.INVOICES_BASE + "/**").permitAll()
        .antMatchers(HttpMethod.POST, OpenPaymentsPathConstants.INVOICES_BASE + "/**").permitAll()
        .antMatchers(HttpMethod.POST, OpenPaymentsPathConstants.PAY_INVOICE + "/**").permitAll()
        .antMatchers(HttpMethod.GET, OpenPaymentsPathConstants.MANDATES_BASE + "/**").permitAll()
        .antMatchers(HttpMethod.POST, OpenPaymentsPathConstants.MANDATES_BASE + "/**").permitAll()
        .antMatchers(HttpMethod.GET, OpenPaymentsPathConstants.SLASH_ACCOUNT_ID).permitAll();
    }

    // Everything else...
    http
      .authorizeRequests()
      .anyRequest().denyAll()
      .and()
      .addFilter(securityContextHolderAwareRequestFilter())
      .cors()
      .and()
      .formLogin().disable()
      .logout().disable()
      //.anonymous().disable()
      .jee().disable()
      //.authorizeRequests()
      //.antMatchers(HttpMethod.GET, HealthController.SLASH_AH_SLASH_HEALTH).permitAll()
      //.anyRequest().denyAll()
      //.and()
      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER).enableSessionUrlRewriting(false)
      .and()
      .exceptionHandling().authenticationEntryPoint(problemSupport).accessDeniedHandler(problemSupport);

    // @formatter:on
  }

  private HttpSecurity configureBearerTokenSecurity(HttpSecurity http, byte[] ephemeralBytes) throws Exception {
    return http
      .authenticationProvider(ilpOverHttpAuthenticationProvider())
      .securityContext()
      .securityContextRepository(new BearerTokenSecurityContextRepository(ephemeralBytes))
      .and()
      .exceptionHandling()
      .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
      .and()
      .httpBasic().disable()
      .csrf().disable()
      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and();
  }

}
