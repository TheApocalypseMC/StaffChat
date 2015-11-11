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
public class ChatHandler implements Listener, PluginMessageListener {

    StaffChat plugin;

    public ChatHandler(StaffChat plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) return; // Ignore non-Bungee messages

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        // Only read forwarded messages
        if (subchannel.equals("Forward")) {
            processIncomingMessage(in);
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
        // If the command contains no arguments, the player wants to toggle.
        if (indexToSplitAt == -1) {
            // Disable the current channel if there is one
            String playerCurrentChannel = plugin.getChatManager().getChannel(e.getPlayer().getName());
            if (playerCurrentChannel != null) {
                // Disable the current channel.
                plugin.getChatManager().disable(e.getPlayer().getName());

                // The player wants to toggle it off, so return
                if (playerCurrentChannel.equals(channel)) {
                    e.getPlayer().sendMessage(StaffChat.color("&cNo longer speaking in the the &4" + channel + " &cchannel."));
                    return;
                }
            }

            // Enable the new one
            plugin.getChatManager().enable(e.getPlayer().getName(), channel);
            // Success message
            e.getPlayer().sendMessage(StaffChat.color("&3Now speaking in the the &b" + channel + " &3channel."));
            return;
        }

        String message = command.substring(indexToSplitAt + 1);

        // Send the message
        send(channel, e.getPlayer().getName(), message);
    }

    /**
     * Process the data from the plugin message, extract the needed variables,
     * and send the message to the players who can see it.
     *
     * @param in The ByteArrayDataInput object provided by the listener.
     */
    private void processIncomingMessage(ByteArrayDataInput in) {
        // Store the header of the message, and then read the rest of it.
        String chatChannel = in.readUTF(); // The chat channel that this is going to
        short len = in.readShort();
        byte[] msgbytes = new byte[len];
        in.readFully(msgbytes);

        // Get the channel
        Channel channel = plugin.getChannel(chatChannel);
        if (channel == null) {
            StaffChat.log("&cError: Tried to receive message from unregistered channel " + chatChannel + ". Ignored...");
            return;
        }

        try {
            // Get the input
            DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
            String sender = msgin.readUTF();
            String message = msgin.readUTF();

            // Send it!
            formatAndBroadcast(channel, sender, message);

        } catch (IOException e) {
            e.printStackTrace();
        }
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
        // Write the header
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward"); // So BungeeCord knows to forward it
        out.writeUTF("ALL"); // All servers
        out.writeUTF(channel); // The channel this is being sent through

        // Get the channel
        Channel channelObj = plugin.getChannel(channel);
        if (channel == null) {
            StaffChat.log("&cError: Tried to send message to unregistered channel " + channel + ". Ignored...");
            return;
        }

        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();

        // Write the data
        try {
            DataOutputStream msgout = new DataOutputStream(msgbytes);
            msgout.writeUTF(sender);
            msgout.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Finalize
        out.writeShort(msgbytes.toByteArray().length);
        out.write(msgbytes.toByteArray());

        // Send the message to any player on the other server - it doesn't matter
        Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (player == null) return;
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());

        // Send to everyone on this server as well.
        formatAndBroadcast(channelObj, sender, message);
    }
}
