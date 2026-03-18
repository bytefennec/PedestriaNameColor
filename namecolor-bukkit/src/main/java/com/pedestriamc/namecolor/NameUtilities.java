package com.pedestriamc.namecolor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.earth2me.essentials.Essentials;
import com.pedestriamc.namecolor.api.Mode;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NameUtilities {

    public static final Pattern HEX = Pattern.compile("#[a-fA-F0-9]{6}", Pattern.CASE_INSENSITIVE);

    public static final Pattern SPIGOT_HEX = Pattern.compile("&#[a-fA-F0-9]{6}", Pattern.CASE_INSENSITIVE);
    public static final Pattern MC_HEX = Pattern.compile("§#[a-fA-F0-9]{6}", Pattern.CASE_INSENSITIVE);

    private final NameColor nameColor;
    private boolean usingEssentials;
    private Essentials essentials;
    private final String nickPrefix;

    public NameUtilities(@NotNull NameColor nameColor) {
        this.nameColor = nameColor;
        determineMode();
        nickPrefix = nameColor.getConfig().getString("nick-prefix", "");
    }

    /**
     * Determines if the plugin is using Essentials
     */
    private void determineMode() {
        if(nameColor.getMode() == Mode.ESSENTIALS) {
            try {
                essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
            } catch(ClassCastException e) {
                nameColor.warn("An error occurred while finding Essentials: " + e.getMessage());
            }
            usingEssentials = essentials != null;
        }
    }

    /**
     * Legacy setColor method. Redirects to setDisplayName
     * @param player The player to set the name color of.
     * @param color The color to set it to.
     */
    @SuppressWarnings("unused")
    public void setColor(Player player, ChatColor color) {
        setDisplayName(color + NameUtilities.stripColor(player.getName()), player);
    }

    /**
     * Legacy setColor method.  Now redirects to the setNick method.
     * @param player The player to set the name color of.
     * @param color The color to set it to.
     * @param save If the color should be saved.
     */
    @SuppressWarnings("unused")
    public void setColor(Player player, String color, boolean save) {
        if(color.charAt(0) == '#') {
            setDisplayName(ChatColor.of(color) + NameUtilities.stripColor(player.getName()), player);
        } else {
            setDisplayName(ChatColor.translateAlternateColorCodes('&', color) + NameUtilities.stripColor(player.getName()), player);
        }
    }

    /**
     * Primary method of changing display names in NameColor.
     * <a href="https://stackoverflow.com/questions/237061/using-regular-expressions-to-extract-a-value-in-java">...</a>
     * <a href="https://stackoverflow.com/questions/15130309/how-to-use-regex-in-string-contains-method-in-java">...</a>
     * @param displayName The new display name of the player.
     * @param player The player to set the display name of.
     */
    public void setDisplayName(String displayName, Player player) {
        Matcher matcher = SPIGOT_HEX.matcher(displayName);
        while(matcher.find()) {
            String hexColor = matcher.group().substring(1).toUpperCase();
            ChatColor color = ChatColor.of(new Color(Integer.parseInt(hexColor.substring(1), 16)));
            displayName = displayName.replace(matcher.group(), color.toString());
        }

        // Add nick prefix if applicable
        if(!stripColor(displayName).equalsIgnoreCase(player.getName())) {
            displayName = nickPrefix + displayName;
        }
        
        displayName += "&r";
        displayName = ChatColor.translateAlternateColorCodes('&', displayName);
        String strippedName = stripColor(displayName);
        player.setPlayerListName(strippedName);
        player.setDisplayName(displayName);

        if(usingEssentials) {
            essentials.getUser(player.getUniqueId()).setNickname(displayName);
        }

        Bukkit.getScheduler().runTaskLater(nameColor, () -> {
            PlayerProfile originalProfile = player.getPlayerProfile();
            PlayerProfile fakeProfile = Bukkit.createProfileExact(originalProfile.getId(), strippedName);
            fakeProfile.setProperties(originalProfile.getProperties());
            player.setPlayerProfile(fakeProfile);
            for(Player otherPlayer : Bukkit.getOnlinePlayers())
            {
                otherPlayer.hidePlayer(nameColor, player);
            }

            Bukkit.getScheduler().runTaskLater(nameColor, () -> {
                for(Player otherPlayer : Bukkit.getOnlinePlayers())
                {
                    otherPlayer.showPlayer(nameColor, player);
                }
            }, 1);
        },2);
    }

    public static String stripColor(String str) {
        String stripped = SPIGOT_HEX.matcher(str).replaceAll("");
        stripped = MC_HEX.matcher(stripped).replaceAll(""); //don't fuck up tab board with hex codes.
        stripped = org.bukkit.ChatColor.translateAlternateColorCodes('&', stripped);
        stripped = org.bukkit.ChatColor.stripColor(stripped);
        return stripped;
    }
}
