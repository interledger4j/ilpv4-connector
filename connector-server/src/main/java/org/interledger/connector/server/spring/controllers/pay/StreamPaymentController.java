package org.interledger.connector.server.spring.controllers.pay;

import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS_PAYMENTS_PATH;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.payments.SendPaymentRequest;
import org.interledger.connector.payments.SendPaymentService;
import org.interledger.connector.payments.StreamPayment;
import org.interledger.connector.payments.StreamPaymentManager;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.Optional;

@RestController
public class StreamPaymentController {

  private final SendPaymentService sendPaymentService;
  private final StreamPaymentManager streamPaymentManager;

  public StreamPaymentController(SendPaymentService sendPaymentService, StreamPaymentManager streamPaymentManager) {
    this.sendPaymentService = sendPaymentService;
    this.streamPaymentManager = streamPaymentManager;
  }

  @RequestMapping(
    value = SLASH_ACCOUNTS_PAYMENTS_PATH + "/{paymentId}", method = {RequestMethod.GET},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public Optional<StreamPayment> findPaymentById(
    @PathVariable("accountId") AccountId accountId,
    @PathVariable("paymentId") String paymentId
  ) {
    return streamPaymentManager.findByAccountIdAndStreamPaymentId(accountId, paymentId);
  }

  @RequestMapping(
    value = SLASH_ACCOUNTS_PAYMENTS_PATH, method = {RequestMethod.GET},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public ListStreamPaymentsResponse listPayments(
    @PathVariable("accountId") AccountId accountId,
    @RequestParam(value = "page", defaultValue = "0") int page
  ) {
    PageRequest pageRequest = PageRequest.of(page, 100);
    return ListStreamPaymentsResponse.builder()
        .payments(streamPaymentManager.findByAccountId(accountId, pageRequest))
      .pageSize(pageRequest.getPageSize())
      .pageNumber(pageRequest.getPageNumber())
      .build();
  }

  /**
   * Sends payment from the given account.
   *
   * @param accountId      The ILP Connector account identifier for this request.
   * @param paymentRequest A {@link LocalSendPaymentRequest}.
   * @return payment result
   */
  @RequestMapping(
    value = SLASH_ACCOUNTS_PAYMENTS_PATH, method = {RequestMethod.POST},
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public LocalSendPaymentResponse sendPayment(
    @PathVariable("accountId") AccountId accountId,
    @RequestBody LocalSendPaymentRequest paymentRequest
  ) {
    return LocalSendPaymentResponse.builder().from(sendPaymentService.sendMoney(
      SendPaymentRequest.builder()
        .accountId(accountId)
        .amount(paymentRequest.amount())
        .destinationPaymentPointer(paymentRequest.destinationPaymentPointer())
        .build()
    )).build();
  }

}
