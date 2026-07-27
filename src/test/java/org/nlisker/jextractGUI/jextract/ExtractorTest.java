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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;
import org.nlisker.jextractGUI.model.CLOption;
import org.nlisker.jextractGUI.model.Displayable;
import org.nlisker.jextractGUI.model.Displayable.Header;
import org.nlisker.jextractGUI.model.Displayable.IncludeKind;
import org.nlisker.jextractGUI.model.Displayable.MainHeader;
import org.openjdk.jextract.Declaration;

/// Tests for [Extractor#createCommand(CheckBoxTreeItem)] and [Extractor#runCommand(CheckBoxTreeItem)]. Calls jextract.
/// To avoid test-coupling with [Parser], header trees are synthesized with [MockDeclaration] items.
class ExtractorTest extends AbstractJextractTest {

	@Nested
	@TestInstance(Lifecycle.PER_METHOD)
	class WhenHeaderIsSelfContained {

		private static final String HEADER_REL_PATH = "full.h";

		@TempDir
		private Path outputDir;

		private final CheckBoxTreeItem<Displayable> mainHeaderItem = createMainHeaderItem(HEADER_REL_PATH);
		private final MainHeader mainHeader = (MainHeader) mainHeaderItem.getValue();

		// Uses synthesized tree
		@Nested
		@TestInstance(Lifecycle.PER_METHOD)
		class CreateCommand {

			private final CheckBoxTreeItem<Displayable> varsItem = createKindGroupItem(IncludeKind.VAR, List.of("v1", "v2", "v3"));
			private final CheckBoxTreeItem<Displayable> structsItem = createKindGroupItem(IncludeKind.STRUCT, List.of("S1", "S2"));

			CreateCommand() {
				var selfHeaderItem = new CheckBoxTreeItem<Displayable>(new Header(resourcePath(HEADER_REL_PATH)));
				selfHeaderItem.getChildren().add(varsItem);
				selfHeaderItem.getChildren().add(structsItem);

				mainHeaderItem.getChildren().add(selfHeaderItem);
				mainHeaderItem.setSelected(true);
			}

			@Test
			void containingOnlyHeaderPath() {
				String command = Extractor.createCommand(mainHeaderItem);

				assertThat(command).isEqualTo(mainHeader.asOption());
			}

			@Test
			void containingHeaderOptionsWhenSpecified() {
				var className = "MyHeader";
				mainHeader.className().set(className);
				mainHeader.outputPath().set(outputDir.toString());
				mainHeader.useSystemLoadLibraries().set(true);

				String command = Extractor.createCommand(mainHeaderItem);

				assertThat(command).contains(mainHeader.asOption());
				assertThat(command).contains(CLOption.CLASS_NAME.command() + " " + className);
				assertThat(command).contains(CLOption.OUTPUT_PATH.command() + " " + outputDir);
				assertThat(command).doesNotContain("--include-");
			}

			@Test
			void containingOnlySelectedDeclarations() {
				var v1Item = (CheckBoxTreeItem<Displayable>) varsItem.getChildren().getFirst();
				v1Item.setSelected(false);
				String removedName = v1Item.getValue().asOption();

				String command = Extractor.createCommand(mainHeaderItem);

				assertThat(command).contains(mainHeader.asOption());

				TreeItem<Displayable> selfHeaderItem = mainHeaderItem.getChildren().getFirst();
				for (var kindGroupItem : selfHeaderItem.getChildren() ) {
					String kindOption = kindGroupItem.getValue().asOption();
					for (var declItem : kindGroupItem.getChildren()) {
						String name = declItem.getValue().asOption();
						if (!name.equals(removedName)) {
							assertThat(command).contains(kindOption + " " + name);
						} else {
							assertThat(command).doesNotContain(kindOption + " " + name);
						}
					}
				}
			}

			@Test
			void containingOnlySelectedKindGroupsDeclarations() {
				varsItem.setSelected(false);

				String command = Extractor.createCommand(mainHeaderItem);

				assertThat(command).contains(mainHeader.asOption());
				assertThat(command).doesNotContain(varsItem.getValue().asOption());
				for (var structItem : structsItem.getChildren()) {
					String name = structItem.getValue().asOption();
					assertThat(command).contains(structsItem.getValue().asOption() + " " + name);
				}
			}
		}

		// Uses the real full.h file
		@Nested
		class GenerateBindings {

			@Test
			void withUnspecifiedClassName() throws IOException {
				mainHeader.outputPath().set(outputDir.toString());

				JextractResult result = Extractor.runCommand(mainHeaderItem);

				// full.h has bitfields that are not supported and will cause a warning
				assertWithMessage(result.errorOutput()).that(result.hasWarning()).isTrue();

				List<String> fileNames;
				try (Stream<Path> paths = Files.walk(outputDir)) {
					fileNames = paths.filter(Files::isRegularFile)
							.map(p -> p.getFileName().toString())
							.toList();
				}
				assertThat(fileNames.stream().allMatch(name -> name.endsWith(".java"))).isTrue();
				assertThat(fileNames.size()).isGreaterThan(2); // 2 for header + at least 1 complex type (union/struct...)
			}

			@Test
			void withGivenClassName() throws IOException {
				mainHeader.outputPath().set(outputDir.toString());

				var className = "MyHeader";
				mainHeader.className().set(className);

				JextractResult result = Extractor.runCommand(mainHeaderItem);

				assertWithMessage(result.errorOutput()).that(result.hasError()).isFalse();

				List<String> fileNames;
				try (Stream<Path> paths = Files.walk(outputDir)) {
					fileNames = paths.filter(Files::isRegularFile)
							.map(p -> p.getFileName().toString())
							.toList();
				}
				assertThat(fileNames.stream().anyMatch(name -> name.equals(className + ".java"))).isTrue();
			}
		}
	}

	@Nested
	class WhenHeaderHasReachableIncludes {

		private static final String INCLUDING = "same/including.h";
		private static final String INCLUDED = "same/included.h";

		@TempDir
		private Path outputDir;

		private final CheckBoxTreeItem<Displayable> mainHeaderItem = createMainHeaderItem(INCLUDING);
		private final MainHeader mainHeader = (MainHeader) mainHeaderItem.getValue();

		// Uses synthesized tree
		@Nested
		@TestInstance(Lifecycle.PER_METHOD)
		class CreateCommand {

			private final TreeItem<Displayable> includingGroupItem = createKindGroupItem(IncludeKind.VAR, List.of("v1", "v2"));
			private final TreeItem<Displayable> includedGroupItem = createKindGroupItem(IncludeKind.STRUCT, List.of("s1", "s2"));

			CreateCommand() {
				var selfHeaderItem = new CheckBoxTreeItem<Displayable>(new Header(resourcePath(INCLUDING)));
				selfHeaderItem.getChildren().add(includingGroupItem);

				var includedHeaderItem = new CheckBoxTreeItem<Displayable>(new Header(resourcePath(INCLUDED)));
				includedHeaderItem.getChildren().add(includedGroupItem);

				mainHeaderItem.getChildren().add(selfHeaderItem);
				mainHeaderItem.getChildren().add(includedHeaderItem);
				selfHeaderItem.setSelected(true);
			}

			@Test
			void containingOnlyMainHeaderIncludes() {
				String command = Extractor.createCommand(mainHeaderItem);

				assertThat(command).contains(mainHeader.asOption());
				assertThat(command).doesNotContain(includedGroupItem.getValue().asOption());
				for (var structItem : includingGroupItem.getChildren()) {
					String name = structItem.getValue().asOption();
					assertThat(command).contains(includingGroupItem.getValue().asOption() + " " + name);
				}
			}
		}

		// Uses the real including.h file
		@Nested
		class GenerateBindings {

			@Test
			void forBothHeadersWithOnlyVars() throws IOException {
				mainHeader.outputPath().set(outputDir.toString());

				JextractResult result = Extractor.runCommand(mainHeaderItem);

				assertWithMessage(result.errorOutput()).that(result.hasError()).isFalse();

				List<String> fileNames;
				try (Stream<Path> paths = Files.walk(outputDir)) {
					fileNames = paths.filter(Files::isRegularFile)
							.map(p -> p.getFileName().toString())
							.toList();
				}
				assertThat(fileNames.stream().allMatch(name -> name.endsWith(".java"))).isTrue();
				assertThat(fileNames).hasSize(2); // 2 for header
			}
		}
	}

	@Nested
	class WhenHeaderHasUnreachableIncludes {

		private static final String HEADER_REL_PATH = "include_somewhere.h";

		@TempDir
		private Path outputDir;

		private final CheckBoxTreeItem<Displayable> mainHeaderItem = createMainHeaderItem(HEADER_REL_PATH);
		private final MainHeader mainHeader = (MainHeader) mainHeaderItem.getValue();

		@Test
		void createCommandContainingOnlyMainHeader() {
			String command = Extractor.createCommand(mainHeaderItem);

			assertThat(command).contains(mainHeader.asOption());
			assertThat(command).doesNotContain("--include-");
		}

		@Test
		void reportTheErrorAndNotGenerateAnyFiles() throws Exception {
			mainHeader.outputPath().set(outputDir.toString());

			JextractResult result = Extractor.runCommand(mainHeaderItem);

			assertThat(result.hasError()).isTrue();
			assertThat(result.errorOutput()).contains("'included1.h' file not found");

			long fileCount;
			try (var stream = Files.walk(outputDir)) {
				fileCount = stream.filter(Files::isRegularFile).count();
			}
			assertThat(fileCount).isEqualTo(0);
		}
	}

	/// An [IncludeDeclaration] created from a `String` instead of from a [Declaration]. Used to synthesize test trees without
	/// invoking [Parser]. Its [#asOption()] is its name.
	protected record MockDeclaration(String name) implements Displayable {

		@Override
		public String simple() {
			return name;
		}

		@Override
		public String detailed() {
			return name;
		}

		@Override
		public String asOption() {
			return name;
		}
	}

//	private CheckBoxTreeItem<Displayable> buildHeader(String path, Map<IncludeKind, List<String>> symbols) {
//		var headerItem = new CheckBoxTreeItem<Displayable>(new Header(resourcePath(path)));
//		symbols.forEach((kind, decls) -> {
//			createIncludeGroupItem(headerItem, kind, decls);
//		});
//		return headerItem;
//	}

	private static CheckBoxTreeItem<Displayable> createKindGroupItem(IncludeKind kind, List<String> decls) {
		var kindGroupItem = new CheckBoxTreeItem<Displayable>(kind);
		for (String decl : decls) {
			kindGroupItem.getChildren().add(new CheckBoxTreeItem<>(new MockDeclaration(decl)));
		}
		return kindGroupItem;
	}
}