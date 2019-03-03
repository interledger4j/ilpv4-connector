# Spring Boot with TLS
Note: These steps were taken from
[Secure Spring Boot Applications with TLS and HTTP/2](https://blog.novatec-gmbh.de/spring-boot-applications-tls-http2/).

## Setting up a private Certificate Authority (CA)

### Certificate for Root CA

```
keytool -genkeypair -storetype pkcs12 -keyalg RSA -keysize 3072 -alias root-ca \
-dname "CN=My Root CA,OU=Development,O=My Organization,C=DE" \
-ext BC:c=ca:true -ext KU=keyCertSign -validity 3650 \
-keystore ./root-ca/ca.jks -storepass secret -keypass secret
```

```
keytool -exportcert -keystore ./root-ca/ca.jks -storepass secret \
-alias root-ca -rfc -file ./root-ca/ca.pem
```

### Signed Server Certificate
```
keytool -genkeypair -storetype pkcs12 -keyalg RSA -keysize 3072 \
-alias localhost -dname "CN=localhost,OU=Development,O=My Organization,C=DE" \
-ext BC:c=ca:false -ext EKU:c=serverAuth -ext "SAN:c=DNS:localhost,IP:127.0.0.1" \
-validity 3650 -keystore ./server/server.jks -storepass secret -keypass secret
```

```
keytool -certreq -keystore ./server/server.jks -storepass secret \
-alias localhost -keypass secret -file ./server/server.csr
```

```
keytool -gencert -storetype pkcs12 -keystore ./root-ca/ca.jks -storepass secret \
 -infile ./server/server.csr -alias root-ca -keypass secret \
 -ext BC:c=ca:false -ext EKU:c=serverAuth -ext "SAN:c=DNS:localhost,IP:127.0.0.1" \
 -validity 3650 -rfc -outfile ./server/server.pem
```

```
keytool -importcert -noprompt -keystore ./server/server.jks -storepass secret -alias root-ca -keypass secret -file ./root-ca/ca.pem \
keytool -importcert -noprompt -keystore ./server/server.jks -storepass secret -alias localhost -keypass secret -file ./server/server.pem
```

## Configure TLS in Spring Boot

To enable TLS put the following entries into your application.properties file.

```
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:tls/server/server.jks
server.ssl.key-store-type=PKCS12
server.ssl.key-store-password=secret
server.ssl.key-alias=localhost
server.ssl.key-password=secret
```

With these property entries you will change the following behavior:

* The application is started on port 8443 instead of port 8080 (by convention this is the usual port for HTTPS connections).
* Use our new java key store server.jks which is of type PKCS12 and is opened with given store password
* Define the alias of public/private key to use for the server certificate with corresponding key password

Important: Please do not forget to copy the java key store file server.jks you have created in previous section into the src/main/resource folder of the new spring boot application.