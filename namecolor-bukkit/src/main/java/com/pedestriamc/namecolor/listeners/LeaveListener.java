package com.pedestriamc.namecolor.listeners;

import com.pedestriamc.namecolor.NameColor;
import com.pedestriamc.namecolor.user.UserUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class LeaveListener implements Listener {

    private final UserUtil userUtil;

    public LeaveListener(@NotNull NameColor nameColor) {
        userUtil = nameColor.getUserUtil();
    }

    @EventHandler
    public void onPlayerLeave(@NotNull PlayerQuitEvent event) {
        event.quitMessage(null);
        String name = PlainTextComponentSerializer.plainText().serialize(event.getPlayer().playerListName());
        Component message = Component.text()
            .append(Component.translatable("multiplayer.player.left").arguments(Component.text(name)))
            .color(TextColor.fromHexString(NamedTextColor.YELLOW.asHexString()))
            .build();

        event.getPlayer().getServer().sendMessage(message);

        userUtil.removeUser(event.getPlayer().getUniqueId());
    }
}
