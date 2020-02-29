package com.patchworkmc.event;

import com.patchworkmc.Patchwork;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
			"net/minecraft",
			"net/minecraftforge"
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
				String description = getDescription(subscribeEvent);
				if (descriptions.contains(description)) {
					throw new RuntimeException(
							String.format(
									"Currently Patchwork cannot handle @SubscribeEvent for overloaded methods. %s %s",
									className,
									subscribeEvent
							)
					);
				}
				descriptions.add(description);
			}
		});
	}

	private List<SubscribeEvent> gatherSubscriptions(String currentClass, String subClass) {
		Entry entry = entries.get(currentClass);
		if (entry == null) {
			if (!shouldTolerateMissingClass(currentClass, entry)) {
				throw new RuntimeException(String.format(
						"Missing information for class %s which is the superclass of %s",
						currentClass, subClass
				));
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
				+ subscribeEvent.getEventClass()
				+ subscribeEvent.getGenericClass().orElse("");
	}

	private boolean shouldTolerateMissingClass(String className, Entry entry) {
		return missingClassWhiteList.stream()
				.anyMatch(className::startsWith);
	}

}
