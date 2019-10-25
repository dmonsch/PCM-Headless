package org.pcm.headless.shared.data;

import lombok.Data;

@Data
public class KeyValuePair<A, B> {

	private A kex;
	private B value;

	public KeyValuePair() {
	}

	public KeyValuePair(A a, B b) {
		this.kex = a;
		this.value = b;
	}

}
