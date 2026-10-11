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

import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.minestom.CommandRegistry;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MinestomCommandExecutor extends CommandManager {

    private final @NotNull LuckPermsCommand command = new LuckPermsCommand();

    private final @NotNull LPMinestomPlugin plugin;
    private final @NotNull CommandRegistry registry;

    public MinestomCommandExecutor(@NotNull LPMinestomPlugin plugin, @NotNull CommandRegistry registry) {
        super(plugin);
        this.plugin = plugin;
        this.registry = registry;
    }

    public void register() {
        this.registry.register(this.command);
    }

    public void unregister() {
        this.registry.unregister(this.command);
    }

    private final class LuckPermsCommand extends Command {

        public LuckPermsCommand() {
            super("luckperms", "lp", "perm", "perms", "permission", "permissions");

            final var params = ArgumentType.StringArray("params");

            params.setSuggestionCallback((sender, context, suggestion) -> {
                Sender wrapped = MinestomCommandExecutor.this.plugin.getSenderFactory().wrap(sender);
                String input = context.getInput();
                String[] split = input.split(" ", 2);
                String args = split.length > 1 ? split[1] : "";
                List<String> arguments = ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(args);
                MinestomCommandExecutor.this.tabCompleteCommand(wrapped, arguments)
                        .stream()
                        .map(SuggestionEntry::new)
                        .forEach(suggestion::addEntry);
            });

            this.setDefaultExecutor((sender, context) -> process(sender, context.getCommandName(), new String[0]));
            this.addSyntax((sender, context) -> process(sender, context.getCommandName(), context.get(params)), params);
        }

        public void process(@NotNull CommandSender sender, @NotNull String command, String @NotNull [] args) {
            List<String> arguments = ArgumentTokenizer.EXECUTE.tokenizeInput(args);
            MinestomCommandExecutor.this.executeCommand(
                    MinestomCommandExecutor.this.plugin.getSenderFactory().wrap(sender),
                    command,
                    arguments
            );
        }
    }
}
