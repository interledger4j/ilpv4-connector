package org.interledger.connector.payments;

import static org.slf4j.LoggerFactory.getLogger;

import org.interledger.connector.accounts.AccountManager;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.localsend.LocalPacketSwitchLink;
import org.interledger.connector.localsend.LocalPacketSwitchLinkFactory;
import org.interledger.connector.localsend.LocalPacketSwitchLinkSettings;
import org.interledger.connector.problems.payments.PaymentAlreadyExistsProblem;
import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.SpspClient;
import org.interledger.stream.Denomination;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.calculators.ExchangeRateCalculator;
import org.interledger.stream.sender.FixedSenderAmountPaymentTracker;
import org.interledger.stream.sender.StreamSender;

import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.annotation.PreDestroy;

public class DefaultSendPaymentService implements SendPaymentService {

  private static final Logger LOGGER = getLogger(DefaultSendPaymentService.class);

  private final StreamSenderFactory streamSenderFactory;
  private final SpspClient spspClient;
  private final ExchangeRateCalculator exchangeRateCalculator;
  private final Supplier<InterledgerAddress> operatorAddressSupplier;
  private final StreamPaymentManager streamPaymentManager;
  private final AccountManager accountManager;
  private final LocalPacketSwitchLinkFactory localPacketSwitchLinkFactory;
  private final ExecutorService executorService;

  public DefaultSendPaymentService(StreamSenderFactory streamSenderFactory,
                                   SpspClient spspClient, ExchangeRateCalculator exchangeRateCalculator,
                                   Supplier<InterledgerAddress> operatorAddressSupplier,
                                   StreamPaymentManager streamPaymentManager,
                                   AccountManager accountManager,
                                   LocalPacketSwitchLinkFactory localPacketSwitchLinkFactory,
                                   int maxConcurrentPackets) {
    this.streamSenderFactory = streamSenderFactory;
    this.spspClient = spspClient;
    this.exchangeRateCalculator = exchangeRateCalculator;
    this.operatorAddressSupplier = operatorAddressSupplier;
    this.streamPaymentManager = streamPaymentManager;
    this.accountManager = accountManager;
    this.localPacketSwitchLinkFactory = localPacketSwitchLinkFactory;
    this.executorService = Executors.newFixedThreadPool(maxConcurrentPackets);
  }

  @PreDestroy
  public void onDestroy() {
    executorService.shutdown();
  }

  @Override
  public StreamPayment sendMoney(SendPaymentRequest request) {
    AccountSettings settings = accountManager.findAccountById(request.accountId())
      .orElseThrow(() -> new AccountNotFoundProblem(request.accountId()));

    StreamConnectionDetails connectionDetails =
      spspClient.getStreamConnectionDetails(PaymentPointer.of(request.destinationPaymentPointer()));

    StreamPayment placeHolder = newStreamPaymentPlaceholder(request, settings, connectionDetails.destinationAddress());

    Optional<StreamPayment> maybeExistingPayment =
      streamPaymentManager.findByAccountIdAndStreamPaymentId(request.accountId(), placeHolder.streamPaymentId());
    if (maybeExistingPayment.isPresent()) {
      StreamPayment existingPayment = maybeExistingPayment.get();
      if (isDuplicateSend(existingPayment, request.amount(), connectionDetails.destinationAddress())) {
        return existingPayment;
      } else {
        throw new PaymentAlreadyExistsProblem(request.accountId(), placeHolder.streamPaymentId());
      }
    }

    Duration timeout = Duration.ofSeconds(50);
    try {
      LocalPacketSwitchLinkSettings linkSettings = LocalPacketSwitchLinkSettings.builder()
        .accountId(request.accountId())
        .sharedSecret(connectionDetails.sharedSecret())
        .build();

      LocalPacketSwitchLink link = localPacketSwitchLinkFactory.constructLink(operatorAddressSupplier, linkSettings);

      StreamSender sender = streamSenderFactory.newStreamSender(link, executorService);

      SendMoneyRequest sendMoneyRequest = SendMoneyRequest.builder()
        .amount(request.amount())
        .denomination(Denomination.builder().assetCode(settings.assetCode())
          .assetScale((short) settings.assetScale())
          .build())
        .paymentTracker(new FixedSenderAmountPaymentTracker(request.amount(), exchangeRateCalculator))
        .sharedSecret(connectionDetails.sharedSecret())
        .destinationAddress(connectionDetails.destinationAddress())
        .timeout(timeout)
        .requestId(UUID.randomUUID())
        .sourceAddress(operatorAddressSupplier.get())
        .build();

      streamPaymentManager.merge(placeHolder);

      sender.sendMoney(sendMoneyRequest).get(timeout.getSeconds(), TimeUnit.SECONDS);
    } catch (Exception e) {
      LOGGER.error("unexpected error sending payment, request={}.", request, e);
    }
    return streamPaymentManager.findByAccountIdAndStreamPaymentId(request.accountId(), placeHolder.streamPaymentId())
      .orElse(placeHolder);
  }

  private boolean isDuplicateSend(StreamPayment existing, UnsignedLong amount, InterledgerAddress destination) {
    return existing.type().equals(StreamPaymentType.PAYMENT_SENT) &&
      existing.expectedAmount().equals(Optional.of(amount.bigIntegerValue())) &&
      existing.destinationAddress().equals(destination);
  }

  private StreamPayment newStreamPaymentPlaceholder(SendPaymentRequest request,
                                                    AccountSettings settings,
                                                    InterledgerAddress destinationAddress) {
    return StreamPayment.builder()
      .accountId(request.accountId())
      .amount(BigInteger.ZERO)
      .expectedAmount(request.amount().bigIntegerValue().negate())
      .deliveredAmount(UnsignedLong.ZERO)
      .createdAt(Instant.now())
      .modifiedAt(Instant.now())
      .assetScale((short) settings.assetScale())
      .assetCode(settings.assetCode())
      .packetCount(0)
      .destinationAddress(destinationAddress)
      .status(StreamPaymentStatus.PENDING)
      .type(StreamPaymentType.PAYMENT_SENT)
      .build();
  }

}
