package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.admin;

import com.sappenin.interledger.ilpv4.connector.server.spring.controllers.model.problems.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.ilpv4.connector.persistence.entities.AccountBalanceSettingsEntity;
import org.interledger.ilpv4.connector.persistence.entities.AccountRateLimitSettingsEntity;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.springframework.core.convert.ConversionService;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.admin.AccountsController.SLASH_ACCOUNTS;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Allows an admin to operate on Accounts in this Connector.
 */
@RestController(SLASH_ACCOUNTS)
public class AccountsController {

  public static final String SLASH_ACCOUNTS = "/accounts";
  public static final String SLASH_ACCOUNT_ID = "/{accountId}";

  private final ConversionService conversionService;
  private final AccountSettingsRepository accountSettingsRepository;

  public AccountsController(
    final ConversionService conversionService,
    final AccountSettingsRepository accountSettingsRepository
  ) {
    this.conversionService = conversionService;
    this.accountSettingsRepository = accountSettingsRepository;
  }

  /**
   * Create a new Account in this server.
   *
   * @param accountSettings
   *
   * @return
   */
  @RequestMapping(
    path = SLASH_ACCOUNTS, method = RequestMethod.POST,
    consumes = {APPLICATION_JSON_VALUE},
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public HttpEntity<Resource<AccountSettings>> createAccount(
    @RequestBody final AccountSettings.AbstractAccountSettings accountSettings
  ) {
    Objects.requireNonNull(accountSettings);

    AccountSettingsEntity accountSettingsEntity = new AccountSettingsEntity(accountSettings);

    final AccountSettings returnableAccountSettings = this.conversionService.convert(
      this.accountSettingsRepository.save(accountSettingsEntity), AccountSettings.class
    );

    final Link selfLink =
      linkTo(methodOn(AccountsController.class).getAccount(returnableAccountSettings.getAccountId())).withSelfRel();

    final Resource resource = new Resource(
      returnableAccountSettings,
      selfLink
    );

    HttpHeaders headers = new HttpHeaders();
    Link selfRel = linkTo(AccountsController.class).slash(accountSettings.getAccountId().value()).withSelfRel();
    headers.setLocation(URI.create(selfRel.getHref()));

    return new ResponseEntity(resource, headers, HttpStatus.CREATED);
  }

  /**
   * Create a new Account in this server.
   *
   * @return
   */
  @RequestMapping(method = RequestMethod.GET, produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE})
  public HttpEntity<Resources<AccountSettings>> getAccounts() {

    // TODO: Add paging.
    final List<Resource<AccountSettings>> resources =
      StreamSupport.stream(this.accountSettingsRepository.findAll().spliterator(), false)
        .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class))
        .map(this::toResource)
        .collect(Collectors.toList());

    final PagedResources pagedResources = PagedResources.wrap(
      resources,
      // TODO: Connect these numbers to spring-data paging result.
      new PagedResources.PageMetadata(resources.size(), 0, resources.size())
    );

    final HttpEntity httpEntity = new HttpEntity(pagedResources);
    return httpEntity;
  }

  /**
   * Create a new Account in this server.
   *
   * @return
   */
  @RequestMapping(
    path = SLASH_ACCOUNTS + SLASH_ACCOUNT_ID, method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public Resource<AccountSettings> getAccount(
    @PathVariable("accountId") final AccountId accountId
  ) {
    return accountSettingsRepository.findByAccountId(accountId)
      .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class))
      .map(this::toResource)
      .orElseThrow(() -> new AccountNotFoundProblem(accountId));
  }

  /**
   * Create a new Account in this server.
   *
   * @return
   */
  @RequestMapping(
    path = SLASH_ACCOUNTS + SLASH_ACCOUNT_ID, method = RequestMethod.PUT,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public Resource<AccountSettings> updateAccount(
    @PathVariable("accountId") final AccountId accountId,
    @RequestBody final AccountSettings.AbstractAccountSettings accountSettings
  ) {

    return accountSettingsRepository.findByAccountId(accountId)
      .map(entity -> {

        // Ignore update accountId

        entity.setAssetCode(accountSettings.getAssetCode());
        entity.setAssetScale(accountSettings.getAssetScale());
        entity.setAccountRelationship(accountSettings.getAccountRelationship());
        entity.setBalanceSettings(
          new AccountBalanceSettingsEntity(accountSettings.getBalanceSettings())
        );
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
        entity.setReceiveRoutes(accountSettings.isReceiveRoutes());
        entity.setSendRoutes(accountSettings.isSendRoutes());

        return accountSettingsRepository.save(entity);
      })
      .map(accountSettingsEntity -> conversionService.convert(accountSettingsEntity, AccountSettings.class))
      .map(this::toResource)
      .orElseThrow(() -> new AccountNotFoundProblem(accountId));
  }

  private Resource<AccountSettings> toResource(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    final Link selfLink =
      linkTo(methodOn(AccountsController.class).getAccount(accountSettings.getAccountId())).withSelfRel();
    return new Resource(accountSettings, selfLink);

  }
}