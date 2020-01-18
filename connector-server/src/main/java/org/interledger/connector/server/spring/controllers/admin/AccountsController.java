package org.interledger.connector.server.spring.controllers.admin;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountManager;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.server.spring.controllers.PathConstants;
import org.interledger.connector.server.spring.settings.Redactor;

import org.springframework.core.convert.ConversionService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
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

/**
 * Allows an admin to operate on Accounts in this Connector.
 */
@RestController(PathConstants.SLASH_ACCOUNTS)
public class AccountsController {

  private final AccountManager accountManager;
  private final ConversionService conversionService;
  private final Redactor redactor;

  public AccountsController(final AccountManager accountManager,
                            final ConversionService conversionService,
                            final Redactor redactor) {
    this.accountManager = Objects.requireNonNull(accountManager);
    this.conversionService = Objects.requireNonNull(conversionService);
    this.redactor = Objects.requireNonNull(redactor);
  }

  /**
   * Create a new Account in this server.
   *
   * @param accountSettings The {@link AccountSettings} to create in this Connector.
   * @return An {@link HttpEntity} that contains a {@link EntityModel} that contains the created {@link
   *   AccountSettings}.
   */
  @RequestMapping(
    path = PathConstants.SLASH_ACCOUNTS,
    method = RequestMethod.POST,
    consumes = {APPLICATION_JSON_VALUE},
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public HttpEntity<EntityModel<AccountSettings>> createAccount(
    @RequestBody final AccountSettings.AbstractAccountSettings accountSettings
  ) {
    Objects.requireNonNull(accountSettings);

    final AccountSettings returnableAccountSettings =
      redactor.redact(this.accountManager.createAccount(accountSettings));
    final Link selfLink =
      linkTo(methodOn(AccountsController.class).getAccount(returnableAccountSettings.accountId())).withSelfRel();
    final EntityModel resource = new EntityModel(returnableAccountSettings, selfLink);

    final HttpHeaders headers = new HttpHeaders();
    final Link selfRel = linkTo(AccountsController.class).slash(accountSettings.accountId().value()).withSelfRel();
    headers.setLocation(URI.create(selfRel.getHref()));

    return new ResponseEntity(resource, headers, HttpStatus.CREATED);
  }

  /**
   * Obtain a pageable collection of accounts on this connector.
   *
   * @return A {@link HttpEntity} that contains a {@link CollectionModel} that contains an {@link AccountSettings} for
   *   each account that exist on this Connector.
   */
  @RequestMapping(
    path = PathConstants.SLASH_ACCOUNTS,
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public HttpEntity<PagedModel<EntityModel<AccountSettings>>> getAccounts() {

    // TODO: Add paging per https://github.com/interledger4j/java-ilpv4-connector/issues/404
    final List<EntityModel<AccountSettings>> accountSettingsResources = accountManager.getAccounts()
      .stream()
      .map(redactor::redact)
      .map(this::toEntityModel)
      .collect(Collectors.toList());

    PagedModel<EntityModel<AccountSettings>> pagedCollectionModel = new PagedModel(
      accountSettingsResources,
      // TODO: Connect these numbers to spring-data paging result per #404 above.
      new PageMetadata(accountSettingsResources.size(), 0, accountSettingsResources.size())
    );

    return new HttpEntity(pagedCollectionModel);
  }

  /**
   * Get an account on this connector by its unique identifier.
   *
   * @param accountId The {@link AccountId} for the account to retrieve.
   * @return An {@link HttpEntity} that contains a {@link EntityModel} that contains the created {@link
   *   AccountSettings}.
   */
  @RequestMapping(
    path = PathConstants.SLASH_ACCOUNTS + PathConstants.SLASH_ACCOUNT_ID,
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public EntityModel<AccountSettings> getAccount(
    @PathVariable(PathConstants.ACCOUNT_ID) final AccountId accountId
  ) {
    return accountManager.findAccountById(accountId)
      .map(redactor::redact)
      .map(this::toEntityModel)
      .orElseThrow(() -> new AccountNotFoundProblem(accountId));
  }

  /**
   * Create a new Account in this server.
   *
   * @param accountId       The {@link AccountId} for the account to update.
   * @param accountSettings An {@link AccountSettings} containing information to update the account with.
   * @return An {@link HttpEntity} that contains a {@link EntityModel} that contains the created {@link
   *   AccountSettings}.
   */
  @RequestMapping(
    path = PathConstants.SLASH_ACCOUNTS + PathConstants.SLASH_ACCOUNT_ID,
    method = RequestMethod.PUT,
    consumes = {APPLICATION_JSON_VALUE},
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public EntityModel<AccountSettings> updateAccount(
    @PathVariable(PathConstants.ACCOUNT_ID) final AccountId accountId,
    @RequestBody final AccountSettings.AbstractAccountSettings accountSettings
  ) {
    return toEntityModel(redactor.redact(accountManager.updateAccount(accountId, accountSettings)));
  }

  @RequestMapping(
    path = PathConstants.SLASH_ACCOUNTS + PathConstants.SLASH_ACCOUNT_ID,
    method = RequestMethod.DELETE,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public ResponseEntity deleteAccountById(@PathVariable(PathConstants.ACCOUNT_ID) final AccountId accountId) {
    this.accountManager.deleteByAccountId(accountId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private EntityModel<AccountSettings> toEntityModel(final AccountSettings accountSettings) {
    Objects.requireNonNull(accountSettings);

    final Link selfLink =
      linkTo(methodOn(AccountsController.class).getAccount(accountSettings.accountId())).withSelfRel();
    return new EntityModel(accountSettings, selfLink);

  }
}
