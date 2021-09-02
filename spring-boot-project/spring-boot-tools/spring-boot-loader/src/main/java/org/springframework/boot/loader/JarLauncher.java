/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.Archive.EntryFilter;
import org.springframework.boot.loader.archive.ExplodedArchive;

/**
 * {@link Launcher} for JAR based archives. This launcher assumes that dependency jars are
 * included inside a {@code /BOOT-INF/lib} directory and that application classes are
 * included inside a {@code /BOOT-INF/classes} directory.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 1.0.0
 * updateBy tangzongyuan on 2021/9/1 09:59
 * 打包成jar包之后，运行这个jar包时真正的入口类。
 */
public class JarLauncher extends ExecutableArchiveLauncher {

	private static final String DEFAULT_CLASSPATH_INDEX_LOCATION = "BOOT-INF/classpath.idx";

	static final EntryFilter NESTED_ARCHIVE_ENTRY_FILTER = (entry) -> {
		if (entry.isDirectory()) {
			return entry.getName().equals("BOOT-INF/classes/");
		}
		return entry.getName().startsWith("BOOT-INF/lib/");
	};

	public JarLauncher() {
	}

	protected JarLauncher(Archive archive) {
		super(archive);
	}

	@Override
	protected ClassPathIndexFile getClassPathIndex(Archive archive) throws IOException {
		// Only needed for exploded archives, regular ones already have a defined order
		if (archive instanceof ExplodedArchive) {
			String location = getClassPathIndexFileLocation(archive);
			return ClassPathIndexFile.loadIfPossible(archive.getUrl(), location);
		}
		return super.getClassPathIndex(archive);
	}

	private String getClassPathIndexFileLocation(Archive archive) throws IOException {
		Manifest manifest = archive.getManifest();
		Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
		String location = (attributes != null) ? attributes.getValue(BOOT_CLASSPATH_INDEX_ATTRIBUTE) : null;
		return (location != null) ? location : DEFAULT_CLASSPATH_INDEX_LOCATION;
	}

	@Override
	protected boolean isPostProcessingClassPathArchives() {
		return false;
	}

	@Override
	protected boolean isSearchCandidate(Archive.Entry entry) {
		return entry.getName().startsWith("BOOT-INF/");
	}

	@Override
	protected boolean isNestedArchive(Archive.Entry entry) {
		return NESTED_ARCHIVE_ENTRY_FILTER.matches(entry);
	}

	/**
	 * 依据jar包规范，一个可执行jar包，必须要在MAINFEST.MF中配置Main-Class，该类的main方法就是jar包的入口
	 * 而springboot项目打成jar包之后，Main-Class的值为org.springframework.boot.loader.JarLauncher
	 * 也就是说，执行java -jar命令，会调用此main方法。
	 * @param args args
	 * @throws Exception exception
	 */
	public static void main(String[] args) throws Exception {
		new JarLauncher().launch(args);
	}

}
