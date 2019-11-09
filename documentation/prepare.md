# Prepare Simulation
Prepares a simulation and returns the ID with which the simulation is referenced.

URL : `/rest/prepare`

Method : `GET`

Permissions required : None

## Success Response
Code : `200 OK`

**Content Examples**

If the request succeeded the ID of the simulation is returned.

Example 1:

```json
"817dc647-fdce-4ffe-807f-53d66ca6a982"
```

Example 2:

```json
"634d5e31-eb09-4d7b-8dfa-575259929204"
```

## Notes
For each new simulation, this end point must be called first.