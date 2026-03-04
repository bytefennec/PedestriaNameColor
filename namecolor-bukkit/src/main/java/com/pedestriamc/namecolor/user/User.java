package com.pedestriamc.namecolor.user;

import com.pedestriamc.namecolor.NameUtilities;
import com.pedestriamc.namecolor.api.NameColorUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public final class User implements NameColorUser {

    private final NameUtilities nameUtilities;
    private final UUID uuid;
    private final Player player;
    private String displayName;
    private String originalName;

    public User(NameUtilities nameUtilities, UUID uuid) {
        this.nameUtilities = nameUtilities;
        this.uuid = uuid;
        player = Objects.requireNonNull(Bukkit.getPlayer(uuid));
        originalName = player.getName();
    }

    public User(NameUtilities nameUtilities, UUID uuid, String displayName) {
        this(nameUtilities, uuid);
        setDisplayName(displayName);
    }


    @NotNull
    public UUID getUniqueID() {
        return uuid;
    }

    @NotNull
    public String getDisplayName() {
        return displayName != null ? displayName : player.getDisplayName();
    }

    @NotNull
    public String getOriginalName() {
        return originalName;
    }

    public void setDisplayName(@NotNull String displayName) {
        this.displayName = displayName;
        nameUtilities.setDisplayName(displayName, player);
    }

    public Player getPlayer() {
        return player;
    }
}
