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

import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.dependencies.DependencyManager;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager;
import me.lucko.luckperms.common.model.manager.user.StandardUserManager;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.treeview.PermissionRegistry;
import me.lucko.luckperms.minestom.CommandRegistry;
import me.lucko.luckperms.minestom.LuckPermsMinestom;
import me.lucko.luckperms.minestom.calculator.MinestomCalculatorFactory;
import me.lucko.luckperms.minestom.context.ContextProvider;
import me.lucko.luckperms.minestom.context.MinestomContextManager;
import me.lucko.luckperms.minestom.context.MinestomPlayerCalculator;
import me.lucko.luckperms.minestom.listeners.MinestomCommandListUpdater;
import me.lucko.luckperms.minestom.listeners.MinestomConnectionListener;
import me.lucko.luckperms.minestom.messaging.MinestomMessagingFactory;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.query.QueryOptions;
import net.minestom.conduit.Conduit;
import net.minestom.conduit.ConduitBootstrap;
import net.minestom.conduit.permissions.PermissionManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class LPMinestomPlugin extends AbstractLuckPermsPlugin {

    private final EventNode<Event> eventNode = EventNode.all("luckperms");

    private final LPMinestomBootstrap bootstrap;
    private final Set<ContextProvider> contextProviders;
    private final @NotNull Set<String> permissionSuggestions;
    private final @NotNull CommandRegistry commandRegistry;

    private MinestomSenderFactory senderFactory;
    private MinestomContextManager contextManager;
    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;
    private MinestomCommandExecutor commandManager;
    private MinestomConnectionListener connectionListener;

    LPMinestomPlugin(
            @NotNull LPMinestomBootstrap bootstrap,
            @NotNull Set<ContextProvider> contextProviders,
            @NotNull Set<String> permissionSuggestions,
            @NotNull CommandRegistry commandRegistry
    ) {
        this.bootstrap = bootstrap;
        this.contextProviders = contextProviders;
        this.permissionSuggestions = permissionSuggestions;
        this.commandRegistry = commandRegistry;
    }

    @Override
    protected DependencyManager createDependencyManager() {
        return new DependencyManager() {
            @Override public void loadDependencies(Set<Dependency> d) {}
            @Override public void loadStorageDependencies(Set<StorageType> s, boolean r, boolean rb, boolean n) {}
            @Override public ClassLoader obtainClassLoaderWith(Set<Dependency> d) { return getClass().getClassLoader(); }
            @Override public void close() {}
        };
    }

    @Override
    protected void setupSenderFactory() {
        this.senderFactory = new MinestomSenderFactory(this);
    }

    @Override
    protected ConfigurationAdapter provideConfigurationAdapter() {
        return new MinestomConfigAdapter(this, resolveConfig("config.yml"));
    }

    @Override
    protected void registerPlatformListeners() {
        this.connectionListener = new MinestomConnectionListener(this, this.eventNode);
        MinecraftServer.getGlobalEventHandler().addChild(this.eventNode);
    }

    @Override
    protected MessagingFactory<?> provideMessagingFactory() {
        return new MinestomMessagingFactory(this, this.eventNode);
    }

    @Override
    protected void registerCommands() {
        this.commandManager = new MinestomCommandExecutor(this, this.commandRegistry);
        this.commandManager.register();
    }

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new MinestomCalculatorFactory(this, getConfiguration());
    }

    @Override
    protected void setupContextManager() {
        this.contextManager = new MinestomContextManager(this);

        Set<String> disabledContexts = getConfiguration().get(ConfigKeys.DISABLED_CONTEXTS);
        this.contextManager.registerCalculator(
                new MinestomPlayerCalculator(this, this.eventNode, this.contextProviders, disabledContexts)
        );
    }

    @Override
    protected void setupPlatformHooks() {
        MinecraftServer.getSchedulerManager().buildShutdownTask(LuckPermsMinestom::disable);
    }

    @Override
    protected void removePlatformHooks() {
        if (this.commandManager != null) {
            this.commandManager.unregister();
        }
        MinecraftServer.getGlobalEventHandler().removeChild(this.eventNode);
    }

    @Override
    protected AbstractEventBus<?> provideEventBus(LuckPermsApiProvider apiProvider) {
        return new MinestomEventBus(this, apiProvider);
    }

    @Override
    protected void registerApiOnPlatform(LuckPerms api) {
        try {
            Conduit.services();
        } catch (IllegalStateException e) {
            ConduitBootstrap.init();
        }

        Conduit.services().register(PermissionManager.class, new LPPermissionProvider(api));
    }

    @Override
    protected void performFinalSetup() {
        PermissionRegistry permissionRegistry = getPermissionRegistry();
        this.permissionSuggestions.forEach(permissionRegistry::offer);

        if (getConfiguration().get(ConfigKeys.UPDATE_CLIENT_COMMAND_LIST)) {
            getApiProvider().getEventBus().subscribe(new MinestomCommandListUpdater(this));
        }
    }

    @Override
    public @NotNull LPMinestomBootstrap getBootstrap() {
        return this.bootstrap;
    }

    @Override
    public StandardUserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public StandardGroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public StandardTrackManager getTrackManager() {
        return this.trackManager;
    }

    @Override
    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    @Override
    public AbstractConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    @Override
    public MinestomContextManager getContextManager() {
        return this.contextManager;
    }

    @Override
    public Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId())
                .map(player -> this.contextManager.getQueryOptions((Player) player));
    }

    @Override
    public Stream<Sender> getOnlineSenders() {
        Collection<Player> players = MinecraftServer.getConnectionManager().getOnlinePlayers();
        return Stream.concat(
                Stream.of(getConsoleSender()),
                players.stream().map(getSenderFactory()::wrap)
        );
    }

    @Override
    public Sender getConsoleSender() {
        return getSenderFactory().wrap(MinecraftServer.getCommandManager().getConsoleSender());
    }

    public MinestomSenderFactory getSenderFactory() {
        return this.senderFactory;
    }

}
