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

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SimplePathTest {

    JnmofsTestUtils utils = new JnmofsTestUtils();

    @Test
    public void testBasicOperations() throws Exception {
        System.out.println("SimplePathTest.testBasicOperations:");
        FileSystem fs = utils.createTestFs(
            "mfs0",
            Collections.singletonMap("separator", "\\")
        );
        System.out.println("  calling fs.getRootDirectories");
        Path root = fs.getRootDirectories().iterator().next();
        String sep = fs.getSeparator();
        System.out.println("  root=%s".formatted(root));

        // Validate assumption about the root
        assertEquals(sep, root.toString(), "root path");
        assertNull(root.getParent());

        // Test absolute path
        Path absolutePath = root.resolve("absolute" + sep + "path");
        System.out.println("  absolutePath=%s".formatted(absolutePath));
        assertTrue(
            absolutePath.isAbsolute(),
            "Path(%s).isAbsolute()".formatted(absolutePath.toString())
        );
        assertEquals(sep + "absolute" + sep + "path", absolutePath.toString());
        assertNotEquals(absolutePath, fs.getPath("absolute" + sep + "path"));

        // Test relative path
        Path relativePath = fs.getPath("relative" + sep + "path");
        assertFalse(relativePath.isAbsolute());
        assertEquals("relative" + sep + "path", relativePath.toString());

        // Test relative to absolute
        assertEquals(root.resolve(relativePath), relativePath.toAbsolutePath());

        // Test path resolution
        Path resolvedPath = relativePath.resolve("to" + sep + "file.txt");
        assertEquals(
            "relative" + sep + "path" + sep + "to" + sep + "file.txt",
            resolvedPath.toString()
        );
        assertEquals(absolutePath, resolvedPath.resolve(absolutePath));

        // Test path normalization
        Path normalizedPath = fs
            .getPath(
                "dir1",
                ".." + sep + "dir2" + sep + "." + sep + sep + "file.txt"
            )
            .normalize();
        assertEquals("dir2" + sep + "file.txt", normalizedPath.toString());
    }

    static Stream<Arguments> relativizeTestArgsProvider() {
        return Stream.of(
            Arguments.of("base/dir", "base/dir", "."),
            Arguments.of(
                "base/dir",
                "base/dir/subdir/file.txt",
                "subdir/file.txt"
            ),
            Arguments.of(
                "/base/dir",
                "/base/dir/subdir/file2.txt",
                "subdir/file2.txt"
            ),
            Arguments.of(
                "basedir/subdir1/subsub",
                "basedir/subdir2/file3.txt",
                "../../subdir2/file3.txt"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("relativizeTestArgsProvider")
    void testRelativizeSuccessful(String base, String target, String expected)
        throws Exception {
        FileSystem mfs = utils.createTestFs();
        Path basePath = mfs.getPath(base);
        Path targetPath = mfs.getPath(target);
        assertEquals(expected, basePath.relativize(targetPath).toString());
    }

    @Test
    void testRelativizeFailures() throws Exception {
        FileSystem mfs5 = utils.createTestFs("mfs5");
        FileSystem mfs6 = utils.createTestFs(
            "mfs6",
            Map.of("roots.0.name", "", "roots.1.name", "@v1")
        );

        List<Executable> funcs = Arrays.asList(
            () -> mfs5.getPath("/foo").relativize(mfs5.getPath("bar")),
            () -> mfs5.getPath("/foo").relativize(mfs6.getPath("/foo")),
            () -> mfs6.getPath("/foo/bar").relativize(mfs6.getPath("@v1/bar"))
        );

        for (Executable func : funcs) {
            assertThrows(IllegalArgumentException.class, func);
        }
    }

    @Test
    public void testEdgeMultiRoot() throws Exception {
        FileSystem mfs1 = utils.createTestFs(
            "mfs1",
            Map.of("roots.0.name", "", "roots.1.name", "@v1")
        );

        assertEquals("@v1", mfs1.getPath("@v1").getRoot().toString());

        Path path1 = mfs1.getPath("@v1/foo/bar");
        assertTrue(path1.isAbsolute());
        assertEquals(mfs1.getPath("@v1"), path1.getRoot());
        assertEquals(mfs1.getPath("foo"), path1.getName(0));
        assertEquals(mfs1.getPath("/baz/quux"), path1.resolve("/baz/quux"));
        assertEquals(mfs1.getPath(""), path1.resolve("/baz/quux").getRoot());

        Path path2 = mfs1.getPath("/foo");
        assertTrue(path2.isAbsolute());
        assertEquals(mfs1.getPath(""), path2.getRoot());
        assertEquals(mfs1.getPath("foo"), path2.getName(0));

        FileSystem mfs2 = utils.createTestFs(
            "mfs2",
            Map.of("roots.0.name", "", "roots.1.name", "/vol/@v1")
        );

        assertThrows(ClassCastException.class, () ->
            mfs1.getPath("/foo").equals(mfs2.getPath("/foo"))
        );

        Path path3 = mfs2.getPath("/foo/bar");
        assertTrue(path3.isAbsolute());
        assertEquals(mfs2.getPath(""), path3.getRoot());
        assertEquals(mfs2.getPath("foo"), path3.getName(0));

        Path path4 = mfs2.getPath("/vol/@v1/foo/bar");
        assertTrue(path4.isAbsolute());
        assertEquals(mfs2.getPath("/vol/@v1"), path4.getRoot());
        assertEquals(mfs2.getPath("foo"), path4.getName(0));

        Path path5 = mfs2.getPath("./foo/bar");
        assertFalse(path5.isAbsolute());
        // TODO: CWD tests
    }

    @Test
    public void testEdgeNonDefaultRoot() throws Exception {
        FileSystem mfs3 = utils.createTestFs(
            "mfs3",
            Map.of("roots.0.name", "@v1", "roots.1.name", "/vol/@v2")
        );

        assertThrows(InvalidPathException.class, () ->
            mfs3.getPath("/foo/bar")
        );
    }

    @Test
    public void testUriConversion() throws Exception {
        System.out.println("SimplePathTest.testUriConversion:");
        FileSystem mfs4 = utils.createTestFs(
            "mfs4",
            Map.of(
                "roots.0.name",
                "",
                "roots.1.name",
                "@v1",
                "roots.2.name",
                "\\vol\\@v2",
                "separator",
                "\\"
            )
        );

        record Case(String path, String uri) {}

        List<Case> cases = List.of(
            new Case("", "jnmofs://mfs4//"),
            new Case("\\foo\\bar", "jnmofs://mfs4//foo/bar"),
            new Case("@v1\\qux", "jnmofs://mfs4/@v1/qux"),
            new Case(
                "\\vol\\@v2\\baz\\quux",
                "jnmofs://mfs4//vol/@v2/baz/quux"
            ),
            new Case("rel\\fred", "jnmofs://mfs4//rel/fred"),
            new Case("\\existing\\dir", "jnmofs://mfs4//existing/dir/")
        );

        Files.createDirectories(mfs4.getPath("\\existing\\dir"));

        for (var c : cases) {
            assertEquals(URI.create(c.uri), mfs4.getPath(c.path).toUri());
        }

        FileSystem mfs5 = utils.createTestFs();
        assertEquals(URI.create("jnmofs:////"), mfs5.getPath("/").toUri());
        assertEquals(
            URI.create("jnmofs:////foo/bar"),
            mfs5.getPath("/foo/bar").toUri()
        );
    }
}
