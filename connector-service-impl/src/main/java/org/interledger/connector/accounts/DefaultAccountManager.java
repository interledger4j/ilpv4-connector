package org.interledger.connector.accounts;

import org.interledger.connector.links.LinkManager;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.ModifiableConnectorSettings;
import org.interledger.connector.settlement.SettlementEngineClient;
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
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.entities.SettlementEngineDetailsEntity;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.settlement.client.CreateSettlementAccountRequest;
import org.interledger.connector.settlement.client.CreateSettlementAccountResponse;
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
  private final SettlementEngineClient settlementEngineClient;

  /**
   * Required-args Constructor.
   */
  public DefaultAccountManager(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final ConversionService conversionService,
    final AccountSettingsRepository accountSettingsRepository,
    final LinkManager linkManager,
    final SettlementEngineClient settlementEngineClient
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.conversionService = Objects.requireNonNull(conversionService);
    this.settlementEngineClient = Objects.requireNonNull(settlementEngineClient);
  }

  @Override
  public AccountSettingsRepository getAccountSettingsRepository() {
    return accountSettingsRepository;
  }

  @Override
  public LinkManager getLinkManager() {
    return linkManager;
  }

  @Override
  public AccountSettings createAccount(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    final AccountSettingsEntity accountSettingsEntity = new AccountSettingsEntity(accountSettings);
    final SettlementEngineDetailsEntity settlementEngineDetailsEntity =
      accountSettingsEntity.getSettlementEngineDetailsEntity();

    if (settlementEngineDetailsEntity != null &&
      settlementEngineDetailsEntity.getBaseUrl() != null // TODO: this line can be removed when #217 is fixed.
    ) {
      // Initialize an Account in the configured Settlement Engine, if that's enabled configured. This call is
      // idempotent, so we do this first before saving anything to the DB. If this call fails, it will throw an
      // exception, and no account will have been created in the Connector.

      final CreateSettlementAccountResponse response = settlementEngineClient.createSettlementAccount(
        accountSettings.getAccountId(),
        settlementEngineDetailsEntity.baseUrl(),
        CreateSettlementAccountRequest.builder()
          .requestedSettlementAccountId(settlementEngineDetailsEntity.settlementEngineAccountId())
          .build()
      );
      settlementEngineDetailsEntity.setSettlementEngineAccountId(response.settlementEngineAccountId().value());
    }

    ////////////////
    // Persist the AccountSettingsEntity
    ////////////////
    final AccountSettings returnableAccountSettings = this.conversionService.convert(
      this.accountSettingsRepository.save(accountSettingsEntity), AccountSettings.class
    );

    // It is _not_ a requirement that a Connector startup with any accounts configured. Thus, the first account added
    // to the connector with a relationship type `PARENT` should trigger IL-DCP, but only if the operator address has
    // not already been populated. This scenario will only ever happen once (the connector starts-up with no ILP
    // address configured, and a new account is added of type `PARENT`). Once this is done, subsequent restarts of
    // the Connector will not enter into this code-block because if the connector has a PARENT account configured and
    // no ILP address specified, then the Connector startup will trigger IL-DCP. In other words, this check is only
    // required for the first-account-creation (under certain conditions).
    if (AccountRelationship.PARENT.equals(accountSettings.getAccountRelationship())) {
      if (!connectorSettingsSupplier.get().getOperatorAddress().isPresent()) {
        this.initializeParentAccountSettingsViaIlDcp(accountSettings.getAccountId());
      }
    }

    // No need to prematurely connect to this account. When packets need to flow over it, it will become connected.
    return returnableAccountSettings;
  }

  /**
   * Initialize a parent account with new settings from the parent connector.
   *
   * @param accountId The {@link AccountId} to initialize via IL-DCP.
   *
   * @return The new {@link AccountSettings} after performing IL-DCP.
   */
  @Override
  public AccountSettings initializeParentAccountSettingsViaIlDcp(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    // For IL-DCP to work, there MUST be a pre-exising parent account configured in the AccountSettingsRepository.
    // It's fine to preemptively load from the data-store here because these settings will naturally be updated later
    // in this method.
    final AccountSettingsEntity parentAccountSettingsEntity =
      getAccountSettingsRepository().safeFindByAccountId(accountId);

    final Link<?> link = this.getLinkManager().getOrCreateLink(parentAccountSettingsEntity);
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
    parentAccountSettingsEntity.setAssetCode(ildcpResponse.getAssetCode());
    parentAccountSettingsEntity.setAssetScale(ildcpResponse.getAssetScale());

    // Modify Account Settings by removing and re-creating the parent account.
    final AccountSettings updatedAccountSettings = getAccountSettingsRepository().save(parentAccountSettingsEntity);

    logger.info(
      "IL-DCP Succeeded! Operator Address: `{}`", connectorSettingsSupplier.get().getOperatorAddress().get()
    );

    return updatedAccountSettings;
  }
}
