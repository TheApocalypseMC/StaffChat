/*
 * Copyright (c) 2015 The Apocalypse MC. All rights reserved. Internal use only.
 */

package com.theapocalypsemc.staffchat.handlers;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.theapocalypsemc.staffchat.Channel;
import com.theapocalypsemc.staffchat.StaffChat;
import io.github.sirfaizdat.bridge.MessageListener;
import io.github.sirfaizdat.bridge.common.MessagePacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;

/**
 * Listens to player chat events, intercepts them, and turns them into StaffChat
 * messages.
 *
 * @author SirFaizdat
 */
public class ChatHandler extends MessageListener implements Listener {

    private StaffChat plugin;

    public ChatHandler(StaffChat plugin) {
        this.plugin = plugin;
    }

    @Override public void onMessageReceived(MessagePacket messagePacket) {
        if(messagePacket.commandName.equalsIgnoreCase("staffchat")) {
            // This is our packet
            processIncomingMessage(messagePacket);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        // Check if this user is actually in a channel before proceeding.
        if (plugin.getChatManager().getChannel(e.getPlayer().getName()) == null) return;

        e.setCancelled(true); // Stop the message from sending normally.
        // Process the data and send it inter-server
        String channel = plugin.getChatManager().getChannel(e.getPlayer().getName());
        send(channel, e.getPlayer().getName(), e.getMessage());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreCommand(PlayerCommandPreprocessEvent e) {
        // Remove the trailing slash from the message.
        String command = e.getMessage().replaceFirst("/", "");
        // Get the channel command.
        String channel = plugin.getChannelByCommand(command.split(" ")[0]);
        if (channel == null) return; // Not a channel command? Not concerned.

        e.setCancelled(true); // Cancel the message from sending as a command.

        Channel channelObj = plugin.getChannel(channel);
        if(!e.getPlayer().hasPermission(channelObj.getPermission()) && !e.getPlayer().isOp()) {
            e.getPlayer().sendMessage(StaffChat.color("&c&lAw snap! &7You don't have permission to do that. &8You need the permission " + channelObj.getPermission() + "."));
            return;
        }

        // Get the message from the command by creating a substring.
        int indexToSplitAt = command.indexOf(" "); // Get the location of the first space
        // If the command contains no arguments, tell the player.
        if (indexToSplitAt == -1) {
            e.getPlayer().sendMessage(StaffChat.color("&cYou must supply a message, or [off|on] to toggle."));
            return;
        }

        String message = command.substring(indexToSplitAt + 1);
        String playerCurrentChannel = plugin.getChatManager().getChannel(e.getPlayer().getName());

        if(message.toLowerCase().equals("off")) {
            plugin.getChatManager().disable(e.getPlayer().getName());
            e.getPlayer().sendMessage(StaffChat.color("&cNo longer speaking in the the &4" + channel + " &cchannel."));
            return;
        } else if(message.toLowerCase().equals("on")) {
            if(playerCurrentChannel != null) {
                plugin.getChatManager().disable(e.getPlayer().getName());
            }

            // Enable the new one
            plugin.getChatManager().enable(e.getPlayer().getName(), channel);
            // Success message
            e.getPlayer().sendMessage(StaffChat.color("&3Now speaking in the the &b" + channel + " &3channel."));
            return;
        }

        // Send the message
        send(channel, e.getPlayer().getName(), message);
    }

    /**
     * Process the data from the plugin message, extract the needed variables,
     * and send the message to the players who can see it.
     *
     * @param packet The {@link MessagePacket} object provided by the listener.
     */
    private void processIncomingMessage(MessagePacket packet) {
        // Get the channel
        String chatChannel  = packet.args[0];
        Channel channel = plugin.getChannel(chatChannel);
        if (channel == null) {
            StaffChat.log("&cError: Tried to receive message from unregistered channel " + chatChannel + ". Ignored...");
            return;
        }

        // And the data
        String sender = packet.args[1];
        String message = packet.args[2];

        // Send it!
        formatAndBroadcast(channel, sender, message);

    }

    private void formatAndBroadcast(Channel channel, String sender, String message) {
        // Format the message
        String format = plugin.getConfig().getString("format");
        format = format.replaceAll("%color", channel.getColor()).
                replaceAll("%channel", channel.getName()).
                replaceAll("%sender", sender).
                replaceAll("%message", message);
        format = StaffChat.color(format);

        // Send the message to players who can see it.
        for (Player p : plugin.getServer().getOnlinePlayers())
            if (p.hasPermission(channel.getPermission()) || p.isOp()) p.sendMessage(format);
    }

    /**
     * Send a message through a channel.
     *
     * @param channel The chat channel to send this through.
     * @param sender  The name of the sender of the message.
     * @param message The message contents. Leave all color codes unformatted.
     */
    public void send(String channel, String sender, String message) {
        MessagePacket packet = new MessagePacket(
            "staffchat",
            "all",
            new String[]{
                channel,
                sender,
                message
            }
        );

        send(packet);
    }
}
