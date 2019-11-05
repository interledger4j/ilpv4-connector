package org.interledger.connector.server;

import org.interledger.connector.server.spring.SpringProfileUtils;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.link.Link;

import com.google.common.base.Preconditions;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.MonetaryConversions;

/**
 * An extension of {@link Server} that implements ILPv4 Connector functionality.
 */
public class ConnectorServer extends Server {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // Allows a Server to use an overridden ConnectorSettings (useful for ITs).
  private final Optional<ConnectorSettings> connectorSettingsOverride;

  public ConnectorServer() {
    super(ConnectorServerConfig.class);
    this.connectorSettingsOverride = Optional.empty();
  }

  public ConnectorServer(final ConnectorSettings connectorSettings) {
    super(ConnectorServerConfig.class);
    this.connectorSettingsOverride = Optional.of(connectorSettings);
  }

  @Override
  public void start() {
    super.start();
    // ...only now is everything wired-up.
    if (SpringProfileUtils.isProfileActive(getContext().getEnvironment(), "MIGRATE-ONLY")) {
      System.out.println("###################################################################");
      System.out.println("!!! Container started with migrate-only profile. Shutting down. !!!");
      System.out.println("###################################################################");
      this.stop();
      return;
    }

    this.emitFxInfo();

    if (getConnectorSettings().get().operatorAddress().equals(Link.SELF)) {
      logger.info("STARTED INTERLEDGER CHILD CONNECTOR: [Operator Address pending IL-DCP]");
    } else {
      logger.info("STARTED INTERLEDGER CONNECTOR: `{}`", getConnectorSettings().get().operatorAddress());
    }
  }

  /**
   * Handle an application event.
   *
   * @param event the event to respond to
   */
  @Override
  public void onApplicationEvent(final ApplicationEvent event) {
    Objects.requireNonNull(event);
    if (event instanceof ApplicationPreparedEvent) {
      // If there is a ConnectorSettingsOverride, then add it to the ApplicationContext. The ConnectorConfig is smart
      // enough to detect it and use it instead.
      this.connectorSettingsOverride
        .ifPresent(cso -> ((ApplicationPreparedEvent) event).getApplicationContext().getBeanFactory()
          .registerSingleton(ConnectorSettings.OVERRIDE_BEAN_NAME, cso));
    }
  }

  /**
   * Helper method to emit currently configured FX info.
   */
  private void emitFxInfo() {
    // Sanity check to ensure that FX is configured properly...
    final CurrencyUnit CURRENCY_XRP = Monetary.getCurrency("XRP");
    final CurrencyUnit CURRENCY_USD = Monetary.getCurrency("USD");
    final CurrencyUnit CURRENCY_EUR = Monetary.getCurrency("EUR");

    Preconditions.checkNotNull(CURRENCY_XRP, "XRP currency not configured");
    Preconditions.checkNotNull(CURRENCY_USD, "USD currency not configured");
    Preconditions.checkNotNull(CURRENCY_EUR, "EUR currency not configured");

    /////////////
    // Tests that Spring-configured JavaMoney is working properly...
    // these values will come from whatever is configured in JavaMoneyConfig (e.g., CryptoCompare).
    /////////////
    final CurrencyConversion usdConversion = MonetaryConversions.getConversion("USD");

    final Money xrpInUsd = Money.of(1, CURRENCY_XRP).with(usdConversion);
    logger.info("Current FX: 1 XRP => ${}", xrpInUsd);

    final Money eurInUsd = Money.of(1, CURRENCY_EUR).with(usdConversion);
    logger.info("Current FX: 1 EUR => ${}", eurInUsd);

    final Money usdInUsd = Money.of(1, CURRENCY_USD).with(usdConversion);
    logger.info("Current FX: 1 USD => ${}", usdInUsd);

    // Round `XRP` to the nearest `Drop`
    // (see https://jaxenter.de/go-for-the-money-einfuehrung-in-das-money-and-currency-api-38668)
    {
      final Money preRounded = Money.of(new BigDecimal("1.1234567898"), CURRENCY_XRP);
      final String expected = "1.123457";
      final MonetaryAmount postRounded = preRounded.with(Monetary.getRounding(preRounded.getCurrency()));
      Preconditions.checkArgument(postRounded.getNumber().toString().equalsIgnoreCase(expected),
        String.format(
          "XRP Rounding is mis-configured. %s should have rounded up to %s, but was instead %s",
          preRounded, expected, postRounded.getNumber().toString())
      );
      logger.info("{} XRP rounds up to => {} (rounded to the nearest drop)", preRounded, postRounded);
    }
    {
      final Money preRounded = Money.of(new BigDecimal("1.1234561"), CURRENCY_XRP);
      final String expected = "1.123456";
      final MonetaryAmount postRounded = preRounded.with(Monetary.getRounding(preRounded.getCurrency()));
      Preconditions.checkArgument(postRounded.getNumber().toString().equalsIgnoreCase(expected),
        String.format(
          "XRP Rounding is mis-configured. %s should have rounded down to %s, but was instead %s",
          preRounded, expected, postRounded.getNumber().toString())
      );
      logger.info("{} XRP rounds down to => {} (rounded to the nearest drop)", preRounded, postRounded);
    }
    {
      final Money preRounded = Money.of(new BigDecimal("1.123456000"), CURRENCY_XRP);
      final String expected = "1.123456";
      final MonetaryAmount postRounded = preRounded.with(Monetary.getRounding(preRounded.getCurrency()));
      Preconditions.checkArgument(postRounded.getNumber().toString().equalsIgnoreCase(expected),
        String.format(
          "XRP Rounding is mis-configured. %s should have rounded evently to %s, but was instead %s",
          preRounded, expected, postRounded.getNumber().toString())
      );
      logger.info("{} XRP rounds evenly to => {} (rounded to the nearest drop)", preRounded, postRounded);
    }
  }
}
