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

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class BasicObjectAttributes implements BasicFileAttributes {

    private final FileTime epochTime = FileTime.fromMillis(0);
    private final JnmofsFileSystemObject fso;

    public BasicObjectAttributes(JnmofsFileSystemObject fso) {
        this.fso = fso;
    }

    @Override
    public FileTime lastModifiedTime() {
        return epochTime;
    }

    @Override
    public FileTime lastAccessTime() {
        return epochTime;
    }

    @Override
    public FileTime creationTime() {
        return epochTime;
    }

    @Override
    public boolean isRegularFile() {
        return fso.getType() == JnmofsObjectType.FILE;
    }

    @Override
    public boolean isDirectory() {
        return fso.getType() == JnmofsObjectType.DIRECTORY;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        return fso.size();
    }

    @Override
    public Object fileKey() {
        return fso.id();
    }
}
