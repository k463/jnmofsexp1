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
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class JnmofsFileSystemProviderTest {

    JnmofsTestUtils utils = new JnmofsTestUtils();

    @Test
    void providerInstallationFunctionsCorrectly() throws Exception {
        assertTrue(
            FileSystemProvider.installedProviders()
                .stream()
                .anyMatch(p ->
                    p
                        .getScheme()
                        .equalsIgnoreCase(JnmofsFileSystemProvider.SCHEME)
                )
        );

        FileSystems.newFileSystem(
            utils.getFsUri("inst-test"),
            Collections.emptyMap()
        );
    }

    @Test
    void canCreateDefault() throws Exception {
        System.out.println("in canCreateDefault");
        FileSystem testFs = utils.createTestFs();
        System.out.println("in canCreateDefault, created");

        assertEquals(
            "io.github.k463.jnmofsexp1.JnmofsFileSystemProvider",
            testFs.provider().getClass().getName()
        );
    }

    @Test
    void canCreateNamed() throws Exception {
        utils.createTestFs("foo");
    }

    @Test
    void createDuplicateThrows() throws Exception {
        utils.createTestFs();
        // second call with the same args should fail
        assertThrows(
            FileSystemAlreadyExistsException.class,
            this.utils::createTestFs
        );
    }

    @Test
    void canGetExistingDefault() throws Exception {
        assertSame(utils.createTestFs(), utils.getTestFs());
    }

    @Test
    void canGetExistingNamed() throws Exception {
        assertSame(utils.createTestFs("bar"), utils.getTestFs("bar"));
    }

    @Test
    void getNotCreatedThrows() throws Exception {
        assertThrows(FileSystemNotFoundException.class, this.utils::getTestFs);
    }
}
