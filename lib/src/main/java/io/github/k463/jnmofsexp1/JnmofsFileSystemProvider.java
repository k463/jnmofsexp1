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

import io.github.k463.common.FileOperations;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JnmofsFileSystemProvider extends FileSystemProvider {

    public static final String SCHEME = "jnmofs";
    private final ConcurrentMap<String, JnmofsFileSystem> fileSystems =
        new ConcurrentHashMap<String, JnmofsFileSystem>();

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env)
        throws IOException {
        this.validateFsUri(uri);
        String fsId = this.getFsId(uri);
        JnmofsFileSystem fs = new JnmofsFileSystem(this, uri, env);
        if (fileSystems.putIfAbsent(fsId, fs) != null) {
            throw new FileSystemAlreadyExistsException(
                "A %s filesystem with id %s already exists".formatted(
                    getScheme(),
                    fsId
                )
            );
        }
        return fs;
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        this.validateFsUri(uri);
        String fsId = this.getFsId(uri);
        JnmofsFileSystem fs = fileSystems.get(fsId);
        if (fs != null) {
            return fs;
        }
        throw new FileSystemNotFoundException(
            "A %s filesystem with id %s not found".formatted(getScheme(), fsId)
        );
    }

    @Override
    public Path getPath(URI uri) {
        FileSystem fs = getFileSystem(uri); // NOPMD - bad rule
        String uriPath = uri.getPath();
        // JnmofsFileSystem.toUri prepends a "/" if the path is not empty in
        // order to differentiate between root=/, root=@v1, and root=/v/@v2, so
        // we need to reverse that here
        if (!"/".equals(uriPath) && uriPath.startsWith("/")) {
            uriPath = uriPath.substring(1, uriPath.length());
        }
        return fs.getPath(uriPath);
    }

    @Override
    public SeekableByteChannel newByteChannel(
        Path path,
        Set<? extends OpenOption> options,
        FileAttribute<?>... attrs
    ) throws IOException {
        return getFileOps(path).newByteChannel(path, options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(
        Path dir,
        Filter<? super Path> filter
    ) throws IOException {
        return getFileOps(dir).newDirectoryStream(dir, filter);
    }

    @Override
    public FileChannel newFileChannel(
        Path path,
        Set<? extends OpenOption> options,
        FileAttribute<?>... attrs
    ) throws IOException {
        return getFileOps(path).newFileChannel(path, options, attrs);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs)
        throws IOException {
        getFileOps(dir).createDirectory(dir, attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        getFileOps(path).delete(path);
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options)
        throws IOException {
        getFileOps(source).copy(source, target, options);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options)
        throws IOException {
        getFileOps(source).move(source, target, options);
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return getFileOps(path).isSameFile(path, path2);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return getFileOps(path).isHidden(path);
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        if (!(path.getFileSystem() instanceof JnmofsFileSystem)) {
            throw new UnsupportedOperationException(
                "Method 'getFileStore' is not implemented for FileSystem %s".formatted(
                    path.getFileSystem()
                )
            );
        }
        return ((JnmofsFileSystem) path.getFileSystem()).getFileStore(path);
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        getFileOps(path).checkAccess(path, modes);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(
        Path path,
        Class<V> type,
        LinkOption... options
    ) {
        try {
            return getFileOps(path).getFileAttributeView(path, type, options);
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(
        Path path,
        Class<A> type,
        LinkOption... options
    ) throws IOException {
        return getFileOps(path).readAttributes(path, type, options);
    }

    @Override
    public Map<String, Object> readAttributes(
        Path path,
        String attributes,
        LinkOption... options
    ) throws IOException {
        return getFileOps(path).readAttributes(path, attributes, options);
    }

    @Override
    public void setAttribute(
        Path path,
        String attribute,
        Object value,
        LinkOption... options
    ) throws IOException {
        getFileOps(path).setAttribute(path, attribute, value, options);
    }

    // Private implementation details

    private FileOperations getFileOps(Path path) throws IOException {
        FileSystem fs = path.getFileSystem(); // NOPMD - bad rule
        FileStore fst = getFileStore(path);
        FileOperations ops;

        if (fs instanceof FileOperations) {
            ops = (FileOperations) fs;
        } else if (fst instanceof FileOperations) {
            ops = (FileOperations) fst;
        } else {
            throw new UnsupportedOperationException(
                "Neither the FileSystem (%s) nor FileStore (%s) implement the FileOperations interface".formatted(
                    fs,
                    fst
                )
            );
        }
        return ops;
    }

    // Use the host part as filesystem ID (e.g. jnmofs://ID/path).
    private String getFsId(URI uri) {
        String id = uri.getHost();
        if (id == null || id.isEmpty()) {
            return "default";
        }
        return id;
    }

    private void validateFsUri(URI uri) {
        Objects.requireNonNull(uri);
        String scheme = uri.getScheme();
        if (scheme == null || !SCHEME.equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(
                "scheme should be {}, got {} (uri: {})"
            );
        }
        if (uri.getFragment() != null || uri.getQuery() != null) {
            throw new IllegalArgumentException(
                "URI fragment and query should be empty, uri: {}"
            );
        }
    }
}
