package com.pedestriamc.namecolor.listeners;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.pedestriamc.namecolor.NameColor;
import com.pedestriamc.namecolor.PacketUtilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatPacketAdjuster extends PacketAdapter
{
	private final NameColor nameColor;

	public ChatPacketAdjuster(NameColor nameColor)
	{
		super(nameColor, ListenerPriority.HIGHEST, PacketType.Play.Server.CHAT);
		this.nameColor = nameColor;
		nameColor.info("Chat hooked!");
	}

	@Override
	public void onPacketSending(PacketEvent event)
	{
		PacketContainer packet = event.getPacket();
		StructureModifier<Object> modifier = packet.getModifier();
		Object chatFormattingObject = modifier.readSafely(7);
		if(chatFormattingObject == null)
		{
			return;
		}

		InternalStructure internal = InternalStructure.getConverter().getSpecific(chatFormattingObject);
		StructureModifier<WrappedChatComponent> components = internal.getChatComponents();
		for(int i = 0; i < components.size(); i++)
		{
			WrappedChatComponent component = components.readSafely(i);
			if(component == null)
			{
				continue;
			}

			component = handleChatComponent(component);
			components.write(i, component);
		}
	}
	
	private WrappedChatComponent handleChatComponent(WrappedChatComponent chatComponent)
	{
		String json = chatComponent.getJson();
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
		return WrappedChatComponent.fromJson(json);
	}
}
