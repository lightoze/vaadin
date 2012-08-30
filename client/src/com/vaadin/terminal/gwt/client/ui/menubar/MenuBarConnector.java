/*
 * Copyright 2011 Vaadin Ltd.
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
package com.vaadin.terminal.gwt.client.ui.menubar;

import java.util.Iterator;
import java.util.Stack;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;
import com.vaadin.shared.ui.Connect;
import com.vaadin.shared.ui.Connect.LoadStyle;
import com.vaadin.shared.ui.menubar.MenuBarConstants;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.TooltipInfo;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.Util;
import com.vaadin.terminal.gwt.client.ui.AbstractComponentConnector;
import com.vaadin.terminal.gwt.client.ui.Icon;
import com.vaadin.terminal.gwt.client.ui.SimpleManagedLayout;

@Connect(value = com.vaadin.ui.MenuBar.class, loadStyle = LoadStyle.LAZY)
public class MenuBarConnector extends AbstractComponentConnector implements
        Paintable, SimpleManagedLayout {

    /**
     * This method must be implemented to update the client-side component from
     * UIDL data received from server.
     * 
     * This method is called when the page is loaded for the first time, and
     * every time UI changes in the component are received from the server.
     */
    @Override
    public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
        if (!isRealUpdate(uidl)) {
            return;
        }

        getWidget().htmlContentAllowed = uidl
                .hasAttribute(MenuBarConstants.HTML_CONTENT_ALLOWED);

        getWidget().openRootOnHover = uidl
                .getBooleanAttribute(MenuBarConstants.OPEN_ROOT_MENU_ON_HOWER);

        getWidget().enabled = isEnabled();

        // For future connections
        getWidget().client = client;
        getWidget().uidlId = uidl.getId();

        // Empty the menu every time it receives new information
        if (!getWidget().getItems().isEmpty()) {
            getWidget().clearItems();
        }

        UIDL options = uidl.getChildUIDL(0);

        if (null != getState() && !getState().isUndefinedWidth()) {
            UIDL moreItemUIDL = options.getChildUIDL(0);
            StringBuffer itemHTML = new StringBuffer();

            if (moreItemUIDL.hasAttribute("icon")) {
                itemHTML.append("<img src=\""
                        + Util.escapeAttribute(client
                                .translateVaadinUri(moreItemUIDL
                                        .getStringAttribute("icon")))
                        + "\" class=\"" + Icon.CLASSNAME + "\" alt=\"\" />");
            }

            String moreItemText = moreItemUIDL.getStringAttribute("text");
            if ("".equals(moreItemText)) {
                moreItemText = "&#x25BA;";
            }
            itemHTML.append(moreItemText);

            getWidget().moreItem = GWT.create(VMenuBar.CustomMenuItem.class);
            getWidget().moreItem.setHTML(itemHTML.toString());
            getWidget().moreItem.setCommand(VMenuBar.emptyCommand);

            getWidget().collapsedRootItems = new VMenuBar(true, getWidget());
            getWidget().moreItem.setSubMenu(getWidget().collapsedRootItems);
            getWidget().moreItem.addStyleName(VMenuBar.CLASSNAME
                    + "-more-menuitem");
        }

        UIDL uidlItems = uidl.getChildUIDL(1);
        Iterator<Object> itr = uidlItems.getChildIterator();
        Stack<Iterator<Object>> iteratorStack = new Stack<Iterator<Object>>();
        Stack<VMenuBar> menuStack = new Stack<VMenuBar>();
        VMenuBar currentMenu = getWidget();

        while (itr.hasNext()) {
            UIDL item = (UIDL) itr.next();
            VMenuBar.CustomMenuItem currentItem = null;

            final int itemId = item.getIntAttribute("id");

            boolean itemHasCommand = item.hasAttribute("command");
            boolean itemIsCheckable = item
                    .hasAttribute(MenuBarConstants.ATTRIBUTE_CHECKED);

            String itemHTML = getWidget().buildItemHTML(item);

            Command cmd = null;
            if (!item.hasAttribute("separator")) {
                if (itemHasCommand || itemIsCheckable) {
                    // Construct a command that fires onMenuClick(int) with the
                    // item's id-number
                    cmd = new Command() {
                        @Override
                        public void execute() {
                            getWidget().hostReference.onMenuClick(itemId);
                        }
                    };
                }
            }

            currentItem = currentMenu.addItem(itemHTML.toString(), cmd);
            currentItem.updateFromUIDL(item, client);

            if (item.getChildCount() > 0) {
                menuStack.push(currentMenu);
                iteratorStack.push(itr);
                itr = item.getChildIterator();
                currentMenu = new VMenuBar(true, currentMenu);
                client.getVTooltip().connectHandlersToWidget(currentMenu);
                // this is the top-level style that also propagates to items -
                // any item specific styles are set above in
                // currentItem.updateFromUIDL(item, client)
                if (getState().hasStyles()) {
                    for (String style : getState().getStyles()) {
                        currentMenu.addStyleDependentName(style);
                    }
                }
                currentItem.setSubMenu(currentMenu);
            }

            while (!itr.hasNext() && !iteratorStack.empty()) {
                boolean hasCheckableItem = false;
                for (VMenuBar.CustomMenuItem menuItem : currentMenu.getItems()) {
                    hasCheckableItem = hasCheckableItem
                            || menuItem.isCheckable();
                }
                if (hasCheckableItem) {
                    currentMenu.addStyleDependentName("check-column");
                } else {
                    currentMenu.removeStyleDependentName("check-column");
                }

                itr = iteratorStack.pop();
                currentMenu = menuStack.pop();
            }
        }// while

        getLayoutManager().setNeedsHorizontalLayout(this);

    }// updateFromUIDL

    @Override
    public VMenuBar getWidget() {
        return (VMenuBar) super.getWidget();
    }

    @Override
    public void layout() {
        getWidget().iLayout();
    }

    @Override
    public TooltipInfo getTooltipInfo(Element element) {
        TooltipInfo info = null;

        // Check content of widget to find tooltip for element
        if (element != getWidget().getElement()) {

            VMenuBar.CustomMenuItem item = getWidget().getMenuItemWithElement(
                    (com.google.gwt.user.client.Element) element);
            if (item != null) {
                info = item.getTooltip();
            }
        }

        // Use default tooltip if nothing found from DOM three
        if (info == null) {
            info = super.getTooltipInfo(element);
        }

        return info;
    }
}