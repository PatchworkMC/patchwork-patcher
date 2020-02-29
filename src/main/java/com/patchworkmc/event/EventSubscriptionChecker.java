package com.patchworkmc.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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

	private Map<String, Entry> data = new HashMap<>();

	public EventSubscriptionChecker() {
	}

	public void onClassScanned(
			String className,
			List<SubscribeEvent> subscribeEvents,
			List<String> superClasses
	) {
		data.put(className, new Entry(subscribeEvents, superClasses));
	}

	public void check() {
		data.forEach((className, entry) -> {
			Set<String> descriptions = new HashSet<>();
			gatherSuperClasses(className).distinct().flatMap(
					c -> data.get(c).subscribeEvents.stream()
			).forEach(subscribeEvent -> {
				String description = subscribeEvent.getMethod()
						+ subscribeEvent.getEventClass()
						+ subscribeEvent.getGenericClass().orElse(null);
				if (descriptions.contains(description)) {
					throw new RuntimeException(
							String.format(
									"Currently Patchwork cannot handle @SubscribeEvent for overloaded methods. %s %s",
									className,
									subscribeEvent
							)
					);
				} else {
					descriptions.add(description);
				}
			});
		});
	}

	private Stream<String> gatherSuperClasses(String className) {
		return Optional.ofNullable(data.get(className))
				.map(entry -> Stream.concat(
						entry.superClasses.stream().flatMap(
								this::gatherSuperClasses
						),
						Stream.of(className)
				))
				.orElse(Stream.empty());
	}
}
