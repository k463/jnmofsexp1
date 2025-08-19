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

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class JnmofsDirectory extends JnmofsFileSystemObject {

    private final Set<Path> members = new CopyOnWriteArraySet<>();

    public JnmofsDirectory() {
        super(JnmofsObjectType.DIRECTORY);
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public boolean addMember(Path member) {
        if (member.getNameCount() != 1) {
            throw new IllegalArgumentException(
                "Directory member must be a single name component, got: %s".formatted(
                    member
                )
            );
        }
        return members.add(member);
    }

    public Set<Path> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public boolean removeMember(Path member) {
        return members.remove(member);
    }

    @Override
    public long size() {
        return 0;
    }
}
