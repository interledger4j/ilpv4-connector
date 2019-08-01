# test-jwt.io

This file contains information that can be plugged-into [https://jwt.io](https://jwt.io) in order to experiment with token-types currently used by the BLAST \(ILP-over-HTTP\) protocol in this implementaiton:

## Headers

```javascript
{
  "alg": "HS256",
  "typ": "JWT"
}
```

## Payload

```javascript
{
  "sub": "alice",
  "aud": "https://connie.example.com/",
  "iss": "https://alice.example.com/",
  "iat": 1556751813
}
```

