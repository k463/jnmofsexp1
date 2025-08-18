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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class UsageTest {

    JnmofsTestUtils utils = new JnmofsTestUtils();

    @Test
    void testBasicOperations() throws Exception {
        System.out.println("UsageTest.testBasicOperations:");
        var mfs0 = FileSystems.newFileSystem(
            utils.getFsUri("usage0"),
            Collections.singletonMap("separator", "\\")
        );
        var root = mfs0.getRootDirectories().iterator().next();

        var dirA = root.resolve("dir-a");
        var fileA = dirA.resolve("file-a");
        Files.createDirectory(dirA);
        Files.createFile(fileA);
        // mfs0
        //     .getFileStores()
        //     .forEach((var t) -> {
        //         ((JnmofsFileSystemNamespace) t).dumpLs();
        //     });
        utils.lsr(dirA);

        assertTrue(Files.exists(dirA));
        assertTrue(Files.isDirectory(dirA));
        assertFalse(Files.isRegularFile(dirA));
        assertTrue(Files.exists(fileA));
        assertFalse(Files.isDirectory(fileA));
        assertTrue(Files.isRegularFile(fileA));

        assertThrows(FileAlreadyExistsException.class, () ->
            Files.createDirectory(dirA)
        );
        assertThrows(FileAlreadyExistsException.class, () ->
            Files.createDirectory(fileA)
        );
        assertThrows(FileAlreadyExistsException.class, () ->
            Files.createFile(dirA)
        );
        assertThrows(FileAlreadyExistsException.class, () ->
            Files.createDirectory(fileA)
        );

        var dirB = mfs0.getPath("dir-b");
        var fileB = dirB.resolve("file-b");

        // parent dir does not exist
        assertThrows(IOException.class, () -> Files.createFile(fileB));
        Files.createDirectory(dirB);
        Files.createFile(fileB);
        assertTrue(Files.exists(dirB));
        assertTrue(Files.exists(fileB));
    }

    @Test
    void testNestedPaths() throws Exception {
        var mfs1 = FileSystems.newFileSystem(
            utils.getFsUri("usage1"),
            Collections.singletonMap("separator", "\\")
        );

        var dirA = mfs1.getPath("dir-a", "subdir-1", "subsubdir-1");
        var fileA = mfs1.getPath("dir-a", "subdir-1", "subsubdir-1", "file-a");

        assertThrows(IOException.class, () -> Files.createDirectory(dirA));

        Files.createDirectories(dirA);
        assertDoesNotThrow(() -> Files.createDirectories(dirA));
        Files.createFile(fileA);
        assertThrows(FileAlreadyExistsException.class, () ->
            Files.createDirectories(fileA)
        );
    }

    @Test
    void testFileReadWrite() throws Exception {
        var mfs2 = FileSystems.newFileSystem(
            utils.getFsUri("usage2"),
            Collections.singletonMap("separator", "\\")
        );

        var fileA = mfs2.getPath("file-a");
        Files.createFile(fileA);

        try (var ch = FileChannel.open(fileA, StandardOpenOption.WRITE)) {
            assertEquals(0, ch.position());
            ch.write(ByteBuffer.wrap("hello-file-a".getBytes()));
            assertEquals(12, ch.position());
            ch.write(ByteBuffer.wrap("skips-bytes".getBytes()), 20);
            assertEquals(12, ch.position()); // absolute position write should not change position
            assertEquals(31, ch.size());
        }

        try (var ch = FileChannel.open(fileA, StandardOpenOption.READ)) {
            assertEquals(0, ch.position());
            var buf = ByteBuffer.allocate(40);
            assertEquals(31, ch.read(buf));
        }
    }

    @Test
    void testHierarchyMoveDelete() throws Exception {
        System.out.println("UsageTest.testHierarchyMoveDelete:");
        var mfs3 = FileSystems.newFileSystem(
            utils.getFsUri("usage3"),
            Collections.singletonMap("separator", "/")
        );

        var l0dir0 = mfs3.getPath("l0dir0");
        var l0dir0l1dir0 = mfs3.getPath("l0dir0/l1dir0");
        var l0dir0l1dir0l2dir0 = mfs3.getPath("l0dir0/l1dir0/l2dir0");
        var l0dir0l1dir0l2dir0l3file0 = mfs3.getPath(
            "l0dir0/l1dir0/l2dir0/l3file0"
        );
        var l0dir0l1dir0l2file0 = mfs3.getPath("l0dir0/l1dir0/l2file0");
        var l0dir0l1dir0l2file1 = mfs3.getPath("l0dir0/l1dir0/l2file1");

        Files.createDirectories(l0dir0l1dir0l2dir0);
        for (var f : List.of(
            l0dir0l1dir0l2dir0l3file0,
            l0dir0l1dir0l2file0,
            l0dir0l1dir0l2file1
        )) {
            Files.createFile(f);
        }

        var l0dir1 = mfs3.getPath("l0dir1");
        var l0dir1l1dir0 = mfs3.getPath("l0dir1/l1dir0");

        Files.createDirectory(l0dir1);
        System.out.println("before move");
        utils.lsr(mfs3.getPath("/"));
        Files.move(l0dir0l1dir0, l0dir1l1dir0);
        System.out.println("after move");
        utils.lsr(mfs3.getPath("/"));

        assertTrue(Files.isDirectory(l0dir0));
        assertFalse(Files.exists(l0dir0l1dir0));
        assertTrue(Files.exists(l0dir1l1dir0));
        assertTrue(Files.exists(l0dir1l1dir0.resolve("l2dir0")));
        assertTrue(Files.exists(l0dir1l1dir0.resolve("l2dir0/l3file0")));
        assertTrue(Files.exists(l0dir1l1dir0.resolve("l2file0")));
        assertTrue(Files.exists(l0dir1l1dir0.resolve("l2file1")));

        // can't move into a subdirectory of itself
        assertThrows(UnsupportedOperationException.class, () ->
            Files.move(l0dir1, l0dir1l1dir0.resolve("l2dir0"))
        );

        // can't delete a non-empty directory
        assertThrows(DirectoryNotEmptyException.class, () ->
            Files.delete(l0dir1l1dir0)
        );

        try (var walk = Files.walk(l0dir1)) {
            Iterable<Path> paths = walk.sorted(
                Comparator.reverseOrder()
            )::iterator;
            // because delete can throw a checked exception
            for (var p : paths) {
                Files.deleteIfExists(p);
            }
        }

        assertTrue(Files.notExists(l0dir1));
        assertTrue(Files.notExists(l0dir1l1dir0));
        assertTrue(Files.notExists(l0dir1l1dir0.resolve("l2dir0")));
        assertTrue(Files.notExists(l0dir1l1dir0.resolve("l2dir0/l3file0")));
        assertTrue(Files.notExists(l0dir1l1dir0.resolve("l2file0")));
        assertTrue(Files.notExists(l0dir1l1dir0.resolve("l2file1")));
    }
}
