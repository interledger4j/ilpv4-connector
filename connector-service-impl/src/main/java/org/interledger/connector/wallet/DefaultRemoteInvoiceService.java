package org.interledger.connector.wallet;

import static org.slf4j.LoggerFactory.getLogger;

import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.problems.InvoiceNotFoundProblem;
import org.interledger.spsp.client.SpspClientException;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.springframework.http.MediaType;

import java.io.IOException;

public class DefaultRemoteInvoiceService implements RemoteInvoiceService {

  private static final Logger LOGGER = getLogger(DefaultRemoteInvoiceService.class);

  private final OkHttpClient okHttpClient;

  private final ObjectMapper objectMapper;

  public DefaultRemoteInvoiceService(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
    this.okHttpClient = okHttpClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public Invoice getInvoice(HttpUrl invoiceUrl) {
    return execute(new Request.Builder()
      .url(invoiceUrl)
      .header("Accept", MediaType.APPLICATION_JSON_VALUE)
      .get()
      .build(), Invoice.class);
  }

  private <T> T execute(Request request, Class<T> clazz) throws SpspClientException {
    try (Response response = okHttpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new InvoiceNotFoundProblem(request.url());
      }
      return objectMapper.readValue(response.body().string(), clazz);
    } catch (IOException e) {
      LOGGER.error("failed to get invoice at url {}", request.url(), e);
      throw new InvoiceNotFoundProblem(request.url());
    }
  }
}
