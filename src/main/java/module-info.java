module jfextract {
	requires static lombok;

	requires transitive javafx.graphics;
	requires javafx.base;
	requires javafx.controls;

	requires org.kordamp.ikonli.core;
	requires org.kordamp.ikonli.javafx;
	requires org.kordamp.ikonli.materialdesign2;

	requires org.openjdk.jextract;

	exports com.nlisker.jfextract to javafx.graphics;
}