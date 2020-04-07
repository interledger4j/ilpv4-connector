package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.entities.TransactionEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;

public class TransactionsRepositoryCustomImpl implements TransactionsRepositoryCustom {

  private static final String UPSERT = "INSERT INTO transactions " +
    "(account_id, amount, asset_code, asset_scale, destination_address, packet_count, reference_id, source_address, " +
    "status, type) values " +
    "(:accountId, :amount, :assetCode, :assetScale, :destinationAddress, :packetCount, :referenceId, :sourceAddress, " +
    ":status, :type) " +
    "ON CONFLICT(account_id, reference_id) DO " +
    "UPDATE SET amount=transactions.amount + excluded.amount, " +
    "  modified_dttm=now(), " +
    "  packet_count=transactions.packet_count+excluded.packet_count";

  private static final String UPDATE_STATUS = "UPDATE transactions SET status = :status, modified_dttm=now() " +
      "WHERE account_id = :accountId AND reference_id = :referenceId";

  private static final String UPDATE_SOURCE_ADDRESS =
    "UPDATE transactions SET source_address = :sourceAddress, modified_dttm=now() " +
    "WHERE account_id = :accountId AND reference_id = :referenceId";

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
    parameters.put("referenceId", transaction.getReferenceId());
    parameters.put("sourceAddress", transaction.getSourceAddress());
    parameters.put("status", transaction.getStatus());
    parameters.put("type", transaction.getType());

    entityManager.clear();
    return jdbcTemplate.update(UPSERT, parameters);
  }

  @Override
  public int updateStatus(AccountId accountId, String referenceId, String status) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("accountId", accountId.value());
    parameters.put("referenceId", referenceId);
    parameters.put("status", status);

    entityManager.clear();
    return jdbcTemplate.update(UPDATE_STATUS, parameters);
  }

  @Override
  public int updateSourceAddress(AccountId accountId, String referenceId, String sourceAddress) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("accountId", accountId.value());
    parameters.put("referenceId", referenceId);
    parameters.put("sourceAddress", sourceAddress);

    entityManager.clear();
    return jdbcTemplate.update(UPDATE_SOURCE_ADDRESS, parameters);
  }

}
