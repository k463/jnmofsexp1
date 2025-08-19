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

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ZipFsInteroperabilityTest {

    JnmofsTestUtils utils = new JnmofsTestUtils();

    @Test
    public void testDefaultFsZip() throws Exception {
        Path testDir = Files.createTempDirectory("jnmofs-default-fs-zip-test-");
        Path testZipFile = testDir.resolve("test-defaultfs-file.zip");
        URI zipFsUri = URI.create("jar:%s".formatted(testZipFile.toUri()));
        Map<String, String> zipOpts = Collections.singletonMap(
            "create",
            "true"
        );

        try (FileSystem zipFs = FileSystems.newFileSystem(zipFsUri, zipOpts)) {
            Path testFile = zipFs.getPath("test-inside-defaultzip.txt");
            try (
                BufferedWriter writer = Files.newBufferedWriter(
                    testFile,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
                )
            ) {
                writer.write("hello-world");
            }
        }

        assertTrue(Files.exists(testZipFile));
        Files.delete(testZipFile);
        Files.delete(testDir);
    }

    @Test
    public void createZippedFile() throws Exception {
        System.out.println("ZipFsInteroperabilityTest.createZippedFile:");

        var mfs = FileSystems.newFileSystem(
            utils.getFsUri(""),
            Collections.emptyMap()
        );
        var root = mfs.getRootDirectories().iterator().next();

        var testZipFile = root.resolve("test-file.zip");
        assertFalse(Files.exists(testZipFile));
        var zipFsUri = URI.create("jar:%s".formatted(testZipFile.toUri()));
        // create the zip file if it doesn't exist
        var zipOpts = Collections.singletonMap("create", "true");
        System.out.println(
            "testZipFile=%s\nzipFsUri=%s".formatted(testZipFile, zipFsUri)
        );

        var ttUnwrappedPath = Paths.get(
            new URI(zipFsUri.getSchemeSpecificPart())
        ).toAbsolutePath();
        System.out.println(
            "ttUnwrappedPath=%s\n  .fileSystem=%s".formatted(
                ttUnwrappedPath,
                ttUnwrappedPath.getFileSystem()
            )
        );

        // {@code FileSystems.newFileSystem} swallows UnsupportedOperationException
        // so when using it we can't tell when the Zip provider is trying to call
        // a method in our filesystem that is unimplemented
        var zipProvider = FileSystemProvider.installedProviders()
            .stream()
            .filter(fsp -> "jar".equalsIgnoreCase(fsp.getScheme()))
            .findFirst()
            .get();
        System.out.println("zipProvider=%s".formatted(zipProvider));

        try (var zipFs = FileSystems.newFileSystem(zipFsUri, zipOpts)) {
            var testFile = zipFs.getPath("test-inside-zip.txt");
            try (
                var writer = Files.newBufferedWriter(
                    testFile,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
                );
            ) {
                writer.write("hello-world");
            }
        }

        assertTrue(Files.exists(testZipFile));
    }
}
