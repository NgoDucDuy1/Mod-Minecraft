package com.ngoducduy.celestialarts.client;

import com.ngoducduy.celestialarts.client.gui.CultivationScreen;
import com.ngoducduy.celestialarts.client.gui.SkillBookScreen;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.skill.Skill;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Six skill slot keys (R, F, V, G, C, Z by default), a skill-book key (K), a cultivation-panel key (N),
 * a meditation toggle (X) and a breakthrough key (B).
 * Slot keys send a cast on press; channelled skills additionally send a release when the key goes up.
 */
public final class ModKeybinds {
	public static final String CATEGORY = "category.celestialarts";
	public static final KeyBinding[] SLOTS = new KeyBinding[PlayerQi.SLOT_COUNT];
	public static KeyBinding OPEN_BOOK;
	public static KeyBinding BREAKTHROUGH;
	public static KeyBinding MEDITATE;
	public static KeyBinding OPEN_CULTIVATION;
	private static final int[] DEFAULT_KEYS = {GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_Z};
	private static final boolean[] HELD = new boolean[PlayerQi.SLOT_COUNT];

	private ModKeybinds() {
	}

	public static void register() {
		for (int i = 0; i < SLOTS.length; i++) {
			SLOTS[i] = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.celestialarts.skill_" + (i + 1), InputUtil.Type.KEYSYM, DEFAULT_KEYS[i], CATEGORY));
		}
		OPEN_BOOK = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.celestialarts.open_book", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));
		BREAKTHROUGH = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.celestialarts.breakthrough", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
		MEDITATE = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.celestialarts.meditate", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY));
		OPEN_CULTIVATION = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.celestialarts.open_cultivation", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY));
	}

	public static void tick(MinecraftClient client) {
		if (client.player == null) return;
		PlayerQi qi = QiHolder.get(client.player);

		while (OPEN_BOOK.wasPressed()) {
			if (client.currentScreen == null) client.setScreen(new SkillBookScreen());
		}
		while (OPEN_CULTIVATION.wasPressed()) {
			if (client.currentScreen == null) client.setScreen(new CultivationScreen());
		}
		while (BREAKTHROUGH.wasPressed()) {
			ClientPackets.sendBreakthrough();
		}
		while (MEDITATE.wasPressed()) {
			// One press sits down, the next stands up; the server owns the state and syncs it back.
			ClientPackets.sendMeditate();
		}

		for (int i = 0; i < SLOTS.length; i++) {
			KeyBinding key = SLOTS[i];
			boolean down = key.isPressed() && client.currentScreen == null;
			if (down && !HELD[i]) {
				HELD[i] = true;
				Skill skill = qi.getSlotSkill(i);
				if (skill != null) {
					ClientPackets.sendCast(i);
				}
			} else if (!down && HELD[i]) {
				HELD[i] = false;
				Skill skill = qi.getSlotSkill(i);
				if (skill != null && skill.isChannel()) {
					ClientPackets.sendRelease(i);
				}
			}
			// Drain the press queue so it doesn't fire later.
			while (key.wasPressed()) {
				// handled through isPressed edge detection above
			}
		}
	}
}
