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
    "(account_id, amount, asset_code, asset_scale, destination_address, packet_count, reference_id, status) values " +
    "(:accountId, :amount, :assetCode, :assetScale, :destinationAddress, :packetCount, :referenceId, :status) " +
    "ON CONFLICT(reference_id) DO " +
    "UPDATE SET amount=transactions.amount + excluded.amount, " +
    "  modified_dttm=now(), " +
    "  packet_count=transactions.packet_count+excluded.packet_count";

  private static final String UPDATE_STATUS = "UPDATE transactions SET status = :status, modified_dttm=now() " +
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
    parameters.put("status", transaction.getStatus());

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

}
