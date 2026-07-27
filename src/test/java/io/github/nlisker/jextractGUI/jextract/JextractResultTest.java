/*
 * Copyright 2026 Nir Lisker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.nlisker.jextractGUI.jextract;

import static com.google.common.truth.Truth.*;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;

import io.github.nlisker.jextractGUI.ReplaceCamelCase;
import io.github.nlisker.jextractGUI.jextract.JextractResult.Status;
import io.github.nlisker.jextractGUI.model.Displayable.MainHeader;

/// Tests for the classification of jextract diagnostics into a [Status]. Does not call jextract.
@DisplayNameGeneration(ReplaceCamelCase.class)
class JextractResultTest {

	private static final MainHeader HEADER = new MainHeader(Path.of("full.h"));

	@Test
	void classifyNoErrorOutputAsSuccess() {
		assertThat(statusOf("")).isEqualTo(Status.SUCCESS);
	}

	@Test
	void classifyAWarningAsWarning() {
		assertThat(statusOf("full.h:48:17: warning: Skipping SB.w (bitfields are not supported)")).isEqualTo(Status.WARNING);
	}

	@Test
	void classifyAnErrorAsError() {
		assertThat(statusOf("include_somewhere.h:1:10: error: 'included1.h' file not found")).isEqualTo(Status.ERROR);
	}

	@Test
	void classifyAWarningAndAnErrorAsError() {
		assertThat(statusOf("""
				full.h:48:17: warning: Skipping SB.w (bitfields are not supported)
				full.h:1:10: error: 'included1.h' file not found""")).isEqualTo(Status.ERROR);
	}

	@Test
	void classifyCorrectlyWithAnAbsoluteWindowsPath() {
		assertThat(statusOf("C:\\Users\\Nir\\full.h:48:17: warning: Skipping SB.w")).isEqualTo(Status.WARNING);
	}

	@Test
	void classifyAMentionOfASeverityOutsideOfPatternAsSuccess() {
		assertThat(statusOf("full.h:12:5: placeholder: a mention of error and warning")).isEqualTo(Status.SUCCESS);
	}

	private static Status statusOf(String errorOutput) {
		return JextractResult.ofErrorOutput(HEADER, errorOutput).status();
	}
}