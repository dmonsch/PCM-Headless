# Add Models to the Simulation
Pushes PCM model parts to the backend, to simulate them there. Please read the notes to make sure that you use this endpoint correctly.

URL : `/rest/{id}/set/{type}`

Parameter :
* `{id}` - The ID of the specific simulation
* `{type}` - The type of the model (see below)
* `file` - POST parameter containing a multipart file (see [https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html](https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html))

Method : `GET`

Permissions required : None

## Model Type
There are 7 different types of a model (the corresponding value is written in brackets behind them):
* Component Repository Model(**"repository"**) - you can push multiple of these
* System Model (**"system"**) - you can push only one (replacing mechanism)
* Usage Model (**"usagemodel"**) - you can push only one (replacing mechanism)
* Allocation Model (**"allocation"**) - you can push only one (replacing mechanism)
* Resource Environment Model (**"resourceenv"**) - you can push only one (replacing mechanism)
* Monitor Repository Model(**"monitor"**) - you can push only one (replacing mechanism)
* Additional Model Parts (**"addit"**) - you can push as many as you like

## Success Response
Code : `200 OK`

**Content Examples**

The endpoint does not return anything in case of success.

## Notes
There are some problems concerning the models that can occur:
* Models often contain absolute paths. These are only valid on a particular system and should be replaced by relative paths in advance.
* Attention must be paid to ensure that all referenced models are also transferred to the backend, otherwise errors may occur during simulation.

If you have problems using the REST interface, use the ready-to-use API implementation that takes care of these things for you (see [Usage](https://github.com/dmonsch/PCM-Headless/wiki/Usage)).