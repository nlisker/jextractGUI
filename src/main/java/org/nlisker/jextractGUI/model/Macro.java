/*
 * Copyright 2024 Nir Lisker
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
package org.nlisker.jextractGUI.model;

import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Macro {

	@Getter
	StringProperty name, value;

	private Macro(String name, String value) {
		this.name = new SimpleStringProperty(name);
		this.value = new SimpleStringProperty(value);
	}

	@Override
	public String toString() {
		return name.get() + "=" + value.get();
	}

	public static Optional<Macro> fromString(String string) {
		String[] tokens = string.split("=");
		if (tokens.length == 2) {
			return Optional.of(new Macro(tokens[0].strip(), tokens[1].strip()));
		}
		return Optional.empty();
	}

	@Override
	public int hashCode() {
		return Objects.hash(name.get());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		return obj instanceof Macro other && Objects.equals(name.get(), other.name.get());
	}
}