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
package org.springframework.vault.support;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JsonMapFlattener}.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
class JsonMapFlattenerUnitTests {

	ObjectMapper OBJECT_MAPPER = ObjectMapperSupplier.get();

	@Test
	void shouldPreserveFlatMap() {

		Map<String, Object> result = JsonMapFlattener.flatten(Collections.singletonMap("key", "value"));
		assertThat(result).containsEntry("key", "value");
	}

	@Test
	void shouldFlattenNestedObject() throws Exception {

		Map<String, Object> map = this.OBJECT_MAPPER.readValue("{\"key\": { \"nested\":true} }", Map.class);
		Map<String, Object> result = JsonMapFlattener.flatten(map);

		assertThat(result).containsEntry("key.nested", true);
	}

	@Test
	void shouldFlattenDeeplyNestedObject() throws Exception {

		Map<String, Object> map = this.OBJECT_MAPPER
			.readValue("{\"key\": { \"nested\": {\"anotherLevel\": \"value\"} } }", Map.class);
		Map<String, Object> result = JsonMapFlattener.flatten(map);

		assertThat(result).containsEntry("key.nested.anotherLevel", "value");
	}

	@Test
	void shouldFlattenNestedListOfSimpleObjects() throws Exception {

		Map<String, Object> map = this.OBJECT_MAPPER
			.readValue("{\"key\": [\"one\", \"two\"], \"dotted.key\": [\"one\", \"two\"] }", Map.class);
		Map<String, Object> result = JsonMapFlattener.flatten(map);

		assertThat(result).containsEntry("key[0]", "one").containsEntry("key[1]", "two");
		assertThat(result).containsEntry("dotted.key[0]", "one").containsEntry("dotted.key[1]", "two");
	}

	@Test
	void shouldFlattenNestedListOfComplexObject() throws Exception {

		Map<String, Object> map = this.OBJECT_MAPPER
			.readValue("{\"key\": [{ \"nested\":\"value\"}, { \"nested\":\"other-value\"}] }", Map.class);
		Map<String, Object> result = JsonMapFlattener.flatten(map);

		assertThat(result).containsEntry("key[0].nested", "value").containsEntry("key[1].nested", "other-value");
	}

	@Test
	void shouldFlattenDeeplyNestedListOfComplexObject() throws Exception {

		Map<String, Object> map = this.OBJECT_MAPPER.readValue(
				"{\"key\": { \"level1\": [{ \"nested\":\"value\"}, { \"nested\":\"other-value\"}]} }", Map.class);
		Map<String, Object> result = JsonMapFlattener.flatten(map);

		assertThat(result).containsEntry("key.level1[0].nested", "value")
			.containsEntry("key.level1[1].nested", "other-value");
	}

}
