package com.pedestriamc.namecolor.listeners;

import com.pedestriamc.namecolor.NameColor;
import com.pedestriamc.namecolor.user.UserUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class JoinListener implements Listener {

    private final UserUtil userUtil;

    public JoinListener(@NotNull NameColor nameColor) {
        userUtil = nameColor.getUserUtil();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEvent(@NotNull PlayerJoinEvent event) {
        event.joinMessage(null);
        userUtil.loadUserAsync(event.getPlayer().getUniqueId()).thenRun(() -> {
            String name = PlainTextComponentSerializer.plainText().serialize(event.getPlayer().playerListName());
            Component message = Component.text()
                .append(Component.translatable("multiplayer.player.joined").arguments(Component.text(name)))
                .color(TextColor.fromHexString(NamedTextColor.YELLOW.asHexString()))
                .build();

            event.getPlayer().getServer().sendMessage(message);
        });
    }
}
