package com.pedestriamc.namecolor.commands;

import com.pedestriamc.common.message.Messenger;
import com.pedestriamc.namecolor.Message;
import com.pedestriamc.namecolor.NameColor;
import com.pedestriamc.namecolor.manager.BlacklistManager;
import com.pedestriamc.namecolor.user.User;
import com.pedestriamc.namecolor.user.UserUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import static com.pedestriamc.namecolor.NameUtilities.stripColor;

import java.util.Map;

public class NicknameCommand implements CommandExecutor {

    private final Messenger<Message> messenger;
    private final BlacklistManager blacklistManager;
    private final UserUtil userUtil;

    // Should players be notified when their nickname is changed?
    private final boolean notifyChange;

    // Should players be allowed to have nicknames that are the same as usernames of other players?
    private final boolean allowUserNick;

    // The max integer nick length, excluding colors.
    private final int maxLength;

    public NicknameCommand(@NotNull NameColor nameColor) {
        messenger = nameColor.getMessenger();
        blacklistManager = nameColor.blacklistManager();
        userUtil = nameColor.getUserUtil();

        FileConfiguration config = nameColor.getConfig();
        notifyChange = config.getBoolean("notify-players", true);

        int nickLength = config.getInt("max-nickname-length");
        maxLength = nickLength <= 0 ? 16 : nickLength;

        allowUserNick = config.getBoolean("allow-username-nicknames");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(doesNotHavePermission(sender)) {
            messenger.sendMessage(sender, Message.NO_PERMS);
            return true;
        }

        switch(args.length) {
            case 0 -> messenger.sendMessage(sender, Message.INSUFFICIENT_ARGS);

            case 1 -> args1(sender, args);

            case 2 -> args2(sender, args);

            default -> messenger.sendMessage(sender, Message.INVALID_ARGS_NICK);
        }

        return true;
    }

    private void args1(@NotNull CommandSender sender, @NotNull String[] args) {
        String arg = args[0];
        Player target;
        if(arg.equalsIgnoreCase("help")) {
            messenger.sendMessage(sender, Message.NICKNAME_HELP);
            return;
        }

        if(!(sender instanceof Player player)) {
            messenger.sendMessage(sender, Message.CONSOLE_MUST_DEFINE_PLAYER);
            return;
        } else {
            target = player;
        }

        if(arg.equalsIgnoreCase("reset")) {
            User user = userUtil.getUser(player.getUniqueId());
            updateTarget(sender, target, user.getOriginalName());
            return;
        }

        if(satisfiesConditions(sender, target, arg)) {
            if(shouldUpdateStripped(sender, arg)) {
                updateTargetStripped(sender, target, arg);
            } else {
                updateTarget(sender, target, arg);
            }
        }
    }

    private void args2(@NotNull CommandSender sender, @NotNull String[] args) {
        if(!sender.hasPermission("namecolor.nick.others")) {
            messenger.sendMessage(sender, Message.NO_PERMS_OTHER);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if(target == null) {
            messenger.sendMessage(sender, Message.INVALID_PLAYER);
            return;
        }

        if(satisfiesConditions(sender, target, args[0])) {
            if(shouldUpdateStripped(sender, args[0])) {
                updateTargetStripped(sender, target, args[0]);
            } else {
                updateTarget(sender, target, args[0]);
            }
        }
    }

    /**
     * Determines if a nick meets requirements (if enabled in config),
     * and sends a message to the sender if requirements aren't met.
     * Checks if the name is blacklisted, a duplicate, or if it's too long.
     * @param sender The sender of the command
     * @param nick The nick to check
     * @return True if requirements are met, false otherwise
     */
    private boolean satisfiesConditions(CommandSender sender, Player target, String nick) {
        nick = stripColor(nick);

        if(
                sender.hasPermission("namecolor.filter.bypass") ||
                sender.hasPermission("namecolor.*") ||
                sender.hasPermission("*") ||
                sender.isOp()
        ) {
            return true;
        }

        if(nick.length() > maxLength) {
            messenger.sendMessage(sender, Message.NICK_TOO_LONG);
            return false;
        }

        User user = userUtil.getUser(target.getUniqueId());
        if(!allowUserNick && !nick.equalsIgnoreCase(user.getOriginalName()) && doesMatchOtherUsername(nick)) {
            messenger.sendMessage(sender, Message.USERNAME_NICK_PROHIBITED);
            return false;
        }

        if(blacklistManager.isBlacklisted(nick)) {
            messenger.sendMessage(sender, Message.NICK_BLACKLIST);
            return false;
        }

        return true;
    }

    private boolean doesMatchOtherUsername(@NotNull String nick) {
        for(OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            User user = userUtil.getUser(player.getUniqueId());
            String name = user.getOriginalName();
            if(name != null && name.equalsIgnoreCase(nick)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets and saves the target's nickname, and sends messages to the sender and/or target.
     * @param sender The sender of the command
     * @param target The target of the command
     * @param nick The new nickname
     */
    private void updateTarget(@NotNull CommandSender sender, @NotNull Player target, @NotNull String nick) {
        User user = userUtil.getUser(target.getUniqueId());
        Bukkit.getLogger().info(user.getOriginalName() + " changed nickname to " + nick);
        user.setDisplayName(nick);
        userUtil.saveUser(user);

        if(!sender.equals(target)) {
            messenger.sendMessage(sender, Message.NAME_SET_OTHER, getPlaceholders(target, user));
        }

        if(notifyChange) {
            messenger.sendMessage(target, Message.NAME_SET, getPlaceholders(target, user));
        }
    }

    /**
     * Sets and saves the target's nickname, with color stripped, and sends messages to the sender and/or target.
     * @param sender The sender of the command
     * @param target The target of the command
     * @param nick The new nickname
     */
    private void updateTargetStripped(@NotNull CommandSender sender, @NotNull Player target, @NotNull String nick) {
        nick = stripColor(nick);
        boolean modifyingOther = !sender.equals(target);

        User user = userUtil.getUser(target.getUniqueId());
        Bukkit.getLogger().info(user.getOriginalName() + " changed nickname to " + nick);
        user.setDisplayName(nick);
        userUtil.saveUser(user);

        if(modifyingOther) {
            messenger.sendMessage(sender, Message.NO_NICK_COLOR_OTHER, getPlaceholders(target, user));
        }

        if(notifyChange) {
            if(modifyingOther) {
                messenger.sendMessage(target, Message.NAME_SET, getPlaceholders(target, user));
            } else {
                messenger.sendMessage(target, Message.NO_NICK_COLOR, getPlaceholders(target, user));
            }
        }
    }

    private boolean shouldUpdateStripped(CommandSender sender, String nick) {
        if(
                !sender.isOp() &&
                !sender.hasPermission("*") &&
                !sender.hasPermission("namecolor.*") &&
                !sender.hasPermission("namecolor.nick.*") &&
                !sender.hasPermission("namecolor.nick.color")

        ) {
            return !nick.equals(stripColor(nick));
        }
        return false;
    }

    private boolean doesNotHavePermission(@NotNull CommandSender sender) {
        return !(
                sender.isOp() ||
                sender.hasPermission("*") ||
                sender.hasPermission("namecolor.nick") ||
                sender.hasPermission("namecolor.*") ||
                sender.hasPermission("namecolor.nick.*")
                );
    }

    @Contract("_ -> new")
    private @NotNull @Unmodifiable Map<String, String> getPlaceholders(@NotNull Player player, @NotNull User user) {
        return Map.of(
                "%display-name%", player.getDisplayName(),
                "%username%", user.getOriginalName()
        );
    }
}
