package org.interledger.connector.accounts;

import org.interledger.codecs.ildcp.IldcpUtils;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.LinkSettingsFactory;
import org.interledger.connector.links.LinkSettingsValidator;
import org.interledger.connector.persistence.entities.AccountBalanceSettingsEntity;
import org.interledger.connector.persistence.entities.AccountRateLimitSettingsEntity;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.entities.DataConstants;
import org.interledger.connector.persistence.entities.DeletedAccountSettingsEntity;
import org.interledger.connector.persistence.entities.SettlementEngineDetailsEntity;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.persistence.repositories.DeletedAccountSettingsRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.ModifiableConnectorSettings;
import org.interledger.connector.settlement.SettlementEngineClient;
import org.interledger.connector.settlement.client.CreateSettlementAccountRequest;
import org.interledger.connector.settlement.client.CreateSettlementAccountResponse;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.ildcp.IldcpFetcher;
import org.interledger.ildcp.IldcpRequest;
import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.link.Link;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The default {@link AccountManager}.
 */
public class DefaultAccountManager implements AccountManager {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final AccountSettingsRepository accountSettingsRepository;
  private final DeletedAccountSettingsRepository deletedAccountSettingsRepository;
  private final LinkManager linkManager;
  private final ConversionService conversionService;
  private final SettlementEngineClient settlementEngineClient;
  private final LinkSettingsFactory linkSettingsFactory;
  private final LinkSettingsValidator linkSettingsValidator;

  /**
   * Required-args Constructor.
   */
  public DefaultAccountManager(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final ConversionService conversionService,
    final AccountSettingsRepository accountSettingsRepository,
    final DeletedAccountSettingsRepository deletedAccountSettingsRepository,
    final LinkManager linkManager,
    final SettlementEngineClient settlementEngineClient,
    LinkSettingsFactory linkSettingsFactory, LinkSettingsValidator linkSettingsValidator) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.deletedAccountSettingsRepository = Objects.requireNonNull(deletedAccountSettingsRepository);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.conversionService = Objects.requireNonNull(conversionService);
    this.settlementEngineClient = Objects.requireNonNull(settlementEngineClient);
    this.linkSettingsFactory = linkSettingsFactory;
    this.linkSettingsValidator = linkSettingsValidator;
  }

  @Override
  public LinkManager getLinkManager() {
    return linkManager;
  }

  @Override
  public Optional<AccountSettings> findAccountById(AccountId accountId) {
    return accountSettingsRepository.findByAccountId(accountId)
      .map(entity -> this.conversionService.convert(entity, AccountSettings.class));
  }

  @Override
  public List<AccountSettings> getAccounts() {
    return StreamSupport.stream(accountSettingsRepository.findAll().spliterator(), false)
      .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class))
      .map(entity -> this.conversionService.convert(entity, AccountSettings.class))
      .collect(Collectors.toList());
  }

  @Override
  public AccountSettings createAccount(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);
    validateAccountIdFormat(accountSettings.accountId());

    if (accountSettingsRepository.findByAccountId(accountSettings.accountId()).isPresent()) {
      throw new AccountAlreadyExistsProblem(accountSettings.accountId());
    }

    final AccountSettingsEntity accountSettingsEntity =
      new AccountSettingsEntity(validateLinkSettings(accountSettings));
    final SettlementEngineDetailsEntity settlementEngineDetailsEntity =
      accountSettingsEntity.getSettlementEngineDetailsEntity();

    if (settlementEngineDetailsEntity != null
    ) {
      // Initialize an Account in the configured Settlement Engine, if that's enabled configured. This call is
      // idempotent, so we do this first before saving anything to the DB. If this call fails, it will throw an
      // exception, and no account will have been created in the Connector.

      final HttpUrl baseUrl;
      try {
        baseUrl = HttpUrl.parse(settlementEngineDetailsEntity.getBaseUrl());
      } catch (Exception e) {
        throw new InvalidAccountSettingsProblem(
          "Settlement Engine BaseURL was invalid: " + settlementEngineDetailsEntity.getBaseUrl(),
          accountSettings.accountId());
      }

      final CreateSettlementAccountResponse response = settlementEngineClient.createSettlementAccount(
        accountSettings.accountId(),
        baseUrl,
        CreateSettlementAccountRequest.builder().build()
      );
      settlementEngineDetailsEntity.setSettlementEngineAccountId(response.settlementEngineAccountId().value());
    }
    AccountSettingsEntity entity = persistAccountSettingsEntity(accountSettingsEntity);

    final AccountSettings returnableAccountSettings = this.conversionService.convert(entity, AccountSettings.class);

    // It is _not_ a requirement that a Connector startup with any accounts configured. Thus, the first account added
    // to the connector with a relationship type `PARENT` should trigger IL-DCP, but only if the operator address has
    // not already been populated. This scenario will only ever happen once (the connector starts-up with no ILP
    // address configured, and a new account is added of type `PARENT`. This account will have the default address of
    // `self.node`, which indicates the address is unset). Once this is done, subsequent restarts of the Connector will
    // not enter into this code-block because if the connector has a PARENT account configured and no ILP address
    // specified, then the Connector startup will trigger IL-DCP. In other words, this check is only required for the
    // first-account-creation (under certain conditions).
    if (AccountRelationship.PARENT.equals(accountSettings.accountRelationship())
      && Link.SELF.equals(connectorSettingsSupplier.get().operatorAddress())
    ) {
      this.initializeParentAccountSettingsViaIlDcp(accountSettings.accountId());
    }

    // No need to prematurely connect to this account. When packets need to flow over it, it will become connected.
    return returnableAccountSettings;
  }

  @Override
  public AccountSettings updateAccount(AccountId accountId, AccountSettings accountSettings) {
    AccountSettings updatedSettings = validateLinkSettings(accountSettings);
    return accountSettingsRepository.findByAccountId(accountId)
      .map(entity -> {

        // Ignore update accountId

        entity.setAssetCode(updatedSettings.assetCode());
        entity.setAssetScale(updatedSettings.assetScale());
        entity.setAccountRelationship(updatedSettings.accountRelationship());
        entity.setBalanceSettings(
          new AccountBalanceSettingsEntity(updatedSettings.balanceSettings())
        );
        entity.setConnectionInitiator(updatedSettings.isConnectionInitiator());
        entity.setDescription(updatedSettings.description());
        entity.setCustomSettings(updatedSettings.customSettings());
        entity.setIlpAddressSegment(updatedSettings.ilpAddressSegment());
        entity.setInternal(updatedSettings.isInternal());
        entity.setLinkType(updatedSettings.linkType());
        entity.setMaximumPacketAmount(updatedSettings.maximumPacketAmount().map(UnsignedLong::bigIntegerValue));
        entity.setRateLimitSettings(
          new AccountRateLimitSettingsEntity(updatedSettings.rateLimitSettings())
        );
        entity.setReceiveRoutes(updatedSettings.isReceiveRoutes());
        entity.setSendRoutes(updatedSettings.isSendRoutes());

        return accountSettingsRepository.save(entity);
      })
      .map(entity -> this.conversionService.convert(entity, AccountSettings.class))
      .orElseThrow(() -> new AccountNotFoundProblem(accountId));
  }

  @Override
  public AccountSettings validateLinkSettings(AccountSettings accountSettings) {
    try {
      if (accountSettings.linkType().equals(IlpOverHttpLink.LINK_TYPE)) {
        IlpOverHttpLinkSettings ilpOverHttpLinkSettings =
          linkSettingsValidator.validateSettings(linkSettingsFactory.constructTyped(accountSettings));

        return AccountSettings.builder()
          .from(accountSettings)
          .customSettings(ilpOverHttpLinkSettings.getCustomSettings())
          .build();
      }
      return accountSettings;
    } catch (IllegalArgumentException e) {
      throw new InvalidAccountSettingsProblem("Bad shared secret: " + e.getMessage()
        , accountSettings.accountId());
    }
  }

  @VisibleForTesting
  protected AccountSettingsEntity persistAccountSettingsEntity(AccountSettingsEntity accountSettingsEntity) {
    ////////////////
    // Persist the AccountSettingsEntity
    ////////////////
    AccountSettingsEntity entity;
    try {
      entity = this.accountSettingsRepository.save(accountSettingsEntity);
    } catch (Exception e) {
      if (e.getCause() instanceof ConstraintViolationException) {
        ConstraintViolationException cause = (ConstraintViolationException) e.getCause();
        if (cause.getConstraintName().contains(DataConstants.ConstraintNames.ACCOUNT_SETTINGS_SETTLEMENT_ENGINE)) {
          throw new AccountSettlementEngineAlreadyExistsProblem(accountSettingsEntity.getAccountId(),
            accountSettingsEntity.getSettlementEngineDetailsEntity().getSettlementEngineAccountId());
        }
      }
      throw e;
    }
    return entity;
  }

  /**
   * Initialize a parent account with new settings from the parent connector.
   *
   * @param accountId The {@link AccountId} to initialize via IL-DCP.
   * @return The new {@link AccountSettings} after performing IL-DCP.
   */
  @Override
  public AccountSettings initializeParentAccountSettingsViaIlDcp(final AccountId accountId) {
    Objects.requireNonNull(accountId);

    // For IL-DCP to work, there MUST be a pre-existing parent account configured in the AccountSettingsRepository.
    // It's fine to preemptively load from the data-store here because these settings will naturally be updated later
    // in this method.
    final AccountSettingsEntity parentAccountSettingsEntity =
      accountSettingsRepository.safeFindByAccountId(accountId);

    final Link<?> link = this.getLinkManager().getOrCreateLink(
      conversionService.convert(parentAccountSettingsEntity, AccountSettings.class)
    );

    // Construct a lambda that implements the Fetch logic for IL-DCP.
    IldcpFetcher ildcpFetcher = ildcpRequest -> {
      Objects.requireNonNull(ildcpRequest);

      final IldcpRequestPacket ildcpRequestPacket = IldcpRequestPacket.builder().build();
      final InterledgerPreparePacket preparePacket =
        InterledgerPreparePacket.builder().from(ildcpRequestPacket).build();

      // Fetch the IL-DCP response using the Link.
      return link.sendPacket(preparePacket)
        .map(
          // If FulfillPacket...
          IldcpUtils::toIldcpResponse,
          // If Reject Packet...
          (interledgerRejectPacket) -> {
            throw new RuntimeException(
              String.format("IL-DCP negotiation failed! Reject: %s", interledgerRejectPacket)
            );
          }
        );
    };

    final IldcpResponse ildcpResponse = ildcpFetcher.fetch(IldcpRequest.builder().build());

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
    final AccountSettingsEntity updatedAccountSettings =
      accountSettingsRepository.save(parentAccountSettingsEntity);

    logger.info(
      "IL-DCP Succeeded! Operator Address: `{}`", connectorSettingsSupplier.get().operatorAddress()
    );

    return conversionService.convert(updatedAccountSettings, AccountSettings.class);
  }

  @Override
  @Transactional
  public void deleteByAccountId(AccountId accountId) {
    Optional<AccountSettingsEntity> entity = accountSettingsRepository.findByAccountId(accountId);
    if (!entity.isPresent()) {
      throw new AccountNotFoundProblem(accountId);
    }
    deletedAccountSettingsRepository.save(new DeletedAccountSettingsEntity(entity.get()));
    accountSettingsRepository.delete(entity.get());
  }

  private void validateAccountIdFormat(AccountId accountId) {
    Preconditions.checkArgument(!accountId.value().contains(":"), "Account id cannot contain a colon");
    Preconditions.checkArgument(CharMatcher.ascii().matchesAllOf(accountId.value()),
      "Account id must be ascii");
  }

}
