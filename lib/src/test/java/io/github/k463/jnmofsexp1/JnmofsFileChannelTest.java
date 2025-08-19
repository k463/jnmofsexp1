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

import static io.github.k463.jnmofsexp1.JnmofsTestUtils.buffer;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.jupiter.api.Test;

public class JnmofsFileChannelTest {

    JnmofsTestUtils utils = new JnmofsTestUtils();

    private FileChannel channel(OpenOption... options)
        throws URISyntaxException, IOException {
        Path testFile = utils.getOrCreateTestFile();
        return FileChannel.open(
            testFile,
            options.length > 0
                ? options
                : new OpenOption[] { StandardOpenOption.READ }
        );
    }

    @Test
    public void testPosition() throws Exception {
        try (FileChannel channel = channel()) {
            assertEquals(0, channel.position());
            assertSame(channel, channel.position(10));
            assertEquals(10, channel.position());
        }
    }

    @Test
    public void testReadWrite() throws Exception {
        try (
            FileChannel channel = channel(
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            );
        ) {
            assertTrue(channel.isOpen());
            assertEquals(0, channel.position());
            assertEquals(0, channel.size());
            channel.write(buffer("hello"));
            assertEquals(5, channel.position());
            channel.write(new ByteBuffer[] { buffer("-"), buffer("world") });
            assertEquals(11, channel.position());
            channel.write(buffer("folks"), 6);
            assertEquals(11, channel.position());
            assertEquals(11, channel.size());
            channel.position(0);
            assertThrows(NonReadableChannelException.class, () ->
                channel.read(ByteBuffer.allocate(11))
            );
        }

        try (FileChannel channel = channel()) {
            assertEquals(0, channel.position());
            assertEquals(11, channel.size());
            ByteBuffer buf = ByteBuffer.allocate(11);
            assertEquals(11, channel.read(buf));
            assertEquals(11, channel.position());
            buf.flip();
            var readStr = StandardCharsets.UTF_8.decode(buf).toString();
            assertEquals("hello-folks", readStr);
            buf.flip();
            assertEquals(-1, channel.read(buf));
            assertEquals(11, channel.position());
            assertThrows(NonWritableChannelException.class, () ->
                channel.write(buffer("test"))
            );
        }
    }
}
