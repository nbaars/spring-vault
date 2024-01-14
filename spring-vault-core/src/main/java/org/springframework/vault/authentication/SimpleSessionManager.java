/*
 * Copyright 2016-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.vault.authentication;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.util.Assert;
import org.springframework.vault.support.VaultToken;

/**
 * Default implementation of {@link SessionManager}.
 * <p>
 * Uses a synchronized login method to log into Vault and reuse the resulting
 * {@link VaultToken} throughout session lifetime.
 *
 * @author Mark Paluch
 * @see ClientAuthentication
 * @see VaultToken
 */
public class SimpleSessionManager implements SessionManager {

	private final ClientAuthentication clientAuthentication;

	private final ReentrantLock lock = new ReentrantLock();

	private volatile Optional<VaultToken> token = Optional.empty();

	/**
	 * Create a new {@link SimpleSessionManager} using a {@link ClientAuthentication}.
	 * @param clientAuthentication must not be {@literal null}.
	 */
	public SimpleSessionManager(ClientAuthentication clientAuthentication) {

		Assert.notNull(clientAuthentication, "ClientAuthentication must not be null");

		this.clientAuthentication = clientAuthentication;
	}

	@Override
	public VaultToken getSessionToken() {

		if (!this.token.isPresent()) {

			this.lock.lock();
			try {
				if (!this.token.isPresent()) {
					this.token = Optional.of(this.clientAuthentication.login());
				}
			}
			finally {
				this.lock.unlock();
			}
		}

		return this.token.orElseThrow(() -> new IllegalStateException("Cannot obtain VaultToken"));
	}

}
