/*
 * Copyright 2023-2025 the original author or authors.
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
package org.springframework.vault.core;

import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.vault.support.VaultMetadataRequest;
import org.springframework.vault.support.VaultMetadataResponse;

/**
 * Default implementation of {@link ReactiveVaultKeyValueMetadataOperations}.
 *
 * @author Timothy R. Weiand
 * @author Mark Paluch
 * @since 3.1
 */
class ReactiveVaultKeyValueMetadataTemplate implements ReactiveVaultKeyValueMetadataOperations {

	private final ReactiveVaultOperations vaultOperations;

	private final String path;

	ReactiveVaultKeyValueMetadataTemplate(ReactiveVaultOperations vaultOperations, String path) {

		Assert.notNull(vaultOperations, "VaultOperations must not be null");

		this.vaultOperations = vaultOperations;
		this.path = path;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Mono<VaultMetadataResponse> get(String path) {

		return vaultOperations.read(getMetadataPath(path), Map.class).flatMap(response -> {
			Map data = response.getData();
			if (null == data) {
				return Mono.empty();
			}

			return Mono.just(data);
		}).map(KeyValueUtilities::fromMap);
	}

	@Override
	public Mono<Void> put(String path, VaultMetadataRequest body) {

		Assert.notNull(body, "Body must not be null");

		return vaultOperations.write(getMetadataPath(path), body).then();
	}

	@Override
	public Mono<Void> delete(String path) {
		return vaultOperations.delete(getMetadataPath(path));
	}

	private String getMetadataPath(String path) {

		Assert.hasText(path, "Path must not be empty");

		return this.path + "/metadata/" + path;
	}

}
