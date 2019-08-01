# README

## Overview

This folder contains a Command-line-interface that can be used to encrypt and encode secret values in a format that can be used by the Java Connector.

For example, imagine the Connector needs to authenticate to a database using a password with a value of `shh`. This value can be encrypted using either the Google KMS \(reccommended for productio usage\) or a Java Keystore loaded from the runtime's filesystem \(most useful for development purposes\).

In either case, the secret value \(i.e., `shh`\) will be encrypted with a particular secret-key with a particular version of that key. The CLI will encode this value so that it can be parsed properly by the connector in order to determine the correct manner to locate keys for decryption.

Here is an example of a secret value that is both encrypted _and_ encoded:

```text
enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=
```

This encoding indicates that the string represents and encoded value \(because it begins with the prefix `enc`\), was encrypted using a Java Keystore \(i.e., the `JKS` keystore type\) using a filename called `crypto.p12`, and using the key with an alias of `secret0` and the `aes_gcm` encryption algorithm \(which is a particular variant supported by the Connector\). Next, a cipher\_message encodes `cipher_text` as well as other meta-data used to decrypt the secret value \(see [here](https://proandroiddev.com/security-best-practices-symmetric-encryption-with-aes-in-java-7616beaaade9) for more details\).

## Running the CLI

To execute the CLI, perform the following steps:

1. Checkout the code and navigate to this director.
2. Build the project: `> mvn clean install`
3. Run the CLI: `> java -jar /target/ilpv4-connector-crypto-0.1.0-SNAPSHOT.jar`

## Create a Keystore

To create a Keystore that the CLI can use, issue the following command to create a new keystore with an AES-256 SecretKey inside:

```bash
> keytool -keystore ./crypto.p12 -storetype PKCS12 -genseckey -alias secret0 -keyalg aes -keysize 256
> keytool -keystore ./crypto.p12 -storetype PKCS12 -list
```

