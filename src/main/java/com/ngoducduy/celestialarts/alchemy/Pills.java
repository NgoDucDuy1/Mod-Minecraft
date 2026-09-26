package com.ngoducduy.celestialarts.alchemy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Pills {
	private static final Map<String, Pill> BY_ITEM_KEY = new HashMap<>();

	static {
		for (Pill pill : PillList.ALL) BY_ITEM_KEY.put(pill.itemKey(), pill);
	}

	private Pills() {
	}

	public static List<Pill> all() {
		return PillList.ALL;
	}

	public static Pill byItemKey(String key) {
		return BY_ITEM_KEY.get(key);
	}
}
