package com.ngoducduy.celestialarts.client.gui;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.ModKeybinds;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.cultivation.SpiritQi;
import com.ngoducduy.celestialarts.skill.Skill;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * In-game overlay: qi bar with realm + cultivation progress (bottom left) and the six skill
 * slots with cooldown sweeps and key hints (bottom right).
 *
 * <p>HUD atlas layout (256x64): qi frame (0,0,128,12), qi fill (0,12,124,8), exp fill (0,20,124,3),
 * slot frame (128,0,24,24), active slot frame (152,0,24,24), empty slot (176,0,24,24).</p>
 */
public final class SkillHud {
	private static final Identifier HUD = CelestialArts.id("textures/gui/hud.png");
	private static final int SLOT = 24;
	private static final int GAP = 2;

	private SkillHud() {
	}

	public static void render(DrawContext ctx, float tickDelta) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (player == null || client.options.hudHidden || player.isSpectator()) return;
		PlayerQi qi = QiHolder.get(player);
		TextRenderer font = client.textRenderer;
		int w = ctx.getScaledWindowWidth();
		int h = ctx.getScaledWindowHeight();

		RenderSystem.enableBlend();
		renderQiBar(ctx, font, qi, 8, h - 22);
		renderSkillBar(ctx, font, player, qi, w - 8 - (SLOT + GAP) * PlayerQi.SLOT_COUNT + GAP, h - 8 - SLOT);
		RenderSystem.disableBlend();
	}

	private static void renderQiBar(DrawContext ctx, TextRenderer font, PlayerQi qi, int x, int y) {
		Realm realm = qi.getRealm();
		float frac = qi.getMaxQi() <= 0 ? 0 : MathHelper.clamp(qi.getQi() / qi.getMaxQi(), 0.0F, 1.0F);
		// Frame + fill.
		ctx.drawTexture(HUD, x, y, 0, 0, 128, 12, 256, 64);
		int fillW = Math.round(124 * frac);
		if (fillW > 0) {
			ctx.setShaderColor(r(realm.getRgb()), g(realm.getRgb()), b(realm.getRgb()), 1.0F);
			ctx.drawTexture(HUD, x + 2, y + 2, 0, 12, fillW, 8, 256, 64);
			ctx.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		}
		// Cultivation progress (thin bar under the qi bar).
		int need = qi.getExpForBreakthrough();
		float expFrac = need <= 0 ? 1.0F : MathHelper.clamp(qi.getExp() / (float) need, 0.0F, 1.0F);
		int expW = Math.round(124 * expFrac);
		if (expW > 0) ctx.drawTexture(HUD, x + 2, y + 11, 0, 20, expW, 3, 256, 64);

		// Text: realm + stage (left) and qi numbers (right).
		Text realmName = Text.empty().append(realm.getName()).append(" ").append(qi.getStage().getName());
		String nums = Math.round(qi.getQi()) + " / " + Math.round(qi.getMaxQi());
		// Long realm names (e.g. "Tribulation Transcendence") move up a line instead of colliding with the numbers.
		boolean twoLines = font.getWidth(realmName) + font.getWidth(nums) + 6 > 128;
		ctx.drawText(font, realmName, x + 1, y - (twoLines ? 20 : 10), realm.getRgb(), true);
		ctx.drawText(font, nums, x + 127 - font.getWidth(nums), y - 10, 0xFFFFFF, true);
		// Spiritual qi of the spot, small and dim, above the realm line; brighter while meditating.
		float density = qi.getSpiritQi();
		Text spirit = Text.translatable("gui.celestialarts.spirit_qi_short").append(": ").append(Text.translatable("spiritqi.celestialarts." + SpiritQi.label(density)));
		int spiritY = y - (twoLines ? 30 : 20);
		int spiritAlpha = qi.isMeditating() ? 0xFF : 0xA0;
		ctx.drawText(font, spirit, x + 1, spiritY, (spiritAlpha << 24) | SpiritQi.labelRgb(density), true);
		// Status line above everything: trance in progress, or a breakthrough waiting for a key press.
		if (qi.isMeditating()) {
			Text med = Text.translatable("gui.celestialarts.meditating_short");
			float pulse = 0.55F + 0.45F * MathHelper.sin((System.currentTimeMillis() % 100000L) / 220.0F);
			int a = (int) (pulse * 255) << 24;
			ctx.drawText(font, med, x + 1, spiritY - 10, a | 0x9BE4FF, true);
		} else if (qi.canBreakthrough()) {
			Text ready = Text.translatable(qi.nextBreakthroughIsTribulation() ? "gui.celestialarts.tribulation_ready" : "gui.celestialarts.breakthrough_ready");
			float blink = 0.6F + 0.4F * MathHelper.sin((System.currentTimeMillis() % 100000L) / 150.0F);
			int a = (int) (blink * 255) << 24;
			ctx.drawText(font, ready, x + 1, spiritY - 10, a | (qi.nextBreakthroughIsTribulation() ? 0xD98BFF : 0xFFE9A8), true);
		}
	}

	private static void renderSkillBar(DrawContext ctx, TextRenderer font, ClientPlayerEntity player, PlayerQi qi, int x, int y) {
		for (int i = 0; i < PlayerQi.SLOT_COUNT; i++) {
			int sx = x + i * (SLOT + GAP);
			Skill skill = qi.getSlotSkill(i);
			boolean active = skill != null && qi.hasActiveCast(skill.getId());
			// Frame.
			ctx.drawTexture(HUD, sx, y, skill == null ? 176 : (active ? 152 : 128), 0, SLOT, SLOT, 256, 64);
			if (skill != null) {
				boolean usable = skill.isUsable(player, qi) && qi.getQi() >= skill.getQiCost();
				if (!usable) ctx.setShaderColor(0.45F, 0.45F, 0.45F, 1.0F);
				ctx.drawTexture(skill.getIconTexture(), sx + 2, y + 2, 20, 20, 0, 0, 32, 32, 32, 32);
				ctx.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				// Cooldown sweep from top to bottom.
				float cd = qi.getCooldownProgress(skill.getId());
				if (cd > 0.0F) {
					int covered = Math.round(20 * cd);
					ctx.fill(sx + 2, y + 2, sx + 22, y + 2 + covered, 0xAA000000);
					String secs = String.format("%.1f", qi.getCooldown(skill.getId()) / 20.0F);
					ctx.drawText(font, secs, sx + SLOT / 2 - font.getWidth(secs) / 2, y + 8, 0xFFFFFF, true);
				}
				if (active) {
					ctx.drawBorder(sx + 1, y + 1, SLOT - 2, SLOT - 2, 0xFF00FFD5);
				}
			}
			// Key hint.
			String key = ModKeybinds.SLOTS[i].getBoundKeyLocalizedText().getString();
			if (key.length() > 3) key = key.substring(0, 3);
			ctx.drawText(font, key, sx + SLOT / 2 - font.getWidth(key) / 2, y - 10, 0xDDDDDD, true);
		}
	}

	private static float r(int rgb) {
		return ((rgb >> 16) & 0xFF) / 255.0F;
	}

	private static float g(int rgb) {
		return ((rgb >> 8) & 0xFF) / 255.0F;
	}

	private static float b(int rgb) {
		return (rgb & 0xFF) / 255.0F;
	}
}
