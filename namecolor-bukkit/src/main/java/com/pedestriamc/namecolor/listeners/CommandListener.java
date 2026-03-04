package com.pedestriamc.namecolor.listeners;

import java.util.regex.Pattern;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import com.pedestriamc.namecolor.NameColor;
import com.pedestriamc.namecolor.NameUtilities;
import com.pedestriamc.namecolor.PacketUtilities;
import com.pedestriamc.namecolor.user.User;
import com.pedestriamc.namecolor.user.UserUtil;

public class CommandListener implements Listener {
	private final UserUtil userUtil;

	public CommandListener(@NotNull NameColor nameColor)
	{
		this.userUtil = nameColor.getUserUtil();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEvent(@NotNull PlayerCommandPreprocessEvent commandEvent)
	{
		String message = commandEvent.getMessage();
		if(message.contains("whois"))
		{
			//who is as is.
			return;
		}

		for(User user : userUtil.getUsers())
		{
			String fakeName = NameUtilities.stripColor(user.getDisplayName());
			if(!message.contains(fakeName))
			{
				continue;
			}

			message = PacketUtilities.getNamePatternFakeToReal(fakeName).matcher(message).replaceAll(user.getOriginalName());
		}

		commandEvent.setMessage(message);
	}
}
