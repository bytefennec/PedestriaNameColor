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
        setDisplayName(color + player.getName(), player);
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
            setDisplayName(ChatColor.of(color) + player.getName(), player);
        } else {
            setDisplayName(ChatColor.translateAlternateColorCodes('&', color) + player.getName(), player);
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
        player.setPlayerListName(stripColor(displayName));
        player.setDisplayName(displayName);
        if(usingEssentials) {
            essentials.getUser(player.getUniqueId()).setNickname(displayName);
        }

        ForceRefreshPlayer(player, stripColor(displayName));
    }

    public static String stripColor(String str) {
        String stripped = SPIGOT_HEX.matcher(str).replaceAll("");
        stripped = MC_HEX.matcher(stripped).replaceAll(""); //don't fuck up tab board with hex codes.
        stripped = org.bukkit.ChatColor.translateAlternateColorCodes('&', stripped);
        stripped = org.bukkit.ChatColor.stripColor(stripped);
        return stripped;
    }

    public void ForceRefreshPlayer(Player player, String name)
    {
        ProtocolManager manager = nameColor.protocolManager;

        PacketContainer removeInfo = manager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        removeInfo.getUUIDLists().write(0, Collections.singletonList(player.getUniqueId()));

        WrappedGameProfile fakeProfile = new WrappedGameProfile(player.getUniqueId(), name);
        WrappedGameProfile realProfile = WrappedGameProfile.fromPlayer(player);
        fakeProfile.getProperties().putAll(realProfile.getProperties());

        PacketContainer addInfo = manager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        addInfo.getPlayerInfoActions().write(0, EnumSet.of(
            EnumWrappers.PlayerInfoAction.ADD_PLAYER,
            EnumWrappers.PlayerInfoAction.UPDATE_LISTED,
            EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE,
            EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME
        ));
        addInfo.getPlayerInfoDataLists().write(1, Collections.singletonList(new PlayerInfoData(
            fakeProfile,
            player.getPing(),
            EnumWrappers.NativeGameMode.fromBukkit(player.getGameMode()),
            WrappedChatComponent.fromText(name)
        )));

        //also have to spawn new entity in too.
        PacketContainer removeEntity = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        removeEntity.getIntLists().write(0, Collections.singletonList(player.getEntityId()));

        PacketContainer addEntity = manager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        addEntity.getIntegers().write(0, player.getEntityId());
        addEntity.getUUIDs().write(0, player.getUniqueId());
        addEntity.getEntityTypeModifier().write(0, EntityType.PLAYER);
        addEntity.getDoubles().write(0, player.getLocation().getX())
            .write(1, player.getLocation().getY())
            .write(2, player.getLocation().getZ());
        addEntity.getBytes().write(0, (byte)(player.getPitch() * 256.0f / 360.0f))
            .write(1, (byte)(player.getYaw() * 256.0f / 360.0f))
            .write(2, (byte)(player.getYaw() * 256.0f / 360.0f));

        PacketContainer entityMetadata = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        entityMetadata.getIntegers().write(0, player.getEntityId());
        WrappedDataWatcher metadataWatcher = WrappedDataWatcher.getEntityWatcher(player);
        //swap type for 1.21.11 packet
        List<WrappedDataValue> wrappedDataValues = new ArrayList<>();
        for(WrappedWatchableObject watchableObject : metadataWatcher.getWatchableObjects())
        {
            if(watchableObject == null)
            {
                continue;
            }

            WrappedDataWatcher.WrappedDataWatcherObject watcherObject = watchableObject.getWatcherObject();
            wrappedDataValues.add(new WrappedDataValue(
                watcherObject.getIndex(),
                watcherObject.getSerializer(),
                watchableObject.getRawValue()
            ));
        }

        entityMetadata.getDataValueCollectionModifier().write(0, wrappedDataValues);

        Bukkit.getScheduler().runTaskLater(nameColor, new Runnable() {
            @Override
            public void run()
            {
                for(Player otherPlayer : Bukkit.getOnlinePlayers())
                {
                    if(otherPlayer.getUniqueId() != player.getUniqueId())
                    {
                        //MC Client starts behaving really funkily if you send a new local player entity to it,
                        //as the client manages its own entity. 
                        manager.sendServerPacket(otherPlayer, removeEntity);
                    }

                    manager.sendServerPacket(otherPlayer, removeInfo);
                }
            }
        }, 0);

        Bukkit.getScheduler().runTaskLater(nameColor, new Runnable() {
            @Override
            public void run()
            {
                for(Player otherPlayer : Bukkit.getOnlinePlayers())
                {
                    manager.sendServerPacket(otherPlayer, addInfo);

                    if(otherPlayer.getUniqueId() != player.getUniqueId())
                    {
                        //MC Client starts behaving really funkily if you send a new local player entity to it,
                        //as the client manages its own entity. 
                        manager.sendServerPacket(otherPlayer, addEntity);
                        manager.sendServerPacket(otherPlayer, entityMetadata);
                    }
                }
            }
        }, 1);
    }
}
