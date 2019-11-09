# Clear specific Simulation Data
Removes the simulation data of **one specified** simulation.

URL : `/rest/{id}/clear`

Parameter : `{id}` - The ID of the specific simulation

Method : `GET`

Permissions required : None

## Success Response
Code : `200 OK`

**Content Examples**

The request returns a simple JSON object indicating whether the deletion was successful or not. If the deletion was not successful, this is usually because the simulation with the given ID was not found.

Example 1 (Failure)

```json
{
	"success" : false
}
```

Example 2 (Success)

```json
{
	"success" : true
}
```

## Notes
This endpoint should be used with caution, otherwise data from simulations may be lost.