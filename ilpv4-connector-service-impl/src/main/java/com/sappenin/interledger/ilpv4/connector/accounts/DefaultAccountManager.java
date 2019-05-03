package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ModifiableConnectorSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketMapper;
import org.interledger.ildcp.IldcpFetcher;
import org.interledger.ildcp.IldcpRequest;
import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.ildcp.IldcpUtils;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * The default {@link AccountManager}.
 */
public class DefaultAccountManager implements AccountManager {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final AccountSettingsRepository accountSettingsRepository;
  private final LinkManager linkManager;
  private final ConversionService conversionService;

  /**
   * Required-args Constructor.
   */
  public DefaultAccountManager(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final AccountSettingsRepository accountSettingsRepository,
    final LinkManager linkManager,
    final ConversionService conversionService
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.conversionService = Objects.requireNonNull(conversionService);
  }

  @Override
  public AccountSettingsRepository getAccountSettingsRepository() {
    return accountSettingsRepository;
  }

  @Override
  public LinkManager getLinkManager() {
    return linkManager;
  }

  /**
   * Create a new account in this connector.
   *
   * @param accountSettings The {@link AccountSettings} for this account.
   *
   * @return The newly-created settings.
   */
  @Override
  public AccountSettings createAccount(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    final AccountSettingsEntity accountSettingsEntity = new AccountSettingsEntity(accountSettings);
    final AccountSettings returnableAccountSettings = this.conversionService.convert(
      this.accountSettingsRepository.save(accountSettingsEntity), AccountSettings.class
    );

    // It is _not_ a requirement that a Connector startup with any accounts configured. Thus, the first account added
    // to the connector with a relationhip type `PARENT` should trigger IL-DCP, but only if the operator address has
    // not already been populated.
    if (AccountRelationship.PARENT.equals(accountSettings.getAccountRelationship())) {
      if (!connectorSettingsSupplier.get().getOperatorAddress().isPresent()) {
        this.initializeParentAccountSettingsViaIlDcp(accountSettings);
      }
    }

    return returnableAccountSettings;
  }

  @Override
  public AccountSettings initializeParentAccountSettingsViaIlDcp(final AccountSettings primaryParentAccountSettings) {
    Objects.requireNonNull(primaryParentAccountSettings);

    final Link<?> link = this.getLinkManager().createLink(primaryParentAccountSettings, true);

    final IldcpResponse ildcpResponse = ((IldcpFetcher) ildcpRequest -> {
      Objects.requireNonNull(ildcpRequest);

      final IldcpRequestPacket ildcpRequestPacket = IldcpRequestPacket.builder().build();
      final InterledgerPreparePacket preparePacket =
        InterledgerPreparePacket.builder().from(ildcpRequestPacket).build();
      final InterledgerResponsePacket response = link.sendPacket(preparePacket);
      return new InterledgerResponsePacketMapper<IldcpResponse>() {
        @Override
        protected IldcpResponse mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
          return IldcpUtils.toIldcpResponse(interledgerFulfillPacket);
        }

        @Override
        protected IldcpResponse mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
          throw new RuntimeException(String.format("IL-DCP negotiation failed! Reject: %s", interledgerRejectPacket));
        }
      }.map(response);

    }).fetch(IldcpRequest.builder().build());

    //////////////////////////////////
    // Update the Operator address with data returned by IL-DCP!
    //////////////////////////////////

    // TODO: Consider a better way to update the operator address for a connector. Maybe an event?
    ((ModifiableConnectorSettings) this.connectorSettingsSupplier.get())
      .setOperatorAddress(ildcpResponse.getClientAddress());

    //////////////////////////////////
    // Update the Account Settings with data returned by IL-DCP!
    //////////////////////////////////
    final AccountSettingsEntity primaryParentAccountSettingsEntity =
      new AccountSettingsEntity(primaryParentAccountSettings);
    primaryParentAccountSettingsEntity.setAssetCode(ildcpResponse.getAssetCode());
    primaryParentAccountSettingsEntity.setAssetScale(ildcpResponse.getAssetScale());

    // Modify Account Settings by removing and re-creating the parent account.
    final AccountSettingsEntity updatedAccountSettings =
      getAccountSettingsRepository().save(primaryParentAccountSettingsEntity);

    logger.info(
      "IL-DCP Succeeded! Operator Address: `{}`", connectorSettingsSupplier.get().getOperatorAddress().get()
    );

    return updatedAccountSettings;
  }
}
