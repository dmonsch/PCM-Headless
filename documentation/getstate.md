# Get state
Get the states and information of all triggered simulations.

URL : `/rest/state/`

Method : `GET`

Permissions required : None

## Success Response
Code : `200 OK`

**Content Examples**

One simulation is running:

```json
{ 
   "simulations":[ 
      { 
         "id":"0f43c5d4-19eb-4e52-a254-9d4e8cdf0ae9",
         "name":"CoCoME Simulation",
         "simulator":"SimuLizar",
         "repetitions":10,
         "finishedRepetitions":5,
         "simulationTime":150000,
         "maximumMeasurementCount":10000,
         "state":"running"
      }
   ]
}
```

One simulation is running and one is finished:

```json
{ 
   "simulations":[ 
      { 
         "id":"0f43c5d4-19eb-4e52-a254-9d4e8cdf0ae9",
         "name":"CoCoME Simulation",
         "simulator":"SimuLizar",
         "repetitions":10,
         "finishedRepetitions":10,
         "simulationTime":150000,
         "maximumMeasurementCount":10000,
         "state":"executed"
      },
      { 
         "id":"b369f756-8c9c-4a74-81e2-2ae5245cccf9",
         "name":"TeaStore Simulation",
         "simulator":"SimuCom",
         "repetitions":10,
         "finishedRepetitions":0,
         "simulationTime":500000,
         "maximumMeasurementCount":50000,
         "state":"running"
      }
   ]
}
```

## Notes
This endpoint is used by the web UI to determine the states of all simulations.