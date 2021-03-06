/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.tests.server.component.label;

import junit.framework.TestCase;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Test;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.declarative.DesignContext;

/**
 * Tests generating an html tree node corresponding to a Label.
 */
public class TestWriteDesign extends TestCase {

    private DesignContext ctx;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ctx = new DesignContext();
    }

    @Test
    public void testWithContent() {
        createAndTestLabel("A label", null);
    }

    @Test
    public void testWithHtmlContent() {
        createAndTestLabel("<b>A label</b>", null);
    }

    @Test
    public void testWithCaption() {
        createAndTestLabel(null, "Label caption");
    }

    @Test
    public void testWithContentAndCaption() {
        createAndTestLabel("A label", "Label caption");
    }

    @Test
    public void testContentModeText() {
        Label l = new Label("plain text label");
        Element e = new Element(Tag.valueOf("v-label"), "", new Attributes());
        l.writeDesign(e, ctx);
        assertTrue("Label should be marked as plain text",
                e.hasAttr("plain-text"));
    }

    @Test
    public void testContentModeHtml() {
        Label l = new Label("html label");
        l.setContentMode(ContentMode.HTML);

        Element e = new Element(Tag.valueOf("v-label"), "", new Attributes());
        l.writeDesign(e, ctx);
        assertFalse("Label should not be marked as plain text",
                e.hasAttr("plain-text"));
    }

    @Test
    public void testChangeContentMode() {
        Label l = new Label("html label");
        l.setContentMode(ContentMode.HTML);

        Element e = new Element(Tag.valueOf("v-label"), "", new Attributes());
        l.writeDesign(e, ctx);

        assertFalse("Label should not be marked as plain text",
                e.hasAttr("plain-text"));
        l.setContentMode(ContentMode.TEXT);
        l.writeDesign(e, ctx);

        assertTrue("Label should be marked as plain text",
                e.hasAttr("plain-text"));
    }

    @Test
    public void testWithoutContentAndCaption() {
        createAndTestLabel(null, null);
    }

    private void createAndTestLabel(String content, String caption) {
        Label l = new Label(content);
        if (caption != null) {
            l.setCaption(caption);
        }
        Element e = ctx.createElement(l);
        assertEquals("Wrong tag name for label.", "v-label", e.tagName());
        if (content != null) {
            assertEquals("Unexpected content in the v-label element.", content,
                    e.html());
        } else {
            assertTrue("Unexpected content in the v-label element.",
                    e.html() == null || "".equals(e.html()));
        }
        if (caption != null) {
            assertEquals("Wrong caption in the v-label element.", caption,
                    e.attr("caption"));
        } else {
            assertTrue("Unexpected caption in the v-label element.",
                    e.attr("caption") == null || "".equals(e.attr("caption")));
        }
    }
}