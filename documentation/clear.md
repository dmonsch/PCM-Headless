# Clear Simulation Data
Allows you to delete the data of all simulations on the backend. But note that this function can be disabled at startup. So it is possible that this endpoint might be disabled.

URL : `/rest/clear`

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
This endpoint should be used with caution, otherwise data from simulations may be lost.