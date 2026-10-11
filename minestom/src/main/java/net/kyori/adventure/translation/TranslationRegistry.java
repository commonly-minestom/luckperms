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

package net.kyori.adventure.translation;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.util.TriState;

/**
 * Compatibility shim for the TranslationRegistry interface removed in Adventure 5,
 * backed by the Adventure 5 TranslationStore API
 */
public interface TranslationRegistry extends TranslationStore.StringBased<MessageFormat> {

    static TranslationRegistry create(Key key) {
        return new MessageFormatStore(TranslationStore.messageFormat(key));
    }

    final class MessageFormatStore implements TranslationRegistry {

        private final TranslationStore.StringBased<MessageFormat> backing;

        private MessageFormatStore(TranslationStore.StringBased<MessageFormat> backing) {
            this.backing = backing;
        }

        @Override
        public void registerAll(Locale locale, Path path, boolean allowPartial) {
            this.backing.registerAll(locale, path, allowPartial);
        }

        @Override
        public void registerAll(Locale locale, ResourceBundle bundle, boolean allowPartial) {
            this.backing.registerAll(locale, bundle, allowPartial);
        }

        @Override
        public void registerAll(Locale locale, Map<String, MessageFormat> translations) {
            this.backing.registerAll(locale, translations);
        }

        @Override
        public void registerAll(Locale locale, Set<String> keys, Function<String, MessageFormat> function) {
            this.backing.registerAll(locale, keys, function);
        }

        @Override
        public void register(String key, Locale locale, MessageFormat translation) {
            this.backing.register(key, locale, translation);
        }

        @Override
        public void unregister(String key) {
            this.backing.unregister(key);
        }

        @Override
        public boolean contains(String key) {
            return this.backing.contains(key);
        }

        @Override
        public boolean contains(String key, Locale locale) {
            return this.backing.contains(key, locale);
        }

        @Override
        public boolean canTranslate(String key, Locale locale) {
            return this.backing.canTranslate(key, locale);
        }

        @Override
        public void defaultLocale(Locale locale) {
            this.backing.defaultLocale(locale);
        }

        @Override
        public Key name() {
            return this.backing.name();
        }

        @Override
        public TriState hasAnyTranslations() {
            return this.backing.hasAnyTranslations();
        }

        @Override
        public MessageFormat translate(String key, Locale locale) {
            return this.backing.translate(key, locale);
        }

        @Override
        public Component translate(TranslatableComponent component, Locale locale) {
            return this.backing.translate(component, locale);
        }
    }
}
