package com.pedestriamc.namecolor.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.pedestriamc.namecolor.NameColor;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class GameProfileAdjuster extends PacketAdapter
{
	private final NameColor nameColor;

	public GameProfileAdjuster(NameColor nameColor)
	{
		super(nameColor, ListenerPriority.HIGHEST, PacketType.Play.Server.PLAYER_INFO);
		this.nameColor = nameColor;
		nameColor.info("Game profiles hooked!");
	}

	@Override
	public void onPacketSending(PacketEvent event)
	{
		PacketContainer packet = event.getPacket();
		
		Set<PlayerInfoAction> actions = packet.getPlayerInfoActions().readSafely(0);
		if(actions == null || !actions.contains(EnumWrappers.PlayerInfoAction.ADD_PLAYER))
		{
			return;
		}

		List<PlayerInfoData> dataList = packet.getPlayerInfoDataLists().readSafely(1);
		if(dataList == null)
		{
			nameColor.warn("PLAYER_INFO packet was ADD_PLAYER but had no PlayerInfoData?");
			return;
		}

		HashMap<UUID, String> IDToNameTable = new HashMap<UUID, String>();
		for(Player player : nameColor.getServer().getOnlinePlayers())
		{
			//fake name is same as real name on people who haven't changed their nick.
			String fakeName = PlainTextComponentSerializer.plainText().serialize(player.playerListName());
			IDToNameTable.put(player.getUniqueId(), fakeName);
		}

		List<PlayerInfoData> newDataList = new ArrayList<>();
		for(PlayerInfoData data : dataList)
		{
			if(data == null)
			{
				nameColor.warn("PLAYER_INFO packet was ADD_PLAYER and had PlayerInfoData, but data was null?");
				continue;
			}

			if(!IDToNameTable.containsKey(data.getProfileId()))
			{
				nameColor.warn("PLAYER_INFO packet had incomplete PlayerInfoData?");
				newDataList.add(data);
				continue;
			}

			WrappedGameProfile fakeProfile = new WrappedGameProfile(
				data.getProfileId(),
				IDToNameTable.get(data.getProfileId())
			);

			fakeProfile.getProperties().putAll(data.getProfile().getProperties());
			PlayerInfoData fakeData = new PlayerInfoData(
				fakeProfile,
				data.getLatency(),
				data.getGameMode(),
				data.getDisplayName()
			);

			newDataList.add(fakeData);
		}

		packet.getPlayerInfoDataLists().write(1, newDataList);
	}
}
