package org.pcm.headless.api.client;

import org.pcm.headless.api.util.PCMUtil;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class PCMHeadlessClient {
	private static boolean PCM_INITED = false;

	private static final String PING_URL = "rest/ping";
	private static final String CLEAR_URL = "rest/clear";
	private static final String PREPARE_URL = "rest/prepare";

	private String baseUrl;

	public PCMHeadlessClient(String baseUrl) {
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
		if (!this.baseUrl.startsWith("http://")) {
			this.baseUrl = "http://" + this.baseUrl;
		}

		if (!PCM_INITED) {
			PCMUtil.loadPCMModels();
			PCM_INITED = true;
		}
	}

	/**
	 * Makes a full clean, this means all analysis results and states are removed.
	 * Be aware: this can break currently running simulations.
	 */
	public void clear() {
		try {
			Unirest.get(this.baseUrl + CLEAR_URL).asString().getBody();
		} catch (UnirestException e) {
		}
	}

	/**
	 * Determines whether the backend is reachable.
	 * 
	 * @param timeout the time to wait for a response
	 * @return true if the backend is reachable, false if not
	 */
	public boolean isReachable(long timeout) {
		Unirest.setTimeouts(timeout, timeout);
		boolean reach;
		try {
			reach = Unirest.get(this.baseUrl + PING_URL).asString().getBody().equals("{}");
		} catch (UnirestException e) {
			reach = false;
		}
		resetTimeouts();
		return reach;
	}

	/**
	 * Prepares a simulation and returns a client to configure and start it.
	 * 
	 * @return instance of {@link SimulationClient} which can be used to execute a
	 *         simulation.
	 */
	public SimulationClient prepareSimulation() {
		try {
			return new SimulationClient(this.baseUrl, Unirest.get(this.baseUrl + PREPARE_URL).asString().getBody());
		} catch (UnirestException e) {
			return null;
		}
	}

	private void resetTimeouts() {
		Unirest.setTimeouts(10000, 60000);
	}

}
