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
package org.spongepowered.configurate.yaml

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNull

import org.junit.jupiter.api.Test
import org.spongepowered.configurate.CommentedConfigurationNode

class CommentTest implements YamlTest {

    @Test
    void testLoadScalarComment() {
        final CommentedConfigurationNode node = parseString(normalize("""
            # Hello world
            "i'm a string"
            """))

        assertEquals("Hello world", node.comment())
        assertEquals("i'm a string", node.raw())
    }

    @Test
    void testLoadBlockMappingComment() {
        final CommentedConfigurationNode node = parseString(normalize("""
            test:
                # meow
                cat: purrs
            """))

        assertEquals("purrs", node.node("test", "cat").raw())
        assertEquals("meow", node.node("test", "cat").comment())
    }

    @Test
    void testLoadBlockScalarSequenceComment() {
        final CommentedConfigurationNode test = parseString(normalize("""
            - first
            # i matter less
            - second
            - third
            # we skipped one
            - fourth
            """))

        assertNull(test[0].comment())
        assertEquals("i matter less", test[1].comment())
        assertEquals("we skipped one", test[3].comment())
    }

    @Test
    void testLoadScalarCommentsInBlockMapping() {
        final CommentedConfigurationNode test = parseString("""
            blah:
            # beginning sequence
            - # first on map entry
              test: hello
            - # on second mapping
              test2: goodbye
            """.stripIndent(true))

        final CommentedConfigurationNode child = test.node("blah", 0)
        assertFalse(child.virtual())
        assertEquals("beginning sequence", child.comment())
        assertEquals("first on map entry", child.node("test").comment())
        assertEquals("on second mapping", child.node("test2").comment())
    }

    // flow collections are a bit trickier
    // we can't really do comments on one line, so these all have to have a line per element

    @Test
    void testLoadCommentInFlowMapping() {
        final CommentedConfigurationNode test = parseString(normalize("""
            {
                # hello
                test: value,
                uncommented: thing,
                #hi there
                last: bye
            }
        """))

        assertEquals("hello", test.node("test").comment())
        assertNull(test.node("uncommented").comment())
        assertEquals("hi there", test.node("last").comment())
    }

    @Test
    void testLoadCommentInFlowSequence() {
        final CommentedConfigurationNode test = parseString(normalize("""
            # on list
            [
                # first
                'first entry',
                # second
                'second entry'
            ]
        """))

        assertEquals("on list", test.comment())
        assertEquals("first", test.node(0).comment())
        assertEquals("second", test.node(1).comment())
    }

    @Test
    void testLoadMixedStructure() {
        final CommentedConfigurationNode test = parseResource(getClass().getResource("comments-complex.yml"))

        assertEquals("very mapping", test["core", "users", 0, "second"].comment())
    }

    @Test
    void testWriteScalarCommented() {
        final CommentedConfigurationNode node = CommentedConfigurationNode.root()
                .raw("test")
                .comment("i have a comment")

        assertEquals(normalize("""
        # i have a comment
        test"""), dump(node).trim())
    }

    @Test
    void testWriteBlockMappingCommented() {
        final CommentedConfigurationNode node = CommentedConfigurationNode.root {
            node("a").set("Hello").comment("I'm first")
            node("b", "one").set("World")
            node("b", "two").set("eee").comment("also me")
        }

        assertEquals(
            normalize("""
                # I'm first
                a: Hello
                b:
                    one: World
                    # also me
                    two: eee
                """),
            dump(node, NodeStyle.BLOCK)
        )
    }

    @Test
    void testWriteBlockSequence() {
        final def node = CommentedConfigurationNode.root {
            appendListNode().set("Hello")
            appendListNode().set("World")
            appendListNode().act {
                it.node("one").set("aaa")
                it.node("two").set("bbb")
            }
        }

        final def expected = normalize("""
        - Hello
        - World
        -   one: aaa
            two: bbb
        """)
        assertEquals(expected, this.dump(node, NodeStyle.BLOCK))
    }

    @Test
    void testWriteBlockSequenceCommented() {
        final def node = CommentedConfigurationNode.root {
            appendListNode().set("red").comment("A colour")
            appendListNode().set("orange").comment("Another colour")
            appendListNode().set("yellow").comment("What? a THIRD colour???")
        }

        final def expected = normalize("""
        # A colour
        - red
        # Another colour
        - orange
        # What? a THIRD colour???
        - yellow
        """)
        assertEquals(expected, this.dump(node, NodeStyle.BLOCK))
    }

    @Test
    void testWriteFlowMappingCommented() {
        final CommentedConfigurationNode node = CommentedConfigurationNode.root {
            node("a").set("Hello").comment("I'm first")
            node("b", "one").set("World")
            node("b", "two").set("eee").comment("also me")
        }

        final def expected = normalize("""{
            # I'm first
            a: Hello,
            b: {one: World,
                # also me
                two: eee } }
            """)
        assertEquals(expected, dump(node, NodeStyle.FLOW))
    }

    @Test
    void testWriteFlowSequenceCommented() {
        final def node = CommentedConfigurationNode.root {
            appendListNode().set("red").comment("A colour")
            appendListNode().set("orange").comment("Another colour")
            appendListNode().set("yellow").comment("What? a THIRD colour???")
        }

        final def expected = normalize("""
            [
            # A colour
            red,
            # Another colour
            orange,
            # What? a THIRD colour???
            yellow
            ]""")
        assertEquals(expected, this.dump(node, NodeStyle.FLOW))
    }

}
