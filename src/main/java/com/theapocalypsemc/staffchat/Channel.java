/*
 * Copyright (c) 2015 The Apocalypse MC. All rights reserved. Internal use only.
 */

package com.theapocalypsemc.staffchat;

import java.util.List;

/**
 * A certain chat channel. This contains data such as the permission and color.
 * The players in this channel are stored in ChatManager - this is merely a data
 * class.
 *
 * @author SirFaizdat
 */
public class Channel {

    private String name, permission, color;
    private List<String> commands;

    public Channel(String name, String permission, List<String> commands, String color) {
        this.name = name;
        this.permission = permission;
        this.commands = commands;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public List<String> getCommands() {
        return commands;
    }

    public String getColor() {
        return color;
    }

}
