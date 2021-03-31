/*
 * Configurate
 * Copyright (C) zml and Configurate contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spongepowered.configurate.yaml;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.CommentedConfigurationNodeIntermediary;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationNodeFactory;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.loader.ParsingException;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.events.AliasEvent;
import org.yaml.snakeyaml.events.CollectionStartEvent;
import org.yaml.snakeyaml.events.CommentEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.NodeEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.scanner.ScannerImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class YamlParser extends ParserImpl {

    private final Map<String, ConfigurationNode> aliases = new HashMap<>();
    private final TagRepository tags;
    private @Nullable StringBuilder commentCollector;
    private final boolean processComments;

    YamlParser(final StreamReader reader, final TagRepository tags) {
        super(new ScannerImpl(reader).setAcceptTabs(true));
        this.processComments = true; // todo: make configurable
        this.tags = tags;
    }

    private ScannerImpl scanner() {
        return (ScannerImpl) this.scanner;
    }

    Event requireEvent(final Event.ID type) throws ParsingException {
        final Event next = peekEvent();
        if (!next.is(type)) {
            throw makeError(next.getStartMark(), "Expected next event of type" + type + " but was " + next.getEventId(), null);
        }
        return this.getEvent();
    }

    @SuppressWarnings("unchecked")
    <T extends Event> T requireEvent(final Event.ID type, final Class<T> clazz) throws ParsingException {
        final Event next = peekEvent();
        if (!next.is(type)) {
            throw makeError(next.getStartMark(), "Expected next event of type" + type + " but was " + next.getEventId(), null);
        }
        if (!clazz.isInstance(next)) {
            throw makeError(next.getStartMark(), "Expected event of type " + clazz + " but got a " + next.getClass(), null);
        }

        return (T) this.getEvent();
    }

    private void consumeComments(final ConfigurationNode node, final boolean collectFirst) {
        if (!(node instanceof CommentedConfigurationNodeIntermediary<?>)) {
            return; // no comments are even collected
        }
        if (collectFirst) {
            this.skipComments();
        }

        if (this.commentCollector != null && this.commentCollector.length() > 0) {
            final StringBuilder collector = this.commentCollector;
            ((CommentedConfigurationNodeIntermediary<?>) node).comment(collector.toString());
            collector.delete(0, collector.length());
        }
        this.skipComments();
    }

    private void skipComments() {
        if (!this.processComments || !this.scanner().isEmitComments()) {
            return;
        }

        while (peekEvent().is(Event.ID.Comment)) {
            final CommentEvent event = (CommentEvent) getEvent();
            if (event.getCommentType() != CommentType.BLANK_LINE) {
                @Nullable StringBuilder commentCollector = this.commentCollector;
                if (commentCollector == null) {
                    this.commentCollector = commentCollector = new StringBuilder();
                }
                if (commentCollector.length() > 0) {
                    commentCollector.append(AbstractConfigurationLoader.CONFIGURATE_LINE_SEPARATOR);
                }
                if (event.getValue().startsWith(" ")) {
                    commentCollector.append(event.getValue(), 1, event.getValue().length());
                } else {
                    commentCollector.append(event.getValue());
                }
            }
        }
    }

    public <N extends ConfigurationNode> Stream<N> stream(final ConfigurationNodeFactory<N> factory) throws ParsingException {
        requireEvent(Event.ID.StreamStart);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<N>() {
            @Override
            public boolean hasNext() {
                return !checkEvent(Event.ID.StreamEnd);
            }

            @Override
            public N next() {
                if (!hasNext()) {
                    throw new IndexOutOfBoundsException();
                }
                try {
                    final N node = factory.createNode();
                    document(node);
                    return node;
                } catch (final ConfigurateException e) {
                    throw new RuntimeException(e); // TODO
                }
            }
        }, Spliterator.IMMUTABLE | Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    public void singleDocumentStream(final ConfigurationNode node) throws ParsingException {
        requireEvent(Event.ID.StreamStart);
        document(node);
        requireEvent(Event.ID.StreamEnd);
    }

    public void document(final ConfigurationNode node) throws ParsingException {
        if (peekEvent().is(Event.ID.StreamEnd)) {
            return;
        }

        requireEvent(Event.ID.DocumentStart);
        if (this.processComments && node instanceof CommentedConfigurationNodeIntermediary<?>) {
            // Only collect comments if we can handle them in the first place
            this.scanner().setEmitComments(true);
        }
        try {
            value(node);
        } catch (final ConfigurateException ex) {
            ex.initPath(node::path);
            throw ex;
        } finally {
            this.scanner().setEmitComments(false);
            this.aliases.clear();
        }
        requireEvent(Event.ID.DocumentEnd);
    }

    void value(final ConfigurationNode node) throws ParsingException {
        this.value(node, true);
    }

    void value(final ConfigurationNode node, final boolean commentHolder) throws ParsingException {
        if (commentHolder) {
            this.consumeComments(node, false);
        } else {
            this.skipComments();
        }
        final Event peeked = peekEvent();
        // extract event metadata
        if (peeked instanceof NodeEvent && !(peeked instanceof AliasEvent)) {
            final String anchor = ((NodeEvent) peeked).getAnchor();
            if (anchor != null) {
                node.hint(YamlConfigurationLoader.ANCHOR_ID, anchor);
                this.aliases.put(anchor, node);
            }
            if (peeked instanceof CollectionStartEvent) {
                node.hint(YamlConfigurationLoader.NODE_STYLE, NodeStyle.fromSnakeYaml(((CollectionStartEvent) peeked).getFlowStyle()));
            }
        }

        // then handle the value
        try {
            switch (peeked.getEventId()) {
                case Scalar:
                    scalar(node);
                    break;
                case MappingStart:
                    mapping(node);
                    break;
                case SequenceStart:
                    sequence(node);
                    break;
                case Alias:
                    alias(node);
                    break;
                default:
                    throw makeError(node, peeked.getStartMark(), "Unexpected event type " + peeked.getEventId(), null);
            }
        } catch (final MarkedYAMLException ex) {
            throw new ParsingException(node, ex.getProblemMark().getLine(), ex.getProblemMark().getColumn(), ex.getProblemMark().get_snippet(), ex.getProblem());
        }
    }

    void scalar(final ConfigurationNode node) throws ParsingException {
        final ScalarEvent scalar = requireEvent(Event.ID.Scalar, ScalarEvent.class);
        node.hint(YamlConfigurationLoader.SCALAR_STYLE, ScalarStyle.fromSnakeYaml(scalar.getScalarStyle()));
        node.raw(scalar.getValue()); // TODO: tags and value types
    }

    void mapping(final ConfigurationNode node) throws ParsingException {
        requireEvent(Event.ID.MappingStart);

        node.raw(Collections.emptyMap());
        final ConfigurationNode keyHolder = BasicConfigurationNode.root(node.options());
        while (!checkEvent(Event.ID.MappingEnd)) {
            value(keyHolder, false);
            // TODO: Merge key?
            final ConfigurationNode child = node.node(keyHolder.raw());
            if (!child.virtual()) { // duplicate keys are forbidden (3.2.1.3)
                throw makeError(node, this.scanner.peekToken().getStartMark(), "Duplicate key '" + child.key() + "' encountered!", null);
            }
            value(child);
            skipComments();
        }

        requireEvent(Event.ID.MappingEnd);
    }

    void sequence(final ConfigurationNode node) throws ParsingException {
        requireEvent(Event.ID.SequenceStart);
        node.raw(Collections.emptyList());

        while (!checkEvent(Event.ID.SequenceEnd)) {
            value(node.appendListNode());
            skipComments();
        }

        requireEvent(Event.ID.SequenceEnd);
    }

    void alias(final ConfigurationNode node) throws ParsingException {
        final AliasEvent event = requireEvent(Event.ID.Alias, AliasEvent.class);
        final ConfigurationNode target = this.aliases.get(event.getAnchor());
        if (target == null) {
            throw makeError(node, event.getStartMark(), "Unknown anchor '" + event.getAnchor() + "'", null);
        }
        node.from(target); // TODO: Reference node types
        node.hint(YamlConfigurationLoader.ANCHOR_ID, null); // don't duplicate alias
    }

    private ParsingException makeError(final Mark mark, final @Nullable String message, final @Nullable Throwable error) {
        return new ParsingException(mark.getLine(), mark.getColumn(), mark.get_snippet(), message, error);
    }

    private ParsingException makeError(
        final ConfigurationNode node,
        final Mark mark,
        final @Nullable String message,
        final @Nullable Throwable error
    ) {
        return new ParsingException(node, mark.getLine(), mark.getColumn(), mark.get_snippet(), message, error);
    }

}
