/*
 * Copyright (c) 2015 The Apocalypse MC. All rights reserved. Internal use only.
 */

package com.theapocalypsemc.staffchat;

import com.theapocalypsemc.staffchat.executors.HelpopExecutor;
import com.theapocalypsemc.staffchat.handlers.ChatHandler;
import io.github.sirfaizdat.bridge.BridgeSpigot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Main class for the StaffChat plugin.
 *
 * @author SirFaizdat
 */
public class StaffChat extends JavaPlugin {

    public static final String LOG_PREFIX = color("&7[&bStaff&3Chat&7]&r");
    private List<Channel> channels;
    private ChatManager chatManager;
    // This is a listener, but the send method is used in the HelpopExecutor class.
    public ChatHandler chatHandler;

    public void onEnable() {
        this.saveDefaultConfig();
        BridgeSpigot.getInstance().setServerNameIfNotSet(getConfig().getString("server-name"));

        reload(); // Reload in this case just means load.

        // Register with the server
        BridgeSpigot.getInstance().addListener(chatHandler);
        getServer().getPluginManager().registerEvents(chatHandler, this);

        // Register commands
        if (getConfig().getBoolean("enable-helpop"))
            getCommand("helpop").setExecutor(new HelpopExecutor(this));
        getCommand("screload").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                reloadConfig();
                reload();
                sender.sendMessage(ChatColor.GREEN + "Reloaded successfully.");
                return true;
            }
        });

        log("&7Successfully enabled &b" + getDescription().getFullName() + "&7. Crafted with <3 by &bSirFaizdat&7.");
    }

    private void reload() {
        // Initialize the needed stuff
        loadChannels();
        chatManager = new ChatManager();

        // Register listeners
        chatHandler = new ChatHandler(this);
    }

    /**
     * Grabs the channels section from the configuration and iterates through each
     * child of it. Then, it loads them all as channels.
     */
    private void loadChannels() {
        channels = new ArrayList<>();
        Set<String> configChannels = getConfig().getConfigurationSection("channels").getKeys(false);
        for (String channelName : configChannels) {
            // Additional data
            String permission = getConfig().getString("channels." + channelName + ".permission");
            List<String> commands = getConfig().getStringList("channels." + channelName + ".aliases");
            String color = getConfig().getString("channels." + channelName + ".color");
            // Construct and store the object
            Channel channel = new Channel(channelName, permission, commands, color);
            channels.add(channel);
        }
        if (getConfig().getBoolean("enable-helpop"))
            channels.add(new Channel("helpop", getConfig().getString("helpop-permission"), new ArrayList<String>(), getConfig().getString("helpop-color")));
    }

    // == Getters

    public Channel getChannel(String name) {
        for (Channel c : channels) if (c.getName().equalsIgnoreCase(name)) return c;
        return null;
    }

    /**
     * Get a channel by the command sent.
     *
     * @param command The command without the slash.
     * @return The channel's name, or null if the command didn't belong to any of them.
     */
    public String getChannelByCommand(String command) {
        for (Channel c : channels)
            for (String cmd : c.getCommands())
                if (cmd.equalsIgnoreCase(command)) return c.getName();
        return null;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    // == Logger methods

    public static void log(String message, Throwable t) {
        CommandSender consoleSender = Bukkit.getConsoleSender();
        String coloredMessage = LOG_PREFIX + color(" " + message);
        // Some consoles don't support colorful messages
        if (consoleSender == null) {
            Bukkit.getLogger().info(ChatColor.stripColor(coloredMessage));
            return;
        }
        consoleSender.sendMessage(coloredMessage);
        // Print the stack trace, if a throwable was provided
        if (t != null) {
            log("&cThe stack trace is as follows; please send this to the developer.");
            t.printStackTrace();
        }
    }

    public static void log(String message) {
        log(message, null);
    }

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }


}
