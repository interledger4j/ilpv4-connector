package org.interledger.connector.server.spring.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.balances.AccountBalance;
import org.interledger.connector.balances.AccountBalanceResponse;
import org.interledger.connector.balances.AccountBalanceService;
import org.interledger.connector.balances.ImmutableAccountBalanceResponse;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.stream.Denomination;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


@RunWith(SpringRunner.class)
@WebMvcTest(controllers = AccountBalanceController.class)
public class AccountBalanceControllerTest extends AbstractControllerTest {

  @MockBean
  private AccountBalanceService accountBalanceService;

  @Autowired
  private MockMvc mvc;

  private ObjectMapper mapper = ObjectMapperFactory.create();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void serialize() throws JsonProcessingException {
    AccountId accountId = AccountId.of("john");

    AccountBalance balance = AccountBalance.builder()
      .accountId(accountId)
      .clearingBalance(1)
      .prepaidAmount(2)
      .build();

    Denomination denomination = Denomination.builder()
      .assetScale((short) 9)
      .assetCode("XRP")
      .build();

    AccountBalanceResponse expected = AccountBalanceResponse.builder()
      .accountBalance(balance)
      .denomination(denomination)
      .build();


    System.out.println(mapper.writeValueAsString(balance));
  }

  @Test
  public void getBalance() throws Exception {
    AccountId accountId = AccountId.of("john");
    AccountBalance balance = AccountBalance.builder()
      .accountId(accountId)
      .clearingBalance(1)
      .prepaidAmount(2)
      .build();

    Denomination denomination = Denomination.builder()
      .assetScale((short) 9)
      .assetCode("XRP")
      .build();

    AccountBalanceResponse expected = AccountBalanceResponse.builder()
      .accountBalance(balance)
      .denomination(denomination)
      .build();

    when(accountBalanceService.getAccountBalance(accountId)).thenReturn(expected);

    HttpHeaders headers = this.testJsonHeaders();

    MvcResult result = this.mvc.perform(get(accountsBalancePath(accountId))
      .headers(headers)
      .with(httpBasic("admin", "password")).with(csrf())
    )
      .andExpect(status().isOk())
      .andExpect(header().string(HeaderConstants.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
      .andReturn();


    AccountBalanceResponse response =
      mapper.readValue(result.getResponse().getContentAsString(), ImmutableAccountBalanceResponse.class);

    assertThat(response).isEqualTo(expected);
  }

  private String accountsBalancePath(AccountId accountId) {
    return PathConstants.SLASH_ACCOUNTS_BALANCE_PATH.replace("{accountId}", accountId.value());
  }
}