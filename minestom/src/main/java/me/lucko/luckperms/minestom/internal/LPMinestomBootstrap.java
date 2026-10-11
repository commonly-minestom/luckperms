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

import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.classpath.ClassPathAppender;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.plugin.logging.Slf4jPluginLogger;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.util.BuildInfo;
import me.lucko.luckperms.minestom.CommandRegistry;
import me.lucko.luckperms.minestom.context.ContextProvider;
import net.luckperms.api.platform.Platform;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public final class LPMinestomBootstrap implements LuckPermsBootstrap {

    private final Path dataDirectory;
    private final PluginLogger logger;
    private final SchedulerAdapter schedulerAdapter;
    private final ClassPathAppender classPathAppender;
    private final LPMinestomPlugin plugin;

    private final CountDownLatch loadLatch = new CountDownLatch(1);
    private final CountDownLatch enableLatch = new CountDownLatch(1);

    private Instant startTime;

    public LPMinestomBootstrap(
            @NotNull Logger logger,
            @NotNull Path dataDirectory,
            @NotNull Set<ContextProvider> contextProviders,
            @NotNull Set<String> permissionSuggestions,
            @NotNull CommandRegistry commandRegistry
    ) {
        this(logger, dataDirectory, contextProviders, permissionSuggestions, commandRegistry, _ -> {});
    }

    public LPMinestomBootstrap(
            @NotNull Logger logger,
            @NotNull Path dataDirectory,
            @NotNull Set<ContextProvider> contextProviders,
            @NotNull Set<String> permissionSuggestions,
            @NotNull CommandRegistry commandRegistry,
            @NotNull ClassPathAppender classPathAppender
    ) {
        this.logger = new Slf4jPluginLogger(logger);
        this.dataDirectory = dataDirectory;
        this.schedulerAdapter = new MinestomSchedulerAdapter(this);
        this.classPathAppender = classPathAppender;
        this.plugin = new LPMinestomPlugin(this, contextProviders, permissionSuggestions, commandRegistry);
    }

    public void onEnable() {
        this.startTime = Instant.now();
        try {
            this.plugin.load();
        } finally {
            this.loadLatch.countDown();
        }

        try {
            this.plugin.enable();
        } finally {
            this.enableLatch.countDown();
        }
    }

    public void onDisable() {
        this.plugin.disable();
    }

    @Override
    public PluginLogger getPluginLogger() {
        return this.logger;
    }

    @Override
    public SchedulerAdapter getScheduler() {
        return this.schedulerAdapter;
    }

    @Override
    public ClassPathAppender getClassPathAppender() {
        return this.classPathAppender;
    }

    @Override
    public CountDownLatch getLoadLatch() {
        return this.loadLatch;
    }

    @Override
    public CountDownLatch getEnableLatch() {
        return this.enableLatch;
    }

    @Override
    public String getVersion() {
        return BuildInfo.VERSION;
    }

    @Override
    public Instant getStartupTime() {
        return this.startTime;
    }

    @Override
    public Platform.Type getType() {
        return Platform.Type.MINESTOM;
    }

    @Override
    public String getServerBrand() {
        return MinecraftServer.getBrandName();
    }

    @Override
    public String getServerVersion() {
        return MinecraftServer.VERSION_NAME;
    }

    @Override
    public @Nullable String getServerName() {
        return MinecraftServer.getBrandName();
    }

    @Override
    public Path getDataDirectory() {
        return this.dataDirectory;
    }

    @Override
    public Optional<?> getPlayer(UUID uniqueId) {
        return Optional.ofNullable(getConnectionManager().getOnlinePlayerByUuid(uniqueId));
    }

    @Override
    public Optional<UUID> lookupUniqueId(String username) {
        Player player = getConnectionManager().getOnlinePlayerByUsername(username);
        return player == null ? Optional.empty() : Optional.of(player.getUuid());
    }

    @Override
    public Optional<String> lookupUsername(UUID uniqueId) {
        Player player = getConnectionManager().getOnlinePlayerByUuid(uniqueId);
        return player == null ? Optional.empty() : Optional.of(player.getUsername());
    }

    @Override
    public int getPlayerCount() {
        return getConnectionManager().getOnlinePlayerCount();
    }

    @Override
    public Collection<String> getPlayerList() {
        List<String> list = new ArrayList<>();
        for (Player player : getConnectionManager().getOnlinePlayers()) {
            list.add(player.getUsername());
        }
        return list;
    }

    @Override
    public Collection<UUID> getOnlinePlayers() {
        List<UUID> list = new ArrayList<>();
        for (Player player : getConnectionManager().getOnlinePlayers()) {
            list.add(player.getUuid());
        }
        return list;
    }

    @Override
    public boolean isPlayerOnline(UUID uniqueId) {
        Player player = getConnectionManager().getOnlinePlayerByUuid(uniqueId);
        return player != null && player.isOnline();
    }

    private static ConnectionManager getConnectionManager() {
        return MinecraftServer.getConnectionManager();
    }

}
