# Heartbeat
Non-functional endpoint that can be used to check whether the backend is accessible.

URL : `/rest/ping`

Method : `GET`

Permissions required : None

## Success Response
Code : `200 OK`

**Content Examples**

If the request succeeded, an empty JSON object is returned.

```json
{}
```

## Notes
This endpoint makes it possible to check the accessibility quickly and without causing any load.