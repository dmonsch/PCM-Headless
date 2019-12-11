package org.pcm.headless.api.client;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.pcm.headless.api.util.PCMUtil;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PCMHeadlessClient {
	private static final long DEFAULT_TIMEOUT = 30000;

	private static boolean PCM_INITED = false;

	private static final String PING_URL = "rest/ping";
	private static final String CLEAR_URL = "rest/clear";
	private static final String PREPARE_URL = "rest/prepare";

	private String baseUrl;

	private OkHttpClient client;

	public PCMHeadlessClient(String baseUrl) {
		this.client = produceClient(DEFAULT_TIMEOUT);
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
	public boolean clear() {
		Request request = new Request.Builder().url(this.baseUrl + CLEAR_URL).build();
		try (Response response = client.newCall(request).execute()) {
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Determines whether the backend is reachable.
	 * 
	 * @param timeout the time to wait for a response
	 * @return true if the backend is reachable, false if not
	 */
	public boolean isReachable(long timeout) {
		OkHttpClient client = produceShallow(timeout);
		Request request = new Request.Builder().url(this.baseUrl + PING_URL).build();

		boolean reach;
		try (Response response = client.newCall(request).execute()) {
			reach = response.body().string().equals("{}");
		} catch (IOException e) {
			reach = false;
		}
		return reach;
	}

	/**
	 * Prepares a simulation and returns a client to configure and start it.
	 * 
	 * @return instance of {@link SimulationClient} which can be used to execute a
	 *         simulation.
	 */
	public SimulationClient prepareSimulation() {
		Request request = new Request.Builder().url(this.baseUrl + PREPARE_URL).build();
		try (Response response = client.newCall(request).execute()) {
			return new SimulationClient(this.baseUrl, response.body().string(), this.client);
		} catch (IOException e) {
			return null;
		}
	}

	private OkHttpClient produceClient(long timeout) {
		return new OkHttpClient.Builder().connectTimeout(timeout, TimeUnit.MILLISECONDS)
				.writeTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).build();
	}

	private OkHttpClient produceShallow(long timeout) {
		return client.newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS)
				.writeTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).build();
	}

}
