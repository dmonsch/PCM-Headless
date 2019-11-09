# Get Results
Allows you to retrieve the results of a simulation.

URL : `/rest/{id}/results`

Parameter : `{id}` - The ID of the specific simulation

Method : `GET`

Permissions required : None

## Success Response
Code : `200 OK`

**Content Examples**

The results are sorted and grouped according to so-called "Measuring Points".
If the simulation has not finished yet, an empty JSON object is returned.

[Example](https://github.com/dmonsch/PCM-Headless/blob/master/documentation/examples/results.json) (we did not include the results here, because they are too large)

## Notes