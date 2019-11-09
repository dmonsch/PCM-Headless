# Get State
Returns the state of a specific simulation.

URL : `/rest/{id}/state`

Parameter : `{id}` - The ID of the specific simulation

Method : `GET`

Permissions required : None

## Success Response
Code : `200 OK`

**Content Examples**

If the request succeeded, the state of the simulation is returned.
The status equals one of the following values: "ready", "queued", "running", "executed", "finished", "failed".
The difference between the status "executed" and "finished" is that with "executed" post processing steps are still performed. If the status is "finished", however, the simulation results have already been successfully processed.

Example 1

```json
"ready"
```

Example 2

```json
"finished"
```

## Notes