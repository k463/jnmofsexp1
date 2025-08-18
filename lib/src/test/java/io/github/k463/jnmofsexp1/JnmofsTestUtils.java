/*
 * jnmofsexp1 - LIC_PRJ_DESC
 * Copyright (C) 2025 Elthevoypra
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.k463.jnmofsexp1;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public class JnmofsTestUtils {

    JnmofsFileSystemProvider provider = new JnmofsFileSystemProvider();

    public static ByteBuffer buffer(String s) {
        return ByteBuffer.wrap(s.getBytes());
    }

    public FileSystem createTestFs() throws URISyntaxException, IOException {
        return createTestFs(null);
    }

    public FileSystem createTestFs(String name)
        throws URISyntaxException, IOException {
        return createTestFs(name, Collections.emptyMap());
    }

    public FileSystem createTestFs(String name, Map<String, ?> env)
        throws URISyntaxException, IOException {
        System.out.println(
            "creating fs %s with config %s".formatted(getFsUri(name), env)
        );
        return provider.newFileSystem(getFsUri(name), env);
    }

    public FileSystem getTestFs() throws URISyntaxException {
        return getTestFs(null);
    }

    public FileSystem getTestFs(String name) throws URISyntaxException {
        return provider.getFileSystem(getFsUri(name));
    }

    public URI getFsUri(String name) throws URISyntaxException {
        String fdId = Optional.ofNullable(name).orElse("");
        return new URI(JnmofsFileSystemProvider.SCHEME, fdId, "/", null);
    }

    public Path getTestFsRoot() throws URISyntaxException, IOException {
        return createTestFs().getRootDirectories().iterator().next();
    }

    public Path createTestFile() throws URISyntaxException, IOException {
        Path testPath = getTestFsRoot().resolve("test-file-1");
        Files.createFile(testPath);
        return testPath;
    }

    /** List files recursively to stdout, for debugging
     * @throws IOException */
    public void lsr(Path dir) throws IOException {
        System.out.println("utils> ls -R %s".formatted(dir));
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(System.out::println);
        }
    }
}
