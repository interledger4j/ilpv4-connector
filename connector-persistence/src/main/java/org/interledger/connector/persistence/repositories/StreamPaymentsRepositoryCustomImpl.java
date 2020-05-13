package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.payments.StreamPaymentStatus;
import org.interledger.connector.persistence.entities.StreamPaymentEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;

public class StreamPaymentsRepositoryCustomImpl implements StreamPaymentsRepositoryCustom {

  private static final String UPSERT = "INSERT INTO stream_payments " +
    "(account_id, amount, asset_code, asset_scale, destination_address, packet_count, stream_payment_id, " +
    "expected_amount, delivered_amount, delivered_asset_code, delivered_asset_scale, " +
    "source_address, status, type) values " +
    "(:accountId, :amount, :assetCode, :assetScale, :destinationAddress, :packetCount, :streamPaymentId, " +
    ":expectedAmount, :deliveredAmount, :deliveredAssetCode, :deliveredAssetScale, " +
    ":sourceAddress, :status, :type) " +
    "ON CONFLICT(account_id, stream_payment_id) DO " +
    "UPDATE SET amount=stream_payments.amount + excluded.amount, " +
    "  delivered_amount=stream_payments.delivered_amount + excluded.delivered_amount, " +
    "  modified_dttm=now(), " +
    "  packet_count=stream_payments.packet_count+excluded.packet_count";

  private static final String UPDATE_STATUS = "UPDATE stream_payments SET status = :status, modified_dttm=now() " +
      "WHERE account_id = :accountId AND stream_payment_id = :streamPaymentId";

  private static final String UPDATE_SOURCE_ADDRESS =
    "UPDATE stream_payments SET source_address = :sourceAddress, modified_dttm=now() " +
    "WHERE account_id = :accountId AND stream_payment_id = :streamPaymentId";

  private static final String UPDATE_DELIVERED_DENOMINATION =
    "UPDATE stream_payments SET delivered_asset_code = :deliveredAssetCode, " +
      "delivered_asset_scale = :deliveredAssetScale, " +
      "modified_dttm=now() " +
      "WHERE account_id = :accountId AND stream_payment_id = :streamPaymentId";

  @Autowired
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  private EntityManager entityManager;

  @Override
  public int upsertAmounts(StreamPaymentEntity streamPayment) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("accountId", streamPayment.getAccountId().value());
    parameters.put("amount", streamPayment.getAmount());
    parameters.put("expectedAmount", streamPayment.getExpectedAmount());
    parameters.put("assetCode", streamPayment.getAssetCode());
    parameters.put("assetScale", streamPayment.getAssetScale());
    parameters.put("destinationAddress", streamPayment.getDestinationAddress());
    parameters.put("deliveredAmount", streamPayment.getDeliveredAmount());
    parameters.put("deliveredAssetCode", streamPayment.getDeliveredAssetCode());
    parameters.put("deliveredAssetScale", streamPayment.getDeliveredAssetScale());
    parameters.put("packetCount", streamPayment.getPacketCount());
    parameters.put("streamPaymentId", streamPayment.getStreamPaymentId());
    parameters.put("sourceAddress", streamPayment.getSourceAddress());
    parameters.put("status", streamPayment.getStatus().toString());
    parameters.put("type", streamPayment.getType().toString());

    entityManager.clear();
    return jdbcTemplate.update(UPSERT, parameters);
  }

  @Override
  public int updateStatus(AccountId accountId, String streamPaymentId, StreamPaymentStatus status) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("accountId", accountId.value());
    parameters.put("streamPaymentId", streamPaymentId);
    parameters.put("status", status.toString());

    entityManager.clear();
    return jdbcTemplate.update(UPDATE_STATUS, parameters);
  }

  @Override
  public int updateSourceAddress(AccountId accountId, String streamPaymentId, String sourceAddress) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("accountId", accountId.value());
    parameters.put("streamPaymentId", streamPaymentId);
    parameters.put("sourceAddress", sourceAddress);

    entityManager.clear();
    return jdbcTemplate.update(UPDATE_SOURCE_ADDRESS, parameters);
  }

  @Override
  public int udpdateDeliveredDenomination(AccountId accountId,
                                          String streamPaymentId,
                                          String deliveredAssetCode,
                                          short deliveredAssetScale) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("accountId", accountId.value());
    parameters.put("streamPaymentId", streamPaymentId);
    parameters.put("deliveredAssetCode", deliveredAssetCode);
    parameters.put("deliveredAssetScale", deliveredAssetScale);

    entityManager.clear();
    return jdbcTemplate.update(UPDATE_DELIVERED_DENOMINATION, parameters);
  }
}
