package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.TransactionEntity;
import org.interledger.connector.transactions.TransactionStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;

public class TransactionsRepositoryCustomImpl implements TransactionsRepositoryCustom {

  private static final String UPSERT = "INSERT INTO transactions " +
    "(account_id, amount, asset_code, asset_scale, destination_address, packet_count, transaction_id, source_address, " +
    "status, type) values " +
    "(:accountId, :amount, :assetCode, :assetScale, :destinationAddress, :packetCount, :transactionId, :sourceAddress, " +
    ":status, :type) " +
    "ON CONFLICT(account_id, transaction_id) DO " +
    "UPDATE SET amount=transactions.amount + excluded.amount, " +
    "  modified_dttm=now(), " +
    "  packet_count=transactions.packet_count+excluded.packet_count";

  private static final String UPDATE_STATUS = "UPDATE transactions SET status = :status, modified_dttm=now() " +
      "WHERE account_id = :accountId AND transaction_id = :transactionId";

  private static final String UPDATE_SOURCE_ADDRESS =
    "UPDATE transactions SET source_address = :sourceAddress, modified_dttm=now() " +
    "WHERE account_id = :accountId AND transaction_id = :transactionId";

  @Autowired
  private NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  private EntityManager entityManager;

  @Override
  public int upsertAmounts(TransactionEntity transaction) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("accountId", transaction.getAccountId().value());
    parameters.put("amount", transaction.getAmount());
    parameters.put("assetCode", transaction.getAssetCode());
    parameters.put("assetScale", transaction.getAssetScale());
    parameters.put("destinationAddress", transaction.getDestinationAddress());
    parameters.put("packetCount", transaction.getPacketCount());
    parameters.put("transactionId", transaction.getTransactionId());
    parameters.put("sourceAddress", transaction.getSourceAddress());
    parameters.put("status", transaction.getStatus().toString());
    parameters.put("type", transaction.getType().toString());

    entityManager.clear();
    return jdbcTemplate.update(UPSERT, parameters);
  }

  @Override
  public int updateStatus(AccountId accountId, String transactionId, TransactionStatus status) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("accountId", accountId.value());
    parameters.put("transactionId", transactionId);
    parameters.put("status", status.toString());

    entityManager.clear();
    return jdbcTemplate.update(UPDATE_STATUS, parameters);
  }

  @Override
  public int updateSourceAddress(AccountId accountId, String transactionId, String sourceAddress) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("accountId", accountId.value());
    parameters.put("transactionId", transactionId);
    parameters.put("sourceAddress", sourceAddress);

    entityManager.clear();
    return jdbcTemplate.update(UPDATE_SOURCE_ADDRESS, parameters);
  }

}
