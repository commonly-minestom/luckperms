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

package me.lucko.luckperms.minestom.context;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A provider which resolves a single LuckPerms context key for a Minestom player.
 *
 * <p>Providers are a Minestom-specific extension point which allow the application
 * to register custom LuckPerms contexts without having to deal with the event
 * system directly. A provider can optionally signal when its value has changed, so
 * that LuckPerms can invalidate the affected player's cached data.</p>
 */
public interface ContextProvider {

    /**
     * Gets the LuckPerms context key this provider resolves.
     *
     * @return the context key
     */
    @NotNull String key();

    /**
     * Resolves the context value for the given player.
     *
     * @param subject the player
     * @return the context value, if present
     */
    @NotNull Optional<String> query(@NotNull Player subject);

    /**
     * Gets all possible values which {@link #query(Player)} could return.
     *
     * @return the possible values
     */
    default @NotNull Set<String> potentialValues() {
        return Set.of();
    }

    /**
     * Registers listeners which signal when the context value may have changed.
     *
     * @param contextUpdateSignaller a consumer which should be invoked when the
     *                               player's context has potentially changed
     * @param eventNode the event node to register listeners on
     */
    default void register(@NotNull Consumer<Player> contextUpdateSignaller, @NotNull EventNode<Event> eventNode) {
    }

}
