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
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JnmofsFileSystem extends FileSystem {

    private final Map<Path, JnmofsFileSystemNamespace> namespaces;
    private final JnmofsFileSystemProvider provider;
    private final List<String> roots;
    private final List<Pattern> rootPatterns;
    private final String separator;
    private final URI uri;

    JnmofsFileSystem(
        JnmofsFileSystemProvider provider,
        URI uri,
        Map<String, ?> env
    ) {
        this.provider = provider;
        this.uri = uri;

        Map<String, Object> props = new HashMap<String, Object>(env);

        this.separator = props.getOrDefault("separator", "/").toString();

        // allow configuring roots, but default to a single root with name ""
        List<String> configuredRoots = props
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().matches("roots\\.\\d+\\.name"))
            .map(entry -> (String) entry.getValue())
            .sorted()
            .toList();
        this.roots = configuredRoots.isEmpty() ? List.of("") : configuredRoots;

        // Matching paths against roots is done quite often through getPath so
        // pre-create the compiled patterns; sort them by longest first so that
        // nested roots are detected correctly.
        this.rootPatterns = roots
            .stream()
            .sorted(
                Comparator.comparingInt(String::length)
                    .reversed()
                    .thenComparing(String::compareTo)
            )
            .map(r ->
                Pattern.compile(
                    "^(?<root>%s)(?:%s(?<path>.*)|)$".formatted(
                        Pattern.quote(r),
                        Pattern.quote(separator)
                    )
                )
            )
            .toList();

        // initialise namespace stores for each of the roots
        // NOTE(k463): because of the circular dep between the FileSystem and
        // SimplePath; properties that SimplePath relies on need to be already
        // initialised, e.g. separator
        this.namespaces = roots
            .stream()
            .map(this::getPath)
            .collect(
                Collectors.toUnmodifiableMap(
                    Function.identity(),
                    JnmofsFileSystemNamespace::new
                )
            );

        System.out.println(
            "JnmofsFileSystem(%s, %s, %s)\n  separator=%s\n  roots=%s, .namespaces=%s".formatted(
                provider,
                uri,
                env,
                separator,
                roots,
                namespaces
            )
        );
    }

    @Override
    public FileSystemProvider provider() {
        return this.provider;
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'close'");
    }

    @Override
    public boolean isOpen() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isOpen'"
        );
    }

    @Override
    public boolean isReadOnly() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isReadOnly'"
        );
    }

    @Override
    public String getSeparator() {
        return separator;
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return roots
            .stream()
            .map((String t) -> (Path) new SimplePath(this, Optional.of(t), ""))
            .toList();
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return namespaces
            .values()
            .stream()
            .map(FileStore.class::cast)::iterator;
    }

    // Oddly this method is not specified on the FileSystem interface which
    // makes it impossible to implement FileSystemProvider.getFileStore(Path)
    // using only NIO.2 interfaces.
    public FileStore getFileStore(Path path) {
        if (!this.equals(path.getFileSystem())) {
            throw new IllegalArgumentException(
                "Path %s is associated with a different FileSystem than this (%s)".formatted(
                    path,
                    this
                )
            );
        }
        Path root = path.normalize().toAbsolutePath().getRoot();
        if (!namespaces.containsKey(root)) {
            throw new IllegalArgumentException(
                "Path's (%s) root (%s) not found in this FileSystem".formatted(
                    path,
                    root
                )
            );
        }
        return namespaces.get(root);
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'supportedFileAttributeViews'"
        );
    }

    @Override
    public Path getPath(String first, String... more) {
        SplitRootPathResult res = splitRootPath(first);
        // System.out.println(
        //     "JnmofsFileSystem().getPath(%s, %s): splitRootPath => %s".formatted(
        //         first,
        //         Arrays.asList(more),
        //         res
        //     )
        // );
        Path p = new SimplePath(this, res.root(), res.path());
        for (String m : more) {
            if (m != null && !m.isBlank()) {
                p = p.resolve(m);
            }
        }
        return p;
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getPathMatcher'"
        );
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getUserPrincipalLookupService'"
        );
    }

    @Override
    public WatchService newWatchService() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'newWatchService'"
        );
    }

    // Another method missing from the FileSystem interface which makes it
    // impossible to implement Path.toUri() using only NIO.2 interfaces.
    public URI toUri(Path path) {
        Path absPath = path.toAbsolutePath();

        // This root mangling may seem weird, but we have to normalize the
        // separators (and root can contain them) to "/", plus jnmofs root
        // (namespace) paths can start with the separator or not, so we need to
        // be able to distinguish between root=/, root=@v1, and root=/v/@v2, so
        // we ensure uriPath always starts with "/", and the "/" is doubled
        // when the root also starts with "/".
        String root = String.join(
            "/",
            absPath.getRoot().toString().split(Pattern.quote(separator))
        );
        if (!"/".equals(root)) {
            root = "/" + root;
        }

        String uriPath = Stream.concat(
            Stream.of(root),
            StreamSupport.stream(absPath.spliterator(), false).map(
                Path::toString
            )
        ).collect(Collectors.joining("/"));
        if (Files.isDirectory(absPath)) {
            uriPath = uriPath + "/";
        }
        try {
            return new URI(
                uri.getScheme(),
                uri.getUserInfo(),
                uri.getHost(),
                uri.getPort(),
                uriPath,
                null,
                null
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // Helper methods

    /**
     * Splits the given path string into a root and the remaining subpath.
     * <p>
     * This method checks if the input path matches any of the configured roots using
     * regular expressions, ensuring that only correct subpaths are matched (not just substrings).
     * <p>
     * The roots are checked in order from the longest to the shortest, so that nested roots
     * are correctly identified and the most specific root is matched first.
     * <p>
     * If the path starts with the separator but does not match any configured root,
     * an {@link InvalidPathException} is thrown.
     *
     * @param in the input path string to split
     * @return a {@code SplitRootPathResult} containing the matched root (if any) and the remaining path
     * @throws NullPointerException if {@code in} is null
     * @throws InvalidPathException if the path starts with the separator but does not match any root
     */
    private JnmofsFileSystem.SplitRootPathResult splitRootPath(String in) {
        Objects.requireNonNull(in);

        Optional<String> root = Optional.empty();
        String path = in;

        for (Pattern rootp : rootPatterns) {
            Matcher m = rootp.matcher(in);
            if (!m.matches()) continue;

            root = Optional.ofNullable(m.group("root"));
            path = Optional.ofNullable(m.group("path")).orElse("");
            // System.out.println(
            //     "JnmofsFileSystem().splitRootPath(%s): m=%s, grp[root]=%s, grp[path]=%s".formatted(
            //         in,
            //         m,
            //         m.group("root"),
            //         m.group("path")
            //     )
            // );
            break;
        }

        // TODO: we don't know for sure if the given path is meant to be absolute
        // if the path starts with the separator, but doesn't match configured roots
        // should we still treat it as relative? Might be confusing, so for now fail.
        if (root.isEmpty() && path.startsWith(getSeparator())) {
            throw new InvalidPathException(
                path,
                "path starts with separator but doesn't match any of the configured FileSystem roots"
            );
        }

        return new SplitRootPathResult(root, path);
    }

    private record SplitRootPathResult(Optional<String> root, String path) {}
}
