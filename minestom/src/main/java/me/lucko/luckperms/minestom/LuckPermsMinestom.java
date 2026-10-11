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

import me.lucko.luckperms.minestom.internal.LPMinestomBootstrap;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.function.Consumer;

public final class LuckPermsMinestom {

    private static LPMinestomBootstrap bootstrap = null;

    private LuckPermsMinestom() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull LuckPerms enable(@NotNull Path dataDirectory) {
        return enable(dataDirectory, _ -> {});
    }

    public static @NotNull LuckPerms enable(@NotNull Path dataDirectory, @NotNull Consumer<LuckPermsMinestomConfig> configurator) {
        LuckPermsMinestomConfig config = new LuckPermsMinestomConfig();
        configurator.accept(config);

        LPMinestomBootstrap instance = new LPMinestomBootstrap(
                config.logger,
                dataDirectory.toAbsolutePath(),
                config.contextProviders,
                config.permissionSuggestions,
                config.commandRegistry
        );

        synchronized (LuckPermsMinestom.class) {
            if (bootstrap != null) {
                throw new IllegalStateException("Already initialized - call disable() first");
            }
            bootstrap = instance;
        }

        instance.onEnable();
        return LuckPermsProvider.get();
    }

    public static void disable() {
        LPMinestomBootstrap instance;

        synchronized (LuckPermsMinestom.class) {
            instance = bootstrap;
            bootstrap = null;
        }

        if (instance != null) {
            instance.onDisable();
        }
    }
}
