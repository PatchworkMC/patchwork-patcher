package com.patchworkmc.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;

// Currently patchwork cannot handle @SubscribeEvent for overloaded methods
public class EventSubscriptionChecker {
	private static class Entry {
		public List<SubscribeEvent> subscribeEvents;
		public List<String> superClasses;

		Entry(List<SubscribeEvent> subscribeEvents, List<String> superClasses) {
			this.subscribeEvents = subscribeEvents;
			this.superClasses = superClasses;
		}
	}

	private Map<String, Entry> entries = new HashMap<>();
	private static final List<String> missingClassWhiteList = Arrays.asList(
			"java/",
			"net/minecraft/",
			"net/minecraftforge/"
	);

	public EventSubscriptionChecker() {
	}

	public void onClassScanned(
			String className,
			List<SubscribeEvent> subscribeEvents,
			List<String> superClasses
	) {
		entries.put(className, new Entry(subscribeEvents, superClasses));
	}

	public void check() {
		entries.forEach((className, entry) -> {
			if (entry.subscribeEvents.isEmpty()) {
				return;
			}

			List<SubscribeEvent> subscribeEvents = gatherSubscriptions(className, "");

			Set<String> descriptions = new HashSet<>();

			for (SubscribeEvent subscribeEvent : subscribeEvents) {
				if ((subscribeEvent.getAccess() & Opcodes.ACC_STATIC) != 0) {
					String description = getDescription(subscribeEvent);

					if (descriptions.contains(description)) {
						throw new RuntimeException(
								String.format(
										"Currently Patchwork cannot handle @SubscribeEvent for overloaded methods, but the class %s has overloaded methods annotated with %s",
										className,
										subscribeEvent
								)
						);
					}

					descriptions.add(description);
				}
			}
		});
	}

	private List<SubscribeEvent> gatherSubscriptions(String currentClass, String subClass) {
		Entry entry = entries.get(currentClass);

		if (entry == null) {
			if (!shouldTolerateMissingClass(currentClass, entry)) {
				throw new RuntimeException(
						"Missing information for class " + currentClass
								+ (subClass.isEmpty() ? "" : (" which is the super class of " + subClass))
				);
			}

			return new ArrayList<>();
		}

		ArrayList<SubscribeEvent> result = new ArrayList<>(entry.subscribeEvents);

		for (String superClass : entry.superClasses) {
			result.addAll(gatherSubscriptions(superClass, currentClass));
		}

		return result;
	}

	// Subscribing the same event should have the same description
	private String getDescription(SubscribeEvent subscribeEvent) {
		return subscribeEvent.getMethod()
				+ subscribeEvent.getEventClass();
	}

	private boolean shouldTolerateMissingClass(String className, Entry entry) {
		return missingClassWhiteList.stream()
				.anyMatch(className::startsWith);
	}
}
