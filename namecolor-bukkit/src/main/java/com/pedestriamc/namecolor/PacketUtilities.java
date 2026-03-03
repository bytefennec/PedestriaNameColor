package com.pedestriamc.namecolor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class PacketUtilities
{
	private final static HashMap<String, Pattern> realToFakeTable = new HashMap<>();
	private final static HashMap<String, Pattern> fakeToRealTable = new HashMap<>();

	public static Pattern getNamePatternRealToFake(String realName)
	{
		return realToFakeTable.computeIfAbsent(realName, name -> 
			Pattern.compile("(?i)\\b" + Pattern.quote(name) + "\\b")
		);
	}

	public static Pattern getNamePatternFakeToReal(String fakeName)
	{
		return fakeToRealTable.computeIfAbsent(fakeName, name -> 
			Pattern.compile("(?i)\\b" + Pattern.quote(name) + "\\b")
		);
	}

	public static Component ComponentSwapNames(Component component, String realName, String fakeName)
	{
		component = component.replaceText(TextReplacementConfig.builder()
				.match(getNamePatternRealToFake(realName))
				.replacement(fakeName)
				.build());

		return ComponentSwapNamesInEvents(component, realName, fakeName);
	}

	private static Component ComponentSwapNamesInEvents(Component component, String realName, String fakeName)
	{
		ClickEvent clickEvent = component.clickEvent();
		if(clickEvent != null)
		{
			if(clickEvent.action() == ClickEvent.Action.RUN_COMMAND
				|| clickEvent.action() == ClickEvent.Action.SUGGEST_COMMAND)
			{
				//deprecated function, but commands are strings anyways so ¯\_(ツ)_/¯
				String newCommand = getNamePatternRealToFake(realName).matcher(clickEvent.value()).replaceAll(fakeName);
				component = component.clickEvent(ClickEvent.clickEvent(clickEvent.action(), newCommand));
			}
		}

		HoverEvent<?> hoverEvent = component.hoverEvent();
		if(hoverEvent != null)
		{
			if(hoverEvent.action() == HoverEvent.Action.SHOW_TEXT)
			{
				Component newHoverBody = ((Component)hoverEvent.value()).replaceText(TextReplacementConfig.builder()
					.match(getNamePatternRealToFake(realName))
					.replacement(fakeName)
					.build());
				
				component = component.hoverEvent(HoverEvent.showText(newHoverBody));
			}
		}

		if(component instanceof TranslatableComponent translatable)
		{
			List<TranslationArgument> args = translatable.arguments();
			List<TranslationArgument> newArgs = new ArrayList<>();
			for(TranslationArgument arg : args)
			{
				if(arg.value() instanceof Component argComponent)
				{
					Component processedArg = ComponentSwapNames(argComponent, realName, fakeName);
					newArgs.add(TranslationArgument.component(processedArg));
					continue;
				}

				//entities and numerics.
				newArgs.add(arg);
			}

			component = translatable.arguments(newArgs);
		}

		List<Component> children = component.children();
		if(!children.isEmpty())
		{
			List<Component> newChildren = new ArrayList<>();
			for(Component child : children)
			{
				newChildren.add(ComponentSwapNames(child, realName, fakeName));
			}

			component = component.children(newChildren);
		}

		return component;
	}
}
