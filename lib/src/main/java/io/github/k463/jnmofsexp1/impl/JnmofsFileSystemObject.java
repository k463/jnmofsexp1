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
import java.util.concurrent.atomic.AtomicLong;

/**
 * This abstract class serves as the base class for all types of objects
 * that can be stored in the FileSystem. It provides common functionality
 * and properties for file system objects, such as their type and methods
 * to determine if the object is a file or a directory.
 */
public abstract class JnmofsFileSystemObject {

    // Unique ID for each filesystem object
    private static final AtomicLong objIdSource = new AtomicLong();
    private final long objId;

    protected final JnmofsObjectType type;
    private final BasicObjectAttributes attributes;

    protected JnmofsFileSystemObject(JnmofsObjectType type) {
        this.objId = objIdSource.incrementAndGet();
        this.type = type;
        this.attributes = new BasicObjectAttributes(this);
    }

    public BasicFileAttributes getAttributes() {
        return attributes;
    }

    public JnmofsObjectType getType() {
        return type;
    }

    public long id() {
        return objId;
    }

    public abstract long size();
}
