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
import io.github.k463.jnmofsexp1.impl.JnmofsDirectory;
import io.github.k463.jnmofsexp1.impl.JnmofsFileSystemObject;
import io.github.k463.jnmofsexp1.impl.JnmofsObjectType;
import io.github.k463.jnmofsexp1.impl.JnmofsRegularFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

// assumption: methods in this class are only called from JnmofsFileSystem and
// JnmofsFileSystemProvider after resolving the given path to the correct
// namespace, so we know the first path argument in every method belongs to
// this namespace.
public class JnmofsFileSystemNamespace
    extends FileStore
    implements FileOperations {

    private final Map<Path, JnmofsFileSystemObject> index =
        new ConcurrentHashMap<>();
    private final Path rootPath;

    JnmofsFileSystemNamespace(Path rootPath) {
        if (!rootPath.isAbsolute()) {
            throw new IllegalArgumentException(
                "FS Namespace root path should be absolute: %s".formatted(
                    rootPath
                )
            );
        }
        this.rootPath = rootPath;

        // initialise root directory, NoSuchFile happens if the parent doesn't
        // exist which isn't checked for root, AlreadyExists also can't happen
        // on init, so ignore them
        try {
            createFsObject(rootPath, JnmofsObjectType.DIRECTORY);
        } catch (NoSuchFileException | FileAlreadyExistsException e) {}
    }

    // FileStore methods

    @Override
    public String name() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'name'");
    }

    @Override
    public String type() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'type'");
    }

    @Override
    public boolean isReadOnly() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isReadOnly'"
        );
    }

    @Override
    public long getTotalSpace() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getTotalSpace'"
        );
    }

    @Override
    public long getUsableSpace() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getUsableSpace'"
        );
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getUnallocatedSpace'"
        );
    }

    @Override
    public boolean supportsFileAttributeView(
        Class<? extends FileAttributeView> type
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'supportsFileAttributeView'"
        );
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'supportsFileAttributeView'"
        );
    }

    // FileOperations methods

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(
        Class<V> type
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getFileStoreAttributeView'"
        );
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getAttribute'"
        );
    }

    @Override
    public SeekableByteChannel newByteChannel(
        Path path,
        Set<? extends OpenOption> options,
        FileAttribute<?>... attrs
    ) throws IOException {
        return newFileChannel(path, options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(
        Path dir,
        Filter<? super Path> filter
    ) throws IOException {
        // TODO: fs lock
        JnmofsFileSystemObject fso = getFsObject(dir);
        if (!fso.getAttributes().isDirectory()) {
            throw new NotDirectoryException(dir.toString());
        }
        JnmofsDirectory fsd = (JnmofsDirectory) fso;

        return new DirectoryStream<Path>() {
            @Override
            public void close() throws IOException {}

            @Override
            public Iterator<Path> iterator() {
                return fsd
                    .getMembers()
                    .stream()
                    .map(dir::resolve)
                    .filter(p -> {
                        // mind boggling why DirectoryStream.Filter is marked as
                        // FunctionalInterface yet it throws a checked exception
                        try {
                            return filter.accept(p);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .iterator();
            }
        };
    }

    @Override
    public FileChannel newFileChannel(
        Path path,
        Set<? extends OpenOption> options,
        FileAttribute<?>... attrs
    ) throws IOException {
        StandardOpenOption[] unsupported = new StandardOpenOption[] {
            StandardOpenOption.DELETE_ON_CLOSE,
            StandardOpenOption.DSYNC,
            StandardOpenOption.SPARSE,
            StandardOpenOption.SYNC,
        };
        if (hasAnyOpt(options, unsupported)) {
            throw new UnsupportedOperationException(
                "Options %s are unsupported".formatted(List.of(unsupported))
            );
        }
        if (
            hasAnyOpt(options, StandardOpenOption.APPEND) &&
            !hasAnyOpt(options, StandardOpenOption.WRITE)
        ) {}

        Optional<JnmofsFileSystemObject> fsObject = findFsObject(path);

        if (fsObject.isEmpty()) {
            if (
                !hasAnyOpt(
                    options,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.CREATE_NEW
                ) ||
                !hasAnyOpt(options, StandardOpenOption.WRITE)
            ) {
                throw new NoSuchFileException(path.toString());
            }
            fsObject = Optional.of(createFsObject(path, JnmofsObjectType.FILE));
        } else if (options.contains(StandardOpenOption.CREATE_NEW)) {
            throw new FileAlreadyExistsException(path.toString());
        }

        if (
            !fsObject.map(o -> o.getAttributes().isRegularFile()).orElse(false)
        ) {
            throw new FileSystemException(
                path.toString(),
                "",
                "Not a regular file"
            );
        }

        JnmofsRegularFile file = (JnmofsRegularFile) fsObject.get();
        return file.openChannel(options, attrs);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs)
        throws IOException {
        createFsObject(dir, JnmofsObjectType.DIRECTORY);
    }

    @Override
    public void delete(Path path) throws IOException {
        Path storePath = toStorePath(path);
        JnmofsFileSystemObject fsObject = getFsObject(storePath);

        if (fsObject.getAttributes().isDirectory()) {
            JnmofsDirectory fsDir = (JnmofsDirectory) fsObject;
            if (fsDir.getMembers().size() > 0) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }

        // System.out.println(
        //     "JnmofsFileSystemNamespace.delete(%s)".formatted(storePath)
        // );
        JnmofsDirectory parentFsd = (JnmofsDirectory) getFsObject(
            storePath.getParent()
        );
        if (index.remove(storePath, fsObject)) {
            parentFsd.removeMember(storePath.getFileName());
            return;
        }
        throw new ConcurrentModificationException(
            "FileSystem object changed in the middle of deletion: %s".formatted(
                storePath
            )
        );
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options)
        throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

    @Override
    public void move(Path source, Path target, CopyOption... options)
        throws IOException {
        Objects.requireNonNull(target);
        Set<CopyOption> optSet = Set.of(options);
        Path sourceAbs = toStorePath(source);
        Path targetAbs = target.normalize().toAbsolutePath();

        JnmofsFileSystemObject sourceFso = getFsObject(sourceAbs);

        boolean sourceIsDir = sourceFso.getAttributes().isDirectory();
        boolean targetExists = Files.exists(targetAbs);
        boolean targetIsDir = Files.isDirectory(targetAbs);
        boolean replace = optSet.contains(StandardCopyOption.REPLACE_EXISTING);
        boolean sameFs = sourceAbs
            .getFileSystem()
            .equals(targetAbs.getFileSystem());

        if (targetExists) {
            if (isSameFile(sourceAbs, targetAbs)) return;
            if (!targetIsDir && !replace) {
                throw new FileAlreadyExistsException(targetAbs.toString());
            }
            if (targetIsDir && !replace) {
                // move into another directory without renaming, so actual target is
                targetAbs = targetAbs.resolve(sourceAbs.getFileName());
                targetExists = Files.exists(targetAbs);
                targetIsDir = Files.isDirectory(targetAbs);
                if (targetExists) {
                    throw new FileAlreadyExistsException(targetAbs.toString());
                }
            }
        }
        if (
            (sourceIsDir || !sameFs) &&
            optSet.contains(StandardCopyOption.ATOMIC_MOVE)
        ) {
            throw new AtomicMoveNotSupportedException(
                sourceAbs.toString(),
                targetAbs.toString(),
                "atomic moves not supported for directories, and only on same FileSystem"
            );
        }
        if (sameFs) {
            ensureNotDescendant(sourceAbs, targetAbs);
        }

        Path targetParent = targetAbs.getParent();
        if (!Files.exists(targetParent)) {
            throw new NoSuchFileException(targetParent.toString());
        } else if (!Files.isDirectory(targetParent)) {
            throw new NotDirectoryException(targetParent.toString());
        }

        // all checks done, start moving

        if (targetExists && replace) {
            Files.delete(targetAbs);
        }

        if (!sameFs) {
            // TODO: copy
            throw new UnsupportedOperationException(
                "file/directory copying not supported yet"
            );
        }

        // we know it's the same FS, but it could be a different namespace, so
        // retrieve the FileSystemObject using the provider
        JnmofsFileSystemNamespace targetNs =
            (JnmofsFileSystemNamespace) targetParent
                .getFileSystem()
                .provider()
                .getFileStore(targetParent);
        JnmofsDirectory targetParentFso =
            (JnmofsDirectory) targetNs.getFsObject(targetParent);
        JnmofsDirectory sourceParentFso = (JnmofsDirectory) getFsObject(
            sourceAbs.getParent()
        );
        List<Path> pathsToMove = index
            .keySet()
            .stream()
            .filter(p -> p.startsWith(sourceAbs))
            .toList();
        for (Path srcMember : pathsToMove) {
            Path targetMember = targetAbs
                .resolve(sourceAbs.relativize(srcMember))
                .normalize();
            targetNs.index.put(targetMember, index.get(srcMember));
            index.remove(srcMember);
        }
        targetParentFso.addMember(targetAbs.getFileName());
        sourceParentFso.removeMember(sourceAbs.getFileName());
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        if (!path.getFileSystem().equals(path2.getFileSystem())) return false;
        if (
            !equals(path2.getFileSystem().provider().getFileStore(path2))
        ) return false;

        JnmofsFileSystemObject fsObject = getFsObject(path);
        JnmofsFileSystemObject fsObject2 = getFsObject(path2);

        return fsObject.id() == fsObject2.id();
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'isHidden'"
        );
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        getFsObject(path);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(
        Path path,
        Class<V> type,
        LinkOption... options
    ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'getFileAttributeView'"
        );
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(
        Path path,
        Class<A> type,
        LinkOption... options
    ) throws IOException {
        return type.cast(getFsObject(path).getAttributes());
    }

    @Override
    public Map<String, Object> readAttributes(
        Path path,
        String attributes,
        LinkOption... options
    ) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'readAttributes'"
        );
    }

    @Override
    public void setAttribute(
        Path path,
        String attribute,
        Object value,
        LinkOption... options
    ) throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException(
            "Unimplemented method 'setAttribute'"
        );
    }

    // Helper methods

    private JnmofsFileSystemObject createFsObject(
        Path path,
        JnmofsObjectType type
    ) throws NoSuchFileException, FileAlreadyExistsException {
        JnmofsFileSystemObject res;
        switch (type) {
            case FILE:
                res = new JnmofsRegularFile();
                break;
            case DIRECTORY:
                res = new JnmofsDirectory();
                break;
            default:
                throw new UnsupportedOperationException(
                    "Unrecognized object type %s".formatted(type)
                );
        }

        Path storePath = toStorePath(path);

        if (index.containsKey(storePath)) {
            throw new FileAlreadyExistsException(storePath.toString());
        }

        if (!storePath.equals(storePath.getRoot())) {
            JnmofsDirectory parentDir = (JnmofsDirectory) getFsObject(
                storePath.getParent()
            );
            parentDir.addMember(storePath.getFileName());
        }
        index.put(storePath, res);

        // System.out.println(
        //     "JnmofsFileSystemNamespace.createFsObject(`%s`) (hashCode=%d); get => %s".formatted(
        //         storePath,
        //         storePath.hashCode(),
        //         index.get(storePath)
        //     )
        // );
        // dumpLs();
        return res;
    }

    /**
     * Ensure {@code target} is not a descendant of {@code source}, as you
     * can't copy or move a directory into a subdirectory of itself.
     *
     * Assumes both source and target are absolute.
     */
    private void ensureNotDescendant(Path source, Path target) {
        // extract a subpath of the target path that is at most the same length as
        // the source path, and check if they're the same path
        Path targetSub = target
            .subpath(0, Math.min(source.getNameCount(), target.getNameCount()))
            .toAbsolutePath();
        if (!source.equals(targetSub)) return;
        throw new UnsupportedOperationException(
            "cannot copy/move directory %s into a subdirectory of itself %s".formatted(
                source,
                target
            )
        );
    }

    private JnmofsFileSystemObject getFsObject(Path path)
        throws NoSuchFileException {
        return findFsObject(path).orElseThrow(() ->
            new NoSuchFileException(path.toString())
        );
    }

    private Optional<JnmofsFileSystemObject> findFsObject(Path path) {
        // var sp = toStorePath(path);
        // System.out.println(
        //     "JnmofsFileSystemNamespace.findFsObject(`%s`) (hashCode=%d) => %s".formatted(
        //         sp,
        //         sp.hashCode(),
        //         index.get(sp)
        //     )
        // );
        // dumpLs();
        return Optional.ofNullable(index.get(toStorePath(path)));
    }

    private boolean hasAnyOpt(
        Set<? extends OpenOption> spec,
        OpenOption... options
    ) {
        return Stream.of(options).anyMatch(o -> spec.contains(o));
    }

    private Path toStorePath(Path path) {
        Path nPath = path.normalize();
        if (!nPath.isAbsolute()) {
            nPath = rootPath.resolve(nPath);
        }
        System.out.println(
            "JnmofsFileSystemNamespace.toStorePath(`%s`) => `%s`".formatted(
                path,
                nPath
            )
        );
        return nPath;
    }

    public void dumpLs() {
        System.out.println("> ls -R %s".formatted(rootPath));
        index.forEach((p, o) -> System.out.println(p.toString()));
    }
}
