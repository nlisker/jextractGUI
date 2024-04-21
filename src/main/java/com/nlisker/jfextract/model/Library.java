package com.nlisker.jfextract.model;

import java.nio.file.Path;

public sealed interface Library {

	record LibPath(Path path) implements Library {

		@Override
		public String toString() {
			return ":" + path.toString();
		}
	}

	record LibName(String name) implements Library {

		@Override
		public String toString() {
			return name();
		}
	}
}