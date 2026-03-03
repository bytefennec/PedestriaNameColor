package com.pedestriamc.namecolor.listeners;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.pedestriamc.namecolor.NameColor;
import com.pedestriamc.namecolor.PacketUtilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class SystemChatPacketAdjuster extends PacketAdapter
{
	private final NameColor nameColor;

	public SystemChatPacketAdjuster(NameColor nameColor)
	{
		super(nameColor, ListenerPriority.HIGHEST, PacketType.Play.Server.SYSTEM_CHAT);
		this.nameColor = nameColor;
		nameColor.info("System chat hooked!");
	}

	@Override
	public void onPacketSending(PacketEvent event)
	{
		PacketContainer packet = event.getPacket();
		WrappedChatComponent chatComponent = packet.getChatComponents().readSafely(0);
		if(chatComponent == null)
		{
			return;
		}
			
		String json = chatComponent.getJson();
		if(json.contains("username is") || json.contains("No player found with nickname"))
		{
			//don't destroy whois command responses.
			return;
		}

		Component component = GsonComponentSerializer.gson().deserialize(json);
		for(Player player : nameColor.getServer().getOnlinePlayers())
		{
			String realName = player.getName();
			String fakeName = PlainTextComponentSerializer.plainText().serialize(player.playerListName());
			if(realName.equalsIgnoreCase(fakeName) || !json.contains(realName))
			{
				continue;
			}

			component = PacketUtilities.ComponentSwapNames(component, realName, fakeName);
		}

		json = GsonComponentSerializer.gson().serialize(component);
		packet.getChatComponents().write(0, WrappedChatComponent.fromJson(json));
	}

}
