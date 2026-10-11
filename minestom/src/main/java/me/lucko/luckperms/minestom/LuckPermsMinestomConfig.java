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

package me.lucko.luckperms.minestom;

import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.minestom.internal.LPMinestomPlugin;
import me.lucko.luckperms.minestom.context.ContextProvider;
import net.minestom.server.command.builder.Command;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class LuckPermsMinestomConfig {

    final Set<ContextProvider> contextProviders = new HashSet<>();
    final Set<String> permissionSuggestions = new HashSet<>();

    CommandRegistry commandRegistry = CommandRegistry.minestom();
    Logger logger = LoggerFactory.getLogger(LuckPermsMinestom.class);

    public @NotNull LuckPermsMinestomConfig commandRegistry(@NotNull CommandRegistry registry) {
        this.commandRegistry = registry;
        return this;
    }

    public @NotNull LuckPermsMinestomConfig commandRegistry(@NotNull Consumer<Command> register, @NotNull Consumer<Command> unregister) {
        return commandRegistry(CommandRegistry.of(register, unregister));
    }

    public @NotNull LuckPermsMinestomConfig contextProvider(@NotNull ContextProvider provider) {
        this.contextProviders.add(provider);
        return this;
    }

    public @NotNull LuckPermsMinestomConfig contextProviders(@NotNull ContextProvider... providers) {
        return contextProviders(Arrays.asList(providers));
    }

    public @NotNull LuckPermsMinestomConfig contextProviders(@NotNull Iterable<ContextProvider> providers) {
        providers.forEach(this.contextProviders::add);
        return this;
    }

    public @NotNull LuckPermsMinestomConfig permissionSuggestion(@NotNull String permission) {
        this.permissionSuggestions.add(permission);
        return this;
    }

    public @NotNull LuckPermsMinestomConfig permissionSuggestions(@NotNull String... permissions) {
        return permissionSuggestions(Arrays.asList(permissions));
    }

    public @NotNull LuckPermsMinestomConfig permissionSuggestions(@NotNull Iterable<String> permissions) {
        permissions.forEach(this.permissionSuggestions::add);
        return this;
    }

    public @NotNull LuckPermsMinestomConfig logger(@NotNull Logger logger) {
        this.logger = logger;
        return this;
    }
}
