/*
 * Copyright 2023-2024 the original author or authors.
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

import java.util.List;

import org.springframework.vault.core.VaultKeyValueOperationsSupport.KeyValueBackend;
import org.springframework.vault.support.VaultResponseSupport;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;

/**
 * Support class to build accessor methods for the Vault key-value backend version 2.
 *
 * @author Timothy R. Weiand
 * @author Mark Paluch
 * @since 3.1
 * @see KeyValueBackend#KV_2
 */
abstract class ReactiveVaultKeyValue2Accessor extends ReactiveVaultKeyValueAccessor {

	private final String path;

	/**
	 * Create a new {@link ReactiveVaultKeyValue2Accessor} given {@link VaultOperations}
	 * and the mount {@code path}.
	 * @param reactiveVaultOperations must not be {@literal null}.
	 * @param path must not be empty or {@literal null}.
	 */
	ReactiveVaultKeyValue2Accessor(ReactiveVaultOperations reactiveVaultOperations, String path) {

		super(reactiveVaultOperations, path);

		this.path = path;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<String> list(String path) {

		return doRead(
				String.format("%s?list=true", createBackendPath("metadata", KeyValueUtilities.normalizeListPath(path))),
				VaultListResponse.class)
			.flatMapMany(response -> {

				List<String> list = (List<String>) response.getRequiredData().get("keys");
				return null == list ? Flux.empty() : Flux.fromIterable(list);
			});
	}

	@Override
	public KeyValueBackend getApiVersion() {
		return KeyValueBackend.KV_2;
	}

	@Override
	JsonNode getJsonNode(VaultResponseSupport<JsonNode> response) {
		return response.getRequiredData().at("/data");
	}

	@Override
	String createDataPath(String path) {
		return createBackendPath("data", path);
	}

	String createBackendPath(String segment, String path) {
		return String.format("%s/%s/%s", this.path, segment, path);
	}

}
