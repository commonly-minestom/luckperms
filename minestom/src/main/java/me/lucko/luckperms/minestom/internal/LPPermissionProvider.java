/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.minestom.internal;

import net.kyori.adventure.util.TriState;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.util.Tristate;
import net.minestom.conduit.permissions.PermissionManager;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class LPPermissionProvider implements PermissionManager {

    private final LuckPerms luckPerms;

    public LPPermissionProvider(@NotNull LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    @Override
    public @NotNull TriState hasPermission(
            @NotNull CommandSender sender,
            @NotNull String permission
    ) {
        if (sender instanceof Player player) {
            return hasPermission(player.getUuid(), permission);
        }
        return TriState.TRUE;
    }

    @Override
    public @NotNull TriState hasPermission(
            @NotNull UUID uuid,
            @NotNull String permission
    ) {
        User user = this.luckPerms.getUserManager().getUser(uuid);

        if (user == null) {
            return TriState.NOT_SET;
        }

        Tristate result = user.getCachedData().getPermissionData().checkPermission(permission);
        return convert(result);
    }

    @Override
    public void setPermission(
            @NotNull UUID uuid,
            @NotNull String permission,
            @NotNull TriState triState
    ) {
        if (triState == TriState.NOT_SET) {
            unsetPermission(uuid, permission);
            return;
        }

        User user = this.luckPerms.getUserManager().getUser(uuid);

        if (user == null) {
            return;
        }

        PermissionNode node = PermissionNode.builder(permission)
                .value(triState == TriState.TRUE)
                .build();
        if (user.data().add(node).wasSuccessful()) {
            this.luckPerms.getUserManager().saveUser(user);
        }
    }

    @Override
    public void unsetPermission(@NotNull UUID uuid, @NotNull String permission) {
        User user = this.luckPerms.getUserManager().getUser(uuid);

        if (user == null) {
            return;
        }

        PermissionNode node = PermissionNode.builder(permission).build();
        if (user.data().remove(node).wasSuccessful()) {
            this.luckPerms.getUserManager().saveUser(user);
        }
    }

    private static @NotNull TriState convert(@NotNull Tristate result) {
        return switch (result) {
            case TRUE -> TriState.TRUE;
            case FALSE -> TriState.FALSE;
            case UNDEFINED -> TriState.NOT_SET;
        };
    }
}
