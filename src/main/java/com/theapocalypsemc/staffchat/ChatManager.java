/*
 * Copyright (c) 2015 The Apocalypse MC. All rights reserved. Internal use only.
 */

package com.theapocalypsemc.staffchat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores which players have which mode enabled.
 *
 * @author SirFaizdat
 */
public class ChatManager {

    // <String = Player name, String = Channel>
    private Map<String, String> playersAndChannels;

    public ChatManager() {
        playersAndChannels = new HashMap<>();
    }

    public void enable(String player, String channel) {
        playersAndChannels.put(player, channel);
    }

    public void disable(String player) {
        playersAndChannels.remove(player);
    }

    /**
     * Gets the channel that the player is in.
     *
     * @param player The name of the player.
     * @return The name of the channel
     */
    public String getChannel(String player) {
        if(!playersAndChannels.containsKey(player)) return null;
        return playersAndChannels.get(player).toLowerCase(); // Return the channel name.
    }

}
