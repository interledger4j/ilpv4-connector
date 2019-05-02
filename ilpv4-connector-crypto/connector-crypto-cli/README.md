# Create a Keystore
To create a Keystore that the CLI can use, issue the following command to create a new keystore with an AES-256 SecretKey inside:

```bash
> keytool -keystore ./crypto.p12 -storetype PKCS12 -genseckey -alias secret0 -keyalg aes -keysize 256
> keytool -keystore ./crypto.p12 -storetype PKCS12 -list
``` 