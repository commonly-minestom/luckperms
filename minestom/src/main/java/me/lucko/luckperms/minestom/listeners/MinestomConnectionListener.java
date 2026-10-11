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

package me.lucko.luckperms.minestom.listeners;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.minestom.internal.LPMinestomPlugin;
import me.lucko.luckperms.minestom.internal.MinestomAdventureBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.network.player.GameProfile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class MinestomConnectionListener extends AbstractConnectionListener {

    private final LPMinestomPlugin plugin;

    public MinestomConnectionListener(LPMinestomPlugin plugin, EventNode<Event> eventNode) {
        super(plugin);
        this.plugin = plugin;

        eventNode.addListener(AsyncPlayerPreLoginEvent.class, this::onPlayerPreLogin);
        eventNode.addListener(AsyncPlayerConfigurationEvent.class, this::onPlayerConfiguration);
        eventNode.addListener(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    }

    private void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        /* wait for the plugin to enable. because these events are fired async, they can be called before
           the plugin has enabled. */
        try {
            this.plugin.getBootstrap().getEnableLatch().await(60, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        GameProfile profile = event.getGameProfile();
        UUID uuid = profile.uuid();
        String username = profile.name();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + uuid + " - " + username);
        }

        if (!event.getConnection().isOnline()) {
            /* another plugin has cancelled the connection. */
            this.plugin.getLogger().info("Another plugin has cancelled the connection for " + uuid + " - " + username + ". No permissions data will be loaded.");
            return;
        }

        /* Actually process the login for the connection.
           We do this here to delay the login until the data is ready.

           This includes:
           - loading uuid data
           - loading permissions
           - creating a user instance in the UserManager for this connection.
           - setting up cached data. */
        try {
            User user = loadUser(uuid, username);
            recordConnection(uuid);
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uuid, username, user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + uuid + " - " + username, ex);

            Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
            MinestomAdventureBridge.kick(event.getConnection(), GsonComponentSerializer.gson().serialize(reason));
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uuid, username, null);
        }
    }

    private void onPlayerConfiguration(AsyncPlayerConfigurationEvent event) {
        final Player player = event.getPlayer();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing login for " + player.getUuid() + " - " + player.getUsername());
        }

        User user = this.plugin.getUserManager().getIfLoaded(player.getUuid());

        if (user == null) {
            if (getUniqueConnections().contains(player.getUuid())) {
                this.plugin.getLogger().warn("User " + player.getUuid() + " - " + player.getUsername() +
                        " doesn't currently have data pre-loaded, but they have been processed before in this session.");
            }

            try {
                user = loadUser(player.getUuid(), player.getUsername());
                recordConnection(player.getUuid());
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(player.getUuid(), player.getUsername(), user);
            } catch (Exception ex) {
                this.plugin.getLogger().severe("Exception occurred whilst loading data for " + player.getUuid() + " - " + player.getUsername(), ex);

                if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                    Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build(), player.getLocale());
                    MinestomAdventureBridge.kick(player, GsonComponentSerializer.gson().serialize(reason));
                    return;
                }
            }
        }

        this.plugin.getContextManager().signalContextUpdate(player);
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        final Player player = event.getPlayer();
        this.handleDisconnect(player.getUuid());
    }

}
