package org.interledger.connector.server.spring.controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountManager;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.crypto.ConnectorEncryptionService;
import org.interledger.connector.server.ConnectorServerConfig;
import org.interledger.connector.server.client.ConnectorAdminTestClient;
import org.interledger.crypto.EncryptedSecret;

import com.google.common.collect.Lists;
import feign.RequestInterceptor;
import feign.auth.BasicAuthRequestInterceptor;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Base64;
import java.util.List;

/**
 * Running-server test that validates behavior of account settings resource.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test", "jks"})
public class EncryptionSpringBootTest {

  private static final EncryptedSecret ENCRYPTED = EncryptedSecret.fromEncodedValue("enc:jks:file:foo:1:aes_gcm:BBBB");

  private static final String PASSWORD = "password";
  private static final String ADMIN = "admin";

  private static final String INCOMING_SECRET = Base64.getEncoder().encodeToString("shh".getBytes());
  private static final String OUTGOING_SECRET = Base64.getEncoder().encodeToString("hush".getBytes());

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @MockBean
  private ConnectorEncryptionService connectorEncryptionServiceMock;

  @MockBean
  private AccountManager accountManagerMock;


  @Autowired
  private TestRestTemplate restTemplate;

  @Value("${interledger.connector.adminPassword}")
  private String adminPassword;

  private ConnectorAdminTestClient adminApiTestClient;

  private BasicJsonTester jsonTester = new BasicJsonTester(getClass());

  @LocalServerPort
  private int localServerPort;

  @Before
  public void setUp() {

    final HttpUrl baseHttpUrl = HttpUrl.parse("http://localhost:" + localServerPort);
    final RequestInterceptor basicAuthRequestInterceptor = new BasicAuthRequestInterceptor("admin", adminPassword);
    adminApiTestClient = ConnectorAdminTestClient.construct(baseHttpUrl, basicAuthRequestInterceptor);
  }

  @Test
  public void testEncrypt() {
    String message = "foo";
    when(connectorEncryptionServiceMock.encryptWithSecret0(any())).thenReturn(ENCRYPTED);
    assertThat(adminApiTestClient.encrypt(message)).isEqualTo(ENCRYPTED.encodedValue());
  }

  @Test
  public void testRefresh() {
    List<AccountSettings> accounts = Lists.newArrayList(mock(AccountSettings.class), mock(AccountSettings.class));
    when(accountManagerMock.getAccounts()).thenReturn(accounts);
    assertThat(adminApiTestClient.refresh()).isEqualTo(2);
  }

}
