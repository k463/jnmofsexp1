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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class JnmofsFileSystemTest {

    JnmofsTestUtils utils = new JnmofsTestUtils();

    @Test
    public void testRootDirectories() throws Exception {
        FileSystem testFs = utils.createTestFs(
            "trdfs0",
            Map.of(
                "roots.0.name",
                "",
                "roots.1.name",
                "@v1",
                "roots.2.name",
                "/svol/@v2"
            )
        );

        for (Path root : testFs.getRootDirectories()) {
            assertEquals(testFs, root.getFileSystem());
            assertEquals(root, root.getRoot());
            assertEquals(root, root.normalize());
            assertNull(root.getFileName());
            assertNull(root.getParent());
            assertEquals(0, root.getNameCount());
            assertFalse(root.iterator().hasNext());
        }
    }
}
