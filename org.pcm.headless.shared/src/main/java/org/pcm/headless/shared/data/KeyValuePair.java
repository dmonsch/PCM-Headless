package org.pcm.headless.shared.data;

import lombok.Data;

@Data
public class KeyValuePair<A, B> {

	private A key;
	private B value;

	public KeyValuePair() {
	}

	public KeyValuePair(A a, B b) {
		this.key = a;
		this.value = b;
	}

}
