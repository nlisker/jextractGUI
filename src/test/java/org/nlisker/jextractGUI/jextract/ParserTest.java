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
package org.nlisker.jextractGUI.jextract;

import static com.google.common.truth.Truth.*;

import java.util.List;

import javafx.scene.control.CheckBoxTreeItem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nlisker.jextractGUI.model.Displayable;
import org.nlisker.jextractGUI.model.Displayable.Header;
import org.nlisker.jextractGUI.model.Displayable.IncludeKind;

/// Test for [Parser#populateHeaderItem(CheckBoxTreeItem)]. Calls jextract.
// TODO consider comparing with the dump includes file, which can be created with
// java -jar -Djava.library.path=lib lib\jextract.jar --dump-includes includes.txt src\test\resources\full.h
class ParserTest extends AbstractJextractTest {

	@Nested
	class WhenHeaderIsSelfContained {

		private static final String HEADER_REL_PATH = "full.h";

		// level 1
		private CheckBoxTreeItem<Displayable> mainHeaderItem;

		// level 2 self-item
		private CheckBoxTreeItem<Displayable> headerItem;

		@BeforeAll
		void parseHeader() throws Exception {
			mainHeaderItem = createMainHeaderItem(HEADER_REL_PATH);
			Parser.populateHeaderItem(mainHeaderItem);
			headerItem = (CheckBoxTreeItem<Displayable>) mainHeaderItem.getChildren().getFirst();
		}

		@Test
		void createOnlyTheSelfHeaderChild() {
			assertThat(mainHeaderItem.getChildren()).hasSize(1);
			assertThat(((Header) headerItem.getValue()).simple()).isEqualTo(HEADER_REL_PATH);
		}

		@Test
		void populateFunctions() {
			assertThat(symbolNamesFor(headerItem, IncludeKind.FUNCTION)).containsExactly("update", "update2", "varfunc");
		}

		@Test
		void populateVars() {
			assertThat(symbolNamesFor(headerItem, IncludeKind.VAR)).containsExactly("aVar", "aConst", "p", "array", "fp");
		}

		@Test
		void populateStructs() {
			assertThat(symbolNamesFor(headerItem, IncludeKind.STRUCT)).containsExactly("S", "SB", "SN");
		}

		@Test
		void populateUnions() {
			assertThat(symbolNamesFor(headerItem, IncludeKind.UNION)).containsExactly("U");
		}

		@Test
		void populateTypedefs() {
			assertThat(symbolNamesFor(headerItem, IncludeKind.TYPEDEF)).containsExactly("T");
		}

		@Test
		void populateConstants() {
			// e1 and e2 are C enum constants; jextract exposes them as top-level Declaration.Constant.
			assertThat(symbolNamesFor(headerItem, IncludeKind.CONSTANT)).containsExactly("e1", "e2", "M");
		}
	}

	@Nested
	class WhenHeaderHasReachableIncludes {

		private static final String INCLUDING = "including.h";
		private static final String INCLUDED = "included.h";

		// level 1
		private CheckBoxTreeItem<Displayable> mainHeaderItem;

		// level 2 self-item
		private CheckBoxTreeItem<Displayable> includingHeaderItem;

		// level 2 include
		private CheckBoxTreeItem<Displayable> includedHeaderItem;

		@BeforeAll
		void parseHeader() throws Exception {
			mainHeaderItem = createMainHeaderItem("same/" + INCLUDING);
			Parser.populateHeaderItem(mainHeaderItem);
			includingHeaderItem = (CheckBoxTreeItem<Displayable>) mainHeaderItem.getChildren().getFirst();
			includedHeaderItem = (CheckBoxTreeItem<Displayable>) mainHeaderItem.getChildren().getLast();
		}

		@Test
		void createTheSelfHeaderAndIncludedChildren() {
			assertThat(mainHeaderItem.getChildren()).hasSize(2);
			assertThat(((Header) includingHeaderItem.getValue()).simple()).isEqualTo(INCLUDING);
			assertThat(((Header) includedHeaderItem.getValue()).simple()).isEqualTo(INCLUDED);
		}

		@Test
		void populateVarsFromMainHeader() {
			assertThat(symbolNamesFor(includingHeaderItem, IncludeKind.VAR)).containsExactly("including1", "including2");
		}

		@Test
		void populatesVarsFromIncludedHeader() {
			assertThat(symbolNamesFor(includedHeaderItem, IncludeKind.VAR)).containsExactly("included1", "included2");
		}
	}

	// include_somewhere.h uses <included1.h> and <included2.h>, which are not on the default search path.
	// The current jextract behavior is to parse the given header without its unreachable included header. This can change in the
	// future to a failure.
	@Nested
	class WhenHeaderHasUnreachableIncludes {

//		private static final String HEADER_REL_PATH = "include_somewhere.h";
//
//		@Test
//		void failToParseHeader() {
//			CheckBoxTreeItem<Displayable> mainHeaderItem = createMainHeaderItem(HEADER_REL_PATH);
//			assertThrows(Exception.class, () -> Parser.populateHeaderItem(mainHeaderItem));
//		}

		private static final String HEADER_REL_PATH = "include_somewhere.h";

		// level 1
		private CheckBoxTreeItem<Displayable> mainHeaderItem;

		// level 2 self-item
		private CheckBoxTreeItem<Displayable> headerItem;

		@BeforeAll
		void parseHeader() throws Exception {
			mainHeaderItem = createMainHeaderItem(HEADER_REL_PATH);
			Parser.populateHeaderItem(mainHeaderItem);
			headerItem = (CheckBoxTreeItem<Displayable>) mainHeaderItem.getChildren().getFirst();
		}

		@Test
		void createOnlyTheSelfHeaderChild() {
			assertThat(mainHeaderItem.getChildren()).hasSize(1);
			assertThat(((Header) headerItem.getValue()).simple()).isEqualTo(HEADER_REL_PATH);
		}

		@Test
		void populateVarsFromMainHeader() {
			assertThat(symbolNamesFor(headerItem, IncludeKind.VAR)).containsExactly("i3");
		}
	}

	/// {@return a list of the declaration names of the given [IncludeKind] group in the given level-2 [Header] item}
	private static List<String> symbolNamesFor(CheckBoxTreeItem<Displayable> headerItem, IncludeKind kind) {
		return headerItem.getChildren().stream()
				.filter(g -> g.getValue().equals(kind))
				.flatMap(g -> g.getChildren().stream())
				.map(d -> d.getValue().simple())
				.toList();
	}
}