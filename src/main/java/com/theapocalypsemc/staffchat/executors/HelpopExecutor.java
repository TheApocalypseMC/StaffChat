/*
 * Copyright (c) 2015 The Apocalypse MC. All rights reserved. Internal use only.
 */

package com.theapocalypsemc.staffchat.executors;

import com.theapocalypsemc.staffchat.StaffChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * The executor for the /helpop command.
 *
 * @author SirFaizdat
 */
public class HelpopExecutor implements CommandExecutor {

    StaffChat plugin;

    public HelpopExecutor(StaffChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(StaffChat.color("&c&lAw snap! &7Please provide a message."));
            return true;
        }
        plugin.chatHandler.send("helpop", sender.getName(), implode(" ", args));
        sender.sendMessage(StaffChat.color("&bSuccess! &7Your message was sent, and a staff member will respond in a moment."));
        return true;
    }

    // A little implode method, thanks to Pshemo
    private String implode(String separator, String... data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length - 1; i++) {
            //data.length - 1 => to not add separator at the end
            if (!data[i].matches(" *")) {//empty string are ""; " "; "  "; and so on
                sb.append(data[i]);
                sb.append(separator);
            }
        }
        sb.append(data[data.length - 1].trim());
        return sb.toString();
    }

}
