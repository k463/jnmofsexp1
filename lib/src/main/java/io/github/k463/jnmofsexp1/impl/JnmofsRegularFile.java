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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
        private int channelPosition = 0;

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
            return read(new ByteBuffer[] { dst }, 0, 1, 0, false);
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length)
            throws IOException {
            return read(dsts, offset, length, 0, false);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            return write(new ByteBuffer[] { src }, 0, 1, 0, false);
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length)
            throws IOException {
            return write(srcs, offset, length, 0, false);
        }

        @Override
        public long position() throws IOException {
            ensureOpen();
            return withIoLock(() -> channelPosition);
        }

        @Override
        public FileChannel position(long newPosition) throws IOException {
            ensureOpen();
            if (newPosition < 0) {
                throw new IllegalArgumentException(
                    "position must be > 0, got: %d".formatted(newPosition)
                );
            }
            withIoLock(() -> channelPosition = (int) newPosition);
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
            return read(new ByteBuffer[] { dst }, 0, 1, position, true);
        }

        @Override
        public int write(ByteBuffer src, long position) throws IOException {
            return write(new ByteBuffer[] { src }, 0, 1, position, true);
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
                contents.position(0);
                contents.limit(contents.capacity());
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

        /**
         * Read a sequence of bytes from this channel into a subsequence of the
         * given buffers.
         *
         * @param   dsts
         *          buffers into which the bytes are to be transferred
         * @param   offset
         *          offset within the {@code dsts} array of the first buffer
         *          into which bytes are to be transferred, must be positive
         *          and not larger than {@code dsts.length}
         * @param   length
         *          maximum number of buffers from {@code dsts} array to access
         *          must be positive and not larger than {@code dsts.length - offset}
         * @param   position
         *          file position at which to begin reading bytes, must be
         *          positive, only used if {@code absolute == true}
         * @param   absolute
         *          if true this is an absolute read at the position specified
         *          in {@code position} argument, without updating the channel's
         *          current position; if false this is a relative read at the
         *          channel's current position as reported by {@link #position()}
         *          which is then updated after the read
         * @return  number of bytes read, possibly 0, or -1 if the channel has
         *          reached end-of-stream
         */
        private int read(
            ByteBuffer[] dsts,
            int offset,
            int length,
            long position,
            boolean absolute
        ) throws IOException {
            ensureReadable();
            Objects.checkFromIndexSize(offset, offset + length, dsts.length);
            List<ByteBuffer> dstBuffers = Arrays.asList(dsts).subList(
                offset,
                offset + length
            );
            dstBuffers.forEach(Objects::requireNonNull);

            return withIoLock(() -> {
                int reqPosition = absolute ? (int) position : channelPosition;

                if (reqPosition >= contents.capacity()) {
                    return -1;
                }

                int bytesRead = 0;

                for (ByteBuffer dst : dstBuffers) {
                    final int bytesAvailable = Math.min(
                        dst.remaining(),
                        contents.capacity() - reqPosition
                    );

                    if (bytesAvailable == 0) continue;

                    contents.position(reqPosition);
                    contents.limit(reqPosition + bytesAvailable);
                    dst.put(contents);

                    bytesRead += bytesAvailable;
                    reqPosition += bytesAvailable;
                    if (!absolute) {
                        channelPosition += bytesAvailable;
                    }
                }

                return bytesRead;
            });
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

        /**
         * Write a sequence of bytes to this channel from a subsequence of
         * given buffers.
         *
         * @param   srcs
         *          buffers from which to retrieve the bytes to write
         * @param   offset
         *          offset within the {@code srcs} array of the first buffer
         *          from which bytes are to be retrieved, must be positive and
         *          not larger than {@code srcs.length}
         * @param   length
         *          maximum number of buffers from {@code srcs} array to access
         *          must be positive and not larger than {@code srcs.length - offset}
         * @param   position
         *          file position at which to begin writing bytes, must be
         *          positive, only used if {@code absolute == true}
         * @param   absolute
         *          if true this is an absolute write at the position specified
         *          in {@code position} argument, without updating the channel's
         *          current position; if false this is a relative write at the
         *          channel's current position as reported by {@link #position()}
         *          which is then updated after the write
         * @return  number of bytes written
         * @throws IOException
         */
        private int write(
            ByteBuffer[] srcs,
            int offset,
            int length,
            long position,
            boolean absolute
        ) throws IOException {
            ensureWritable();
            Objects.checkFromIndexSize(offset, offset + length, srcs.length);
            List<ByteBuffer> srcBuffers = Arrays.asList(srcs).subList(
                offset,
                offset + length
            );
            srcBuffers.forEach(Objects::requireNonNull);

            return withIoLock(() -> {
                int bytesWritten = 0;
                int reqPosition = absolute ? (int) position : channelPosition;

                if (
                    !absolute && openOptions.contains(StandardOpenOption.APPEND)
                ) {
                    channelPosition = reqPosition = contents.capacity();
                }

                // ensure capacity
                final int bytesRemaining = srcBuffers
                    .stream()
                    .collect(Collectors.summingInt(ByteBuffer::remaining));
                ensureCapacity(reqPosition + bytesRemaining);

                // write
                contents.position(reqPosition);
                // srcBuffers.forEach(contents::put);
                srcBuffers.forEach(src -> {
                    dumpPreviewBufRead(src, true);
                    contents.put(src);
                });
                bytesWritten = contents.position() - reqPosition;
                dumpPreviewBufRead(contents, false);

                if (!absolute) {
                    channelPosition += bytesWritten;
                }

                return bytesWritten;
            });
        }

        private void dumpPreviewBufRead(ByteBuffer buf, boolean fromCurrent) {
            int origPosition = buf.position();
            int fromPosition = fromCurrent ? origPosition : 0;
            final byte[] bytes = new byte[buf.capacity() - fromPosition];
            buf.position(fromPosition);
            buf.get(bytes);
            buf.position(origPosition);
            final String s = new String(bytes);
            System.out.println(
                "InternalFileChannel[file.id=%d].dumpPreviewBufRead: buf=%s, read=`%s`".formatted(
                    id(),
                    buf.toString(),
                    s
                )
            );
        }
    }
}
