package com.pedestriamc.namecolor.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.pedestriamc.namecolor.NameColor;

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
		//stop "Chat validation error" nags.
		byte[] emptySignature = new byte[0];
		packet.getByteArrays().writeSafely(0, emptySignature);
		//also force unsigned status
		if(packet.getBooleans().size() > 0)
		{
			packet.getBooleans().writeSafely(0, false);
		}
	}
}
