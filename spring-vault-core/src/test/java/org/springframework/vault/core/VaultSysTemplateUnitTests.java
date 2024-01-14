/*
 * Copyright 2019-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link VaultSysTemplate}.
 *
 * @author Mark Paluch
 */
class VaultSysTemplateUnitTests {

	@Test
	void shouldReportRecoveryReplicationMode() {

		VaultSysTemplate.VaultHealthImpl disabled = new VaultSysTemplate.VaultHealthImpl(true, true, true, true,
				"disabled", 0, null);

		assertThat(disabled.isRecoveryReplicationSecondary()).isFalse();

		VaultSysTemplate.VaultHealthImpl enabled = new VaultSysTemplate.VaultHealthImpl(true, true, true, true,
				"enabled", 0, null);

		assertThat(enabled.isRecoveryReplicationSecondary()).isTrue();
	}

}
