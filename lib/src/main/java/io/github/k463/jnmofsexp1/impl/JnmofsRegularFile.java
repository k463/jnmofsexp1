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
package io.github.k463.jnmofsexp1.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class JnmofsRegularFile extends JnmofsFileSystemObject {

    private ByteBuffer contents = ByteBuffer.allocate(0);
    private final Object fileLock = new Object();

    public JnmofsRegularFile() {
        super(JnmofsObjectType.FILE);
    }

    public FileChannel openChannel(
        Set<? extends OpenOption> options,
        FileAttribute<?>... attrs
    ) throws IOException {
        return new InternalFileChannel(options, attrs);
    }

    @Override
    public long size() {
        return contents.capacity();
    }

    // Implement the FileChannel as internal class so that RegularFile doesn't
    // need to effectively also implement the SeekableByteChannel interface.
    private class InternalFileChannel extends FileChannel {

        private volatile boolean open = true;
        private final Set<? extends OpenOption> openOptions;
        private int position = 0;

        InternalFileChannel(
            Set<? extends OpenOption> options,
            FileAttribute<?>... attrs
        ) throws IOException {
            this.openOptions = options;

            if (options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
                truncate(0);
            }
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            ensureReadable();
            return withIoLock(() -> {
                if (position > contents.capacity()) {
                    return -1;
                }

                final int bytesRemaining = Math.min(
                    dst.remaining(),
                    contents.capacity() - position
                );
                contents.position(position);
                contents.limit(position + bytesRemaining);
                dst.put(contents);

                position += bytesRemaining;
                return bytesRemaining;
            });
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length)
            throws IOException {
            ensureReadable();
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException(
                "Unimplemented method 'read'"
            );
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            ensureWritable();
            Objects.requireNonNull(src);
            return withIoLock(() -> {
                int bytesWritten = 0;
                if (openOptions.contains(StandardOpenOption.APPEND)) {
                    position = contents.capacity();
                }

                // write
                final int bytesRemaining = src.remaining();
                ensureCapacity(position + bytesRemaining);
                contents.position(position);
                contents.put(src);
                bytesWritten = contents.position() - position;

                position += bytesWritten;
                return bytesWritten;
            });
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length)
            throws IOException {
            ensureWritable();
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException(
                "Unimplemented method 'write'"
            );
        }

        @Override
        public long position() throws IOException {
            ensureOpen();
            return withIoLock(() -> position);
        }

        @Override
        public FileChannel position(long newPosition) throws IOException {
            ensureOpen();
            if (newPosition < 0) {
                throw new IllegalArgumentException(
                    "position must be > 0, got: %d".formatted(newPosition)
                );
            }
            withIoLock(() -> position = (int) newPosition);
            return this;
        }

        @Override
        public long size() throws IOException {
            ensureOpen();
            return withIoLock(() -> JnmofsRegularFile.this.size());
        }

        @Override
        public FileChannel truncate(long size) throws IOException {
            ensureWritable();
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException(
                "Unimplemented method 'truncate'"
            );
        }

        @Override
        public void force(boolean metaData) throws IOException {
            ensureOpen();
            // no-op for in-memory filesystem
        }

        @Override
        public long transferTo(
            long position,
            long count,
            WritableByteChannel target
        ) throws IOException {
            ensureReadable();
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException(
                "Unimplemented method 'transferTo'"
            );
        }

        @Override
        public long transferFrom(
            ReadableByteChannel src,
            long position,
            long count
        ) throws IOException {
            ensureWritable();
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException(
                "Unimplemented method 'transferFrom'"
            );
        }

        @Override
        public int read(ByteBuffer dst, long position) throws IOException {
            ensureReadable();
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException(
                "Unimplemented method 'read'"
            );
        }

        @Override
        public int write(ByteBuffer src, long position) throws IOException {
            ensureWritable();
            int iposition = (int) position;
            // NOTE: this is an absolute position write, so APPEND mode doesn't matter
            Objects.requireNonNull(src);
            return withIoLock(() -> {
                int bytesWritten = 0;

                // write
                final int bytesRemaining = src.remaining();
                ensureCapacity(iposition + bytesRemaining);
                contents.position(iposition);
                contents.put(src);
                bytesWritten = contents.position() - iposition;

                return bytesWritten;
            });
        }

        @Override
        public MappedByteBuffer map(MapMode mode, long position, long size)
            throws IOException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException(
                "Unimplemented method 'map'"
            );
        }

        @Override
        public FileLock lock(long position, long size, boolean shared)
            throws IOException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException(
                "Unimplemented method 'lock'"
            );
        }

        @Override
        public FileLock tryLock(long position, long size, boolean shared)
            throws IOException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException(
                "Unimplemented method 'tryLock'"
            );
        }

        @Override
        protected void implCloseChannel() throws IOException {
            open = false;
        }

        // Helper methods

        // this should only be used in context with a file lock held
        private void ensureCapacity(int newCap) {
            if (contents.capacity() > newCap) return;
            ByteBuffer newContents = ByteBuffer.allocate(newCap);
            if (contents.capacity() > 0) {
                newContents.put(contents);
            }
            contents = newContents;
        }

        private void ensureOpen() throws IOException {
            if (!open) throw new ClosedChannelException();
        }

        private void ensureReadable() throws IOException {
            ensureOpen();
            if (!openOptions.contains(StandardOpenOption.READ)) {
                throw new NonReadableChannelException();
            }
        }

        private void ensureWritable() throws IOException {
            ensureOpen();
            if (!openOptions.contains(StandardOpenOption.WRITE)) {
                throw new NonWritableChannelException();
            }
        }

        private <T> T withIoLock(Supplier<T> func)
            throws AsynchronousCloseException {
            boolean completed = false;
            T res;
            synchronized (fileLock) {
                try {
                    begin();
                    res = func.get();
                    completed = true;
                } finally {
                    end(completed);
                }
            }
            return res;
        }
    }
}
