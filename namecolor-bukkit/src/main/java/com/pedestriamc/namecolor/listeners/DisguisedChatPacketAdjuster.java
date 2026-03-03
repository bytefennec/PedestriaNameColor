package com.pedestriamc.namecolor.listeners;

import java.util.Optional;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.pedestriamc.namecolor.NameColor;
import com.pedestriamc.namecolor.PacketUtilities;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class DisguisedChatPacketAdjuster extends PacketAdapter
{
	private final NameColor nameColor;

	public DisguisedChatPacketAdjuster(NameColor nameColor)
	{
		super(nameColor, ListenerPriority.HIGHEST, PacketType.Play.Server.DISGUISED_CHAT);
		this.nameColor = nameColor;
		nameColor.info("Disguised chat hooked!");
	}

	@Override
	public void onPacketSending(PacketEvent event)
	{
		PacketContainer packet = event.getPacket();
		
		InternalStructure boundChat = packet.getStructures().readSafely(0);
		if(boundChat == null)
		{
			return;
		}
		
		StructureModifier<InternalStructure> structures = packet.getStructures();
		for (int i = 0; i < structures.size(); i++)
		{
			InternalStructure struct = structures.readSafely(i);
			if(struct == null)
			{
				continue;
			}

			StructureModifier<WrappedChatComponent> innerComponents = struct.getChatComponents();
			for (int j = 0; j < innerComponents.size(); j++)
			{
           		WrappedChatComponent component = innerComponents.readSafely(j);
            	if (component == null)
				{
					continue;
				}

				component = handleChatComponent(component);
				innerComponents.write(j, component);
			}

			StructureModifier<Optional<Object>> optionals = struct.getOptionals(Converters.passthrough(Object.class));
			for(int j = 0; j < optionals.size(); j++)
			{
				Optional<Object> optional = optionals.readSafely(j);
				if(optional == null || !optional.isPresent())
				{
					continue;
				}

				Object rawNms = optional.get();
				WrappedChatComponent component = WrappedChatComponent.fromHandle(rawNms);
				if(component == null)
				{
					continue;
				}

				component = handleChatComponent(component);
				optionals.write(j, Optional.of(component.getHandle()));
			}
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
