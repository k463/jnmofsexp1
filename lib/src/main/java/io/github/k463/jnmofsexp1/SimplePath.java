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
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents a simple implementation of the {@link Path} interface.
 * <p>
 * This class splits the path on the separator returned by {@link FileSystem#getSeparator()}.
 * The path is immutable and is internally stored as both a string and a list of path components.
 * <p>
 * The path is stored as-is except that if it ends with a path separator, the separator is stripped.
 * <p>
 * Use the {@link #normalize()} method to return a new {@link Path} with all components normalized.
 * <p>
 * Quirks of dealing with paths:
 * <p>
 * * "" = default/current directory of the filesystem
 * * root = root of the FS hierarchy of this path
 *   * FS can have multiple roots (btrfs, ZFS)
 *   * root can have a name, path, or "" (e.g. main subvolume)
 *   * null root = relative path
 * * relative path is relative to the default/current directory which should have a root
 * * edge case: root != null && path.startsWith(sep)
 *   * since this implies abspath anyway just strip sep from both ends of path
 * * q: root detection - what if FS.getPath is given a str path that starts with a root name
 *   * Path interface differentiates between root and other path components
 *     * e.g. root shouldn't be returned by {@link #iterator()}
 *   * yet there's no way to indicate something is a root in inputs
 *   * if we do detection there's no way to differentiate between a root name and a subdirectory
 *     or file name in the current directory with the same name
 *   * or between root name "" and the default directory path for that matter
 *   * best bet is to assume root name and let user force directory/file name via `./foo`, but
 *     that has implications for how getPath/resolve variants with multiple components are
 *     implemented (i.e. can't do `p=Path(first); more.map(e -> p.resolve(e)))`)
 *   * root detection is only needed in FS.getPath, Path.resolve(String) is just sugar for
 *     Path.resolve(FS.getPath(String))
 * * root detection edge cases:
 *   * root="", resolve("/foo") => root="", path="foo" - only do sep stripping after root detect
 *   * root="", path="", resolve("foo") => /foo
 *   * root="", path=".", resolve("foo") => /CWD/foo
 *   * root=nil, path="", resolve("foo") => /CWD/foo
 *   * roots=["", "@v1"], getPath("@v1/foo/bar") => root="@v1", path="foo/bar"
 *   * roots=["", "/vol/@v1"], getPath("/vol/@v1/foo/bar") => root="/vol/@v1", path="foo/bar"
 *   * roots=["@v1", "/vol/@v2"], getPath("/foo/bar") => ERR invalid root ""
 *   * roots=["", "@v1"], getPath("@v1/foo").resolve("/foo") => root="", path="foo"
 */
public class SimplePath implements Path {

    private static final String CUR_DIR = ".";
    private static final String PARENT_DIR = "..";

    private final FileSystem fs;
    private final List<String> components;
    private final String path;
    private final Optional<String> root;
    private final Pattern separatorRe;

    SimplePath(FileSystem fs, Optional<String> root, String in) {
        Objects.requireNonNull(in);
        this.fs = fs;
        this.path = stripFinalSep(in);
        this.root = root;
        this.separatorRe = Pattern.compile(Pattern.quote(fs.getSeparator()));
        this.components = separatorRe
            .splitAsStream(path)
            .filter(Predicate.not(String::isEmpty))
            .collect(Collectors.toUnmodifiableList());
        // System.out.println(
        //     "SimplePath(%s, %s, %s)\n  path=%s\n  components=%s, .size=%d".formatted(
        //         fs,
        //         root,
        //         in,
        //         path,
        //         components,
        //         components.size()
        //     )
        // );
    }

    private SimplePath create(List<String> comps) {
        return create(String.join(fs.getSeparator(), comps));
    }

    private SimplePath create(String in) {
        return new SimplePath(fs, root, in);
    }

    private SimplePath createRel(List<String> comps) {
        return createRel(String.join(fs.getSeparator(), comps));
    }

    private SimplePath createRel(String in) {
        return new SimplePath(fs, Optional.empty(), in);
    }

    @Override
    public FileSystem getFileSystem() {
        return fs;
    }

    @Override
    public boolean isAbsolute() {
        return root.isPresent();
    }

    @Override
    public Path getRoot() {
        return root.map(r -> new SimplePath(fs, root, "")).orElse(null);
    }

    @Override
    public Path getFileName() {
        if (components.isEmpty()) return null;
        return createRel(components.get(components.size() - 1));
    }

    @Override
    public Path getParent() {
        if (components.isEmpty()) return null;
        if (components.size() == 1 && !isAbsolute()) return null;
        return create(components.subList(0, components.size() - 1));
    }

    @Override
    public int getNameCount() {
        return components.size();
    }

    @Override
    public Path getName(int index) {
        return createRel(components.get(index));
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        if (
            beginIndex < 0 ||
            endIndex > components.size() ||
            beginIndex > endIndex
        ) {
            throw new IllegalArgumentException(
                "Invalid range [%d,%d], should be within [0,%d]".formatted(
                    beginIndex,
                    endIndex,
                    components.size()
                )
            );
        }
        return createRel(components.subList(beginIndex, endIndex));
    }

    @Override
    public boolean startsWith(Path other) {
        Objects.requireNonNull(other);
        if (!isSameFs(other)) return false;
        if (!getRoot().equals(other.getRoot())) return false;
        if (other.getNameCount() > getNameCount()) return false;
        Path eqLenPath = subpath(0, other.getNameCount());
        if (isAbsolute()) {
            eqLenPath = eqLenPath.toAbsolutePath();
        }
        return eqLenPath.equals(other);
    }

    @Override
    public boolean endsWith(Path other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'endsWith'"
        );
    }

    @Override
    public Path normalize() {
        List<String> normalized = new ArrayList<>();
        for (String comp : components) {
            switch (comp) {
                case "":
                case CUR_DIR:
                    if (normalized.isEmpty()) {
                        normalized.add(CUR_DIR);
                    }
                    break;
                case PARENT_DIR:
                    int last = normalized.size() - 1;
                    if (
                        normalized.isEmpty() ||
                        PARENT_DIR.equals(normalized.get(last))
                    ) {
                        normalized.add(comp);
                    } else {
                        normalized.remove(last);
                    }
                    break;
                default:
                    if (
                        normalized.size() == 1 &&
                        CUR_DIR.equals(normalized.get(0))
                    ) {
                        normalized.clear();
                    }
                    normalized.add(comp);
            }
        }
        return create(normalized);
    }

    @Override
    public Path resolve(Path other) {
        if (other.isAbsolute()) {
            return other;
        } else if (other.getNameCount() == 0) {
            return this;
        }
        return create(
            Stream.concat(
                StreamSupport.stream(this.spliterator(), false),
                StreamSupport.stream(other.spliterator(), false)
            )
                // .peek(e ->
                //     System.out.println(
                //         "Path(%s, %s).resolve(%s): e=%s".formatted(
                //             root,
                //             path,
                //             other,
                //             e
                //         )
                //     )
                // )
                .map(Path::toString)
                .toList()
        );
    }

    @Override
    public Path relativize(Path other) {
        Objects.requireNonNull(other);
        if (!isSameFs(other)) {
            throw new IllegalArgumentException(
                "Paths refer to different FileSystems (%s, %s)".formatted(
                    this,
                    other
                )
            );
        }
        if (isAbsolute() != other.isAbsolute()) {
            throw new IllegalArgumentException(
                "Either both or none of the paths have to be absolute (%s, %s)".formatted(
                    this,
                    other
                )
            );
        }
        if (isAbsolute() && !getRoot().equals(other.getRoot())) {
            throw new IllegalArgumentException(
                "Paths refer to different roots (%s, %s)".formatted(this, other)
            );
        }
        if (equals(other)) {
            return createRel(CUR_DIR);
        }
        if (!isAbsolute() && getNameCount() == 0) {
            return other;
        }

        List<String> otherComponents = StreamSupport.stream(
            other.spliterator(),
            false
        )
            .map(Path::toString)
            .collect(Collectors.toList());

        List<String> newComponents = new ArrayList<>();
        int i = 0;

        while (i < components.size() && i < otherComponents.size()) {
            String thisComp = components.get(i);
            String otherComp = otherComponents.get(i);
            if (thisComp.equals(otherComp)) {
                i++;
            } else {
                break;
            }
        }

        for (int j = i; j < components.size(); j++) {
            newComponents.add(PARENT_DIR);
        }

        for (int j = i; j < otherComponents.size(); j++) {
            newComponents.add(otherComponents.get(j));
        }

        return createRel(newComponents);
    }

    @Override
    public URI toUri() {
        if (!(getFileSystem() instanceof JnmofsFileSystem)) {
            throw new UnsupportedOperationException(
                "Method 'toUri' is not implemented for FileSystem %s".formatted(
                    getFileSystem()
                )
            );
        }
        return ((JnmofsFileSystem) getFileSystem()).toUri(this);
    }

    @Override
    public Path toAbsolutePath() {
        // TODO Path spec just says this resolves against FileSystem default
        // directory, but there's no definition of a default directory, nor any
        // standard way to get it. For now just assume a relative path is
        // relative to the first root.
        return getFileSystem()
            .getRootDirectories()
            .iterator()
            .next()
            .resolve(this);
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        return normalize().toAbsolutePath();
    }

    @Override
    public WatchKey register(
        WatchService watcher,
        Kind<?>[] events,
        Modifier... modifiers
    ) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'register'"
        );
    }

    @Override
    public int compareTo(Path other) {
        if (this == other) return 0; // NOPMD - same object check intended
        if (other == null) return 1;
        if (!isSameFs(other)) {
            throw new ClassCastException(
                "Paths refer to different FileSystems (%s, %s)".formatted(
                    this,
                    other
                )
            );
        }
        return normalize().toString().compareTo(other.normalize().toString());
    }

    // Object methods

    @Override
    public boolean equals(Object other) {
        return compareTo((Path) other) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fs, root, components);
    }

    @Override
    public String toString() {
        // special case root="", components=[] => /
        if (root.isPresent() && root.get().isEmpty() && components.isEmpty()) {
            return fs.getSeparator();
        }
        return Stream.concat(root.stream(), components.stream()).collect(
            Collectors.joining(fs.getSeparator())
        );
    }

    // Helper methods

    private boolean isSameFs(Path other) {
        return getFileSystem().equals(other.getFileSystem());
    }

    private String stripFinalSep(String in) {
        int sepLen = fs.getSeparator().length();
        if (in.length() > sepLen && in.endsWith(fs.getSeparator())) {
            return in.substring(0, in.length() - sepLen);
        }
        return in;
    }
}
