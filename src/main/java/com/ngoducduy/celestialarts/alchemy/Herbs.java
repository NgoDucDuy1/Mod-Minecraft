package com.ngoducduy.celestialarts.alchemy;

import com.ngoducduy.celestialarts.skill.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lookup over the herb table. */
public final class Herbs {
	private static final Map<String, Herb> BY_KEY = new HashMap<>();
	private static final Map<Herb.Habitat, List<Herb>> BY_HABITAT = new HashMap<>();

	static {
		for (Herb herb : HerbList.ALL) {
			if (BY_KEY.put(herb.key(), herb) != null) {
				throw new IllegalStateException("Duplicate herb key " + herb.key());
			}
			BY_HABITAT.computeIfAbsent(herb.habitat(), h -> new ArrayList<>()).add(herb);
		}
	}

	private Herbs() {
	}

	public static List<Herb> all() {
		return HerbList.ALL;
	}

	public static Herb byKey(String key) {
		return BY_KEY.get(key);
	}

	public static List<Herb> inHabitat(Herb.Habitat habitat) {
		return Collections.unmodifiableList(BY_HABITAT.getOrDefault(habitat, List.of()));
	}

	public static List<Herb> ofElement(Element element) {
		List<Herb> out = new ArrayList<>();
		for (Herb herb : HerbList.ALL) {
			if (herb.element() == element) out.add(herb);
		}
		return out;
	}
}
