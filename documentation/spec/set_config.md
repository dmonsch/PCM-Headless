# Apply Simulation Configuration
This endpoint is a central point for the configuration of the simulation. Simulation time, simulation engine, number of measurements and more can be specified.

URL : `/rest/{id}/set/config`

Parameter :
* `{id}` - The ID of the specific simulation
* `configJson` - POST parameter which must contain a valid JSON string which contains the configuration

Method : `POST`

Permissions required : None

## Success Response
Code : `200 OK`

**Content Examples**

The endpoint returns nothing and therefore we include examples for the POST parameter "configJson" in the following.

Example 1 (CoCoME simulation, 1 repetition, using SimuLizar)

```json
{ 
   "experimentName":"CoCoME Simulation",
   "simulationTime":150000,
   "maximumMeasurementCount":10000,
   "useFixedSeed":false,
   "parallelizeRepetitions":false,
   "repetitions":1,
   "type":"SimuLizar",
   "simuComStoragePath":null
}
```

Example 2 (TeaStore simulation, 10 repetitions, using SimuCom)

```json
{ 
   "experimentName":"TeaStore Simulation",
   "simulationTime":500000,
   "maximumMeasurementCount":50000,
   "useFixedSeed":false,
   "parallelizeRepetitions":false,
   "repetitions":10,
   "type":"SimuCom",
   "simuComStoragePath":null
}
```

## Notes
You can always define the attribute "simuComStoragePath" in the configuration as "null", because it will be changed by the backend anyway.