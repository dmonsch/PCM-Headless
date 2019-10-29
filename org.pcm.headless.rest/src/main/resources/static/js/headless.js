MicroModal.init();

$(document).ready(function() {
	pollUpdates();
	setInterval(pollUpdates, 2000);
});

function pollUpdates() {
	$.getJSON("/rest/state", function(data) {
		$("#table-body").empty();

		data.simulations.forEach(function(sim) {
			var row = createRow(sim);
			$("#table-body").append(row);
			registerEvents(sim.id);
		});
	});
}

// events
function clearSimulation(id) {
	$.ajax({
		url : "/rest/" + id + "/clear"
	}).done(function() {
		console.log("Cleaned up simulation with id '" + id + "'.");
		pollUpdates();
	});
}

function executeSimulation(id) {
	$.ajax({
		url : "/rest/" + id + "/start"
	}).done(function() {
		console.log("Queued simulation with id '" + id + "'.");
		pollUpdates();
	});
}

function showSimulationResults(id) {
	$.getJSON("/rest/" + id + "/results", function(data) {
		console.log(data);
		$("#modal-1-content p").html(
				"The results consist of <b>" + data.values.length
						+ "</b> measurements.");
		$("#modal-1-download").click(function() {
			downloadJSONResults('/rest/' + id + '/results');
		});

		MicroModal.show('modal-1'); // [1]
	});
}

function downloadJSONResults(url) {
	fetch(url)
	  .then(resp => resp.blob())
	  .then(blob => {
	    const url = window.URL.createObjectURL(blob);
	    const a = document.createElement('a');
	    a.style.display = 'none';
	    a.href = url;
	    // the filename you want
	    a.download = 'results.json';
	    document.body.appendChild(a);
	    a.click();
	    window.URL.revokeObjectURL(url);
	    console.log("Results downloaded succesfully.");
	  })
	  .catch(() => console.log('Failed to download the results.'));
}

// helpers
function createRow(data) {
	res = "<tr>";
	res += createTD('<i class="fa mr-2 ' + getIcon(data.state) + '"></i>'
			+ data.state);
	res += createTD(compressName(data.name));
	res += createTD(data.simulator);
	res += createTD(data.finishedRepetitions + " / " + data.repetitions);
	res += createTD(data.simulationTime);
	res += createTD(data.maximumMeasurementCount);
	res += createTD(createClearButton(data.id, data.state));
	res += createTD(createResultsButton(data.id, data.state));
	res += createTD(createExecutionButton(data.id, data.state));
	res += "</tr>";

	return res;
}

function registerEvents(id) {
	$("#clear-" + id).click(function() {
		clearSimulation(id);
	});

	$("#results-" + id).click(function() {
		showSimulationResults(id);
	});

	$("#execute-" + id).click(function() {
		executeSimulation(id);
	});
}

function createClearButton(id, state) {
	return '<input class="btn btn-sm btn-primary" type="button"'
			+ (state === "running" ? ' disabled' : '') + ' id="clear-' + id
			+ '" value="Clean up">';
}

function createResultsButton(id, state) {
	return '<input class="btn btn-sm btn-primary" type="button"'
			+ (state !== "executed" ? ' disabled' : '') + ' id="results-' + id
			+ '" value="Show Results">';
}

function createExecutionButton(id, state) {
	return '<input class="btn btn-sm btn-primary" type="button"'
			+ (state !== "ready" ? ' disabled' : '') + ' id="execute-' + id
			+ '" value="Execute">';
}

function compressName(name) {
	subStr = name.substring(0, 15);
	if (subStr.length < name.length) {
		subStr += "...";
	}
	return subStr;
}

function createTD(inner) {
	return '<td class="align-middle">' + inner + "</td>";
}

function getIcon(state) {
	if (state === "executed") {
		return "fa-check color-success";
	} else if (state === "queued") {
		return "fa-spinner color-warning";
	} else if (state === "running") {
		return "fa-tasks color-running";
	} else if (state === "ready") {
		return "fa-pause color-gray";
	} else if (state === "finished") {
		return "fa-paper-plane color-success";
	} else if (state === "failed") {
		return "fa-times color-error";
	}
}