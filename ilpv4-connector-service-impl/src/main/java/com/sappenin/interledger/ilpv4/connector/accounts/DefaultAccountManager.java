package com.sappenin.interledger.ilpv4.connector.accounts;

import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ModifiableConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementEngineClient;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
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
import org.interledger.ilpv4.connector.persistence.entities.AccountBalanceSettingsEntity;
import org.interledger.ilpv4.connector.persistence.entities.AccountRateLimitSettingsEntity;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.entities.SettlementEngineDetailsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.ilpv4.connector.settlement.client.CreateSettlementAccountRequest;
import org.interledger.ilpv4.connector.settlement.client.CreateSettlementAccountResponse;
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

  @Override
  public AccountSettings updateAccount(
    final AccountId accountId, final AccountSettings accountSettings
  ) {
    Objects.requireNonNull(accountSettings);

    // Make sure this account exists first, and only update it if it exists...
    return this.getAccountSettingsRepository().findByAccountId(accountId)
      .map(entity -> {
        // Ignore update accountId
        entity.setAssetCode(accountSettings.getAssetCode());
        entity.setAssetScale(accountSettings.getAssetScale());
        entity.setAccountRelationship(accountSettings.getAccountRelationship());
        entity.setConnectionInitiator(accountSettings.isConnectionInitiator());
        entity.setDescription(accountSettings.getDescription());
        entity.setCustomSettings(accountSettings.getCustomSettings());
        entity.setIlpAddressSegment(accountSettings.getIlpAddressSegment());
        entity.setInternal(accountSettings.isInternal());
        entity.setLinkType(accountSettings.getLinkType());
        entity.setMaximumPacketAmount(accountSettings.getMaximumPacketAmount());
        entity.setRateLimitSettings(
          new AccountRateLimitSettingsEntity(accountSettings.getRateLimitSettings())
        );
        entity.setBalanceSettings(
          new AccountBalanceSettingsEntity(accountSettings.getBalanceSettings())
        );
        accountSettings.settlementEngineDetails().ifPresent(settlementEngineDetails ->
          entity.setSettlementEngineDetails(new SettlementEngineDetailsEntity(settlementEngineDetails))
        );
        entity.setReceiveRoutes(accountSettings.isReceiveRoutes());
        entity.setSendRoutes(accountSettings.isSendRoutes());

        // Save the account...if this fails, then the SE will not have been updated, but a caller _could_ try again
        // with the same request.
        accountSettingsRepository.save(entity);

        //////////////////////
        // TODO: See https://github.com/sappenin/java-ilpv4-connector/issues/270
        //  In the current version of the SE RFC, there is nothing to update on a settlement engine account
        //  other than the identifier. However, this implementation doesn't allow the id to be updated because there
        //  currently isn't a good way to guard against this being abused from a security perspective, and it doesn't
        //  seem like a valid use-case anyway. Thus, once the RFC if finalized, we might have webhooks that need to
        //  be updated. If that's the case, then we should update this code here. If webhooks don't make their way
        //  into the RFC, then this can be removed.
        // Update the account on the settlement engine, but don't allow the identifier to be updated!
        //        accountSettings.settlementEngineDetails().ifPresent(settlementEngineDetails -> {
        //
        //          // WARNING: This value MUST be source from the DB entity in order to avoid settlement engine account
        //          // hijacking!
        //          final SettlementEngineAccountId settlementEngineAccountId = SettlementEngineAccountId.of(
        //            entity.getSettlementEngineDetailsEntity().getSettlementEngineAccountId()
        //          );
        //
        //          // Update the account in the Settlement Engine
        //          final SettlementAccount response = settlementEngineClient.updateSettlementAccount(
        //            accountSettings.getAccountId(),
        //            // WARNING: This value MUST be source from the DB entity in order to avoid settlement engine account
        //            // hijacking!
        //            settlementEngineAccountId,
        //            settlementEngineDetails.baseUrl(),
        //            SettlementAccount.builder().settlementAccountId(settlementEngineAccountId).build()
        //          );
        //
        //          // There is nothing to update in the SettlementEngineAccount at present, but this will change once the RFC
        //          // is completed.
        //
        //        });

        return entity;
      })
      .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class))
      .orElseThrow(() -> new AccountNotFoundProblem(accountId));
  }

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
