/*
 * Copyright 2018-2024 the original author or authors.
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

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.vault.authentication.LifecycleAwareSessionManagerSupport.FixedTimeoutRefreshTrigger;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link LifecycleAwareSessionManagerSupport} .
 *
 * @author Mark Paluch
 */
class LifecycleAwareSessionManagerSupportUnitTests {

	@Test
	void shouldScheduleNextExecutionTimeCorrectly() {

		FixedTimeoutRefreshTrigger trigger = new FixedTimeoutRefreshTrigger(5, TimeUnit.SECONDS);

		Instant nextExecutionTime = trigger.nextExecution(LoginToken.of("foo".toCharArray(), Duration.ofMinutes(1)));
		assertThat(nextExecutionTime).isBetween(Instant.now().plusSeconds(52), Instant.now().plusSeconds(56));
	}

	@Test
	void shouldScheduleNextExecutionIfValidityLessThanTimeout() {

		FixedTimeoutRefreshTrigger trigger = new FixedTimeoutRefreshTrigger(5, TimeUnit.SECONDS);

		Instant nextExecutionTime = trigger.nextExecution(LoginToken.of("foo".toCharArray(), Duration.ofSeconds(2)));
		assertThat(nextExecutionTime).isBetween(Instant.now(), Instant.now().plusSeconds(2));
	}

}
