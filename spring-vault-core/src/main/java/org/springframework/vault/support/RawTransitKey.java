/*
 * Copyright 2017-2024 the original author or authors.
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
package org.springframework.vault.support;

import java.util.Map;

/**
 * A exported raw key inside Vault's {@code transit} backend.
 *
 * @author Sven Schürmann
 */
public interface RawTransitKey {

	/**
	 * @return a {@link Map} of key version to its key value.
	 */
	Map<String, String> getKeys();

	/**
	 * @return name of the key
	 */
	String getName();

}
