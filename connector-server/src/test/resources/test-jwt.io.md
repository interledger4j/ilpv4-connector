This file contains information that can be plugged-into https://jwt.io in order to experiment with token-types 
currently used by the ILP-over-HTTP protocol in this implementation:

# Headers
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

# Payload
```json
{
  "sub": "alice",
  "aud": "https://connie.example.com/",
  "iss": "https://alice.example.com/",
  "iat": 1556751813
}
```
