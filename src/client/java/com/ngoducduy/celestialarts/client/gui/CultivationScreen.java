package com.ngoducduy.celestialarts.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.ClientPackets;
import com.ngoducduy.celestialarts.cultivation.CultivationStats;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.cultivation.Realm;
import com.ngoducduy.celestialarts.cultivation.SpiritQi;
import com.ngoducduy.celestialarts.cultivation.SpiritRoot;
import com.ngoducduy.celestialarts.cultivation.Stage;
import com.ngoducduy.celestialarts.cultivation.Talent;
import com.ngoducduy.celestialarts.skill.Element;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Đạo Cơ – the cultivation panel: realm and minor stage, progress to the next breakthrough (with
 * its odds, or the size of the waiting tribulation), the spiritual qi of the current spot, the
 * spirit root, the talent and the aptitude they add up to. Buttons start meditation, attempt the
 * breakthrough or jump to the skill book.
 *
 * <p>Layout mirrors {@code textures/gui/cultivation.png} (256x222): title bar, left column
 * "Cảnh giới" (10..125), right column "Linh căn / Thiên phú" (132..246), button row at 196.</p>
 */
public class CultivationScreen extends Screen {
	private static final Identifier BG = CelestialArts.id("textures/gui/cultivation.png");
	private static final int PANEL_W = 256;
	private static final int PANEL_H = 222;
	private static final int LEFT_X = 12;
	private static final int RIGHT_X = 134;

	private int left;
	private int top;
	private ButtonWidget meditateButton;
	private ButtonWidget breakthroughButton;

	public CultivationScreen() {
		super(Text.translatable("gui.celestialarts.cultivation"));
	}

	private static PlayerQi qi() {
		return QiHolder.get(MinecraftClient.getInstance().player);
	}

	private static Identifier rootIcon(SpiritRoot.Kind kind) {
		return CelestialArts.id("textures/gui/roots/" + kind.getKey() + ".png");
	}

	private static Identifier talentSeal(Talent.Rarity rarity) {
		return CelestialArts.id("textures/gui/talent_" + rarity.getKey() + ".png");
	}

	@Override
	protected void init() {
		super.init();
		this.left = (this.width - PANEL_W) / 2;
		this.top = (this.height - PANEL_H) / 2;
		this.meditateButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.celestialarts.meditate"), b -> {
			ClientPackets.sendMeditate();
			this.close();
		}).dimensions(left + 10, top + 196, 74, 20).tooltip(Tooltip.of(Text.translatable("gui.celestialarts.meditate_tip"))).build());
		this.breakthroughButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.celestialarts.breakthrough"), b -> {
			ClientPackets.sendBreakthrough();
			this.close();
		}).dimensions(left + 91, top + 196, 74, 20).build());
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.celestialarts.open_book"), b ->
				MinecraftClient.getInstance().setScreen(new SkillBookScreen())
		).dimensions(left + 172, top + 196, 74, 20).build());
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		this.renderBackground(ctx);
		RenderSystem.enableBlend();
		ctx.drawTexture(BG, left, top, 0, 0, PANEL_W, PANEL_H, 256, 256);
		ctx.drawText(textRenderer, this.title, left + 12, top + 10, 0xFFE9A8, false);

		PlayerQi qi = qi();
		List<Text> tooltip = new ArrayList<>();
		renderRealmColumn(ctx, qi, mouseX, mouseY, tooltip);
		renderRootColumn(ctx, qi, mouseX, mouseY, tooltip);

		meditateButton.setMessage(Text.translatable(qi.isMeditating() ? "gui.celestialarts.meditate_stop" : "gui.celestialarts.meditate"));
		breakthroughButton.active = qi.canBreakthrough();
		breakthroughButton.setMessage(Text.translatable(qi.nextBreakthroughIsTribulation() ? "gui.celestialarts.tribulation" : "gui.celestialarts.breakthrough"));

		super.render(ctx, mouseX, mouseY, delta);
		if (!tooltip.isEmpty()) ctx.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
		RenderSystem.disableBlend();
	}

	// ------------------------------------------------------------ left

	private void renderRealmColumn(DrawContext ctx, PlayerQi qi, int mouseX, int mouseY, List<Text> tooltip) {
		int x = left + LEFT_X;
		int y = top + 30;
		Realm realm = qi.getRealm();
		Stage stage = qi.getStage();

		ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.realm_header"), x, y, 0xC8C8C8, false);
		y += 12;
		MutableText realmLine = Text.empty().append(realm.getName()).append(Text.literal(" · ").formatted(Formatting.GRAY)).append(stage.getName().copy().formatted(realm.getColor()));
		ctx.drawText(textRenderer, realmLine, x, y, realm.getRgb(), false);
		y += 10;
		ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.rank", qi.getRank(), Realm.values().length * Stage.values().length), x, y, 0x9A9A9A, false);
		y += 14;

		// Progress to the next breakthrough.
		int need = qi.getExpForBreakthrough();
		float frac = need <= 0 ? 1.0F : MathHelper.clamp(qi.getExp() / (float) need, 0.0F, 1.0F);
		ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.exp"), x, y, 0xFFE9A8, false);
		String expText = need < 0 ? Text.translatable("gui.celestialarts.max_realm").getString() : qi.getExp() + " / " + need;
		ctx.drawText(textRenderer, expText, x + 112 - textRenderer.getWidth(expText), y, 0xFFFFFF, false);
		y += 10;
		bar(ctx, x, y, 112, 6, frac, 0xFFE0A040, 0xFF3A2A18);
		y += 10;
		// Qi pool.
		ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.qi"), x, y, 0x9BE4FF, false);
		String qiText = Math.round(qi.getQi()) + " / " + Math.round(qi.getMaxQi());
		ctx.drawText(textRenderer, qiText, x + 112 - textRenderer.getWidth(qiText), y, 0xFFFFFF, false);
		y += 10;
		bar(ctx, x, y, 112, 6, qi.getMaxQi() <= 0 ? 0 : qi.getQi() / qi.getMaxQi(), 0xFF5AB8FF, 0xFF182A3A);
		y += 12;

		// What the next breakthrough looks like.
		if (qi.isAtPeakOfCultivation()) {
			ctx.drawTextWrapped(textRenderer, Text.translatable("gui.celestialarts.at_peak"), x, y, 112, 0xFFD36B);
			y += 20;
		} else if (qi.nextBreakthroughIsTribulation()) {
			int bolts = CultivationStats.tribulationBolts(qi);
			float dmg = CultivationStats.tribulationBoltDamage(qi) * qi.getTalent().tribulationDamageMultiplier();
			ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.next_tribulation", realm.next().getName()), x, y, 0xD98BFF, false);
			y += 10;
			ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.tribulation_stats", bolts, String.format("%.1f", dmg / 2.0F)), x, y, 0xC8C8C8, false);
			y += 10;
			if (hover(mouseX, mouseY, x, y - 20, 112, 20)) {
				tooltip.add(Text.translatable("gui.celestialarts.tribulation_tip1"));
				tooltip.add(Text.translatable("gui.celestialarts.tribulation_tip2").formatted(Formatting.GRAY));
				tooltip.add(Text.translatable("gui.celestialarts.tribulation_tip3").formatted(Formatting.RED));
			}
		} else {
			int chance = Math.round(CultivationStats.breakthroughChance(qi, qi.getSpiritQi()) * 100);
			ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.next_stage", stage.next().getName()), x, y, 0xC8C8C8, false);
			y += 10;
			int c = chance >= 80 ? 0x9CFFB0 : chance >= 55 ? 0xFFE08A : 0xFF8A8A;
			ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.chance", chance), x, y, c, false);
			y += 10;
			if (hover(mouseX, mouseY, x, y - 20, 112, 20)) {
				tooltip.add(Text.translatable("gui.celestialarts.chance_tip1"));
				tooltip.add(Text.translatable("gui.celestialarts.chance_tip2").formatted(Formatting.GRAY));
				tooltip.add(Text.translatable("gui.celestialarts.chance_tip3").formatted(Formatting.RED));
			}
		}
		y += 4;

		// Spiritual qi of this spot.
		float d = qi.getSpiritQi();
		ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.spirit_qi"), x, y, 0xC8C8C8, false);
		y += 10;
		Text label = Text.translatable("spiritqi.celestialarts." + SpiritQi.label(d));
		ctx.drawText(textRenderer, Text.empty().append(label).append(Text.literal(String.format("  ×%.2f", d)).formatted(Formatting.GRAY)), x, y, SpiritQi.labelRgb(d), false);
		y += 10;
		bar(ctx, x, y, 112, 4, MathHelper.clamp(d / SpiritQi.MAX, 0.0F, 1.0F), 0xFF000000 | SpiritQi.labelRgb(d), 0xFF202020);
		if (hover(mouseX, mouseY, x, y - 20, 112, 26)) {
			tooltip.add(Text.translatable("gui.celestialarts.spirit_qi_tip1"));
			tooltip.add(Text.translatable("gui.celestialarts.spirit_qi_tip2").formatted(Formatting.GRAY));
			tooltip.add(Text.translatable("gui.celestialarts.spirit_qi_tip3").formatted(Formatting.GRAY));
		}
		y += 8;
		if (qi.isMeditating()) {
			float perSecond = (0.6F + 0.4F * realm.getLevel()) * d * CultivationStats.expMultiplier(qi);
			ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.meditating", String.format("%.1f", perSecond)).formatted(Formatting.AQUA), x, y, 0x9BE4FF, false);
		} else {
			ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.meditate_hint"), x, y, 0x8A8A8A, false);
		}
	}

	// ----------------------------------------------------------- right

	private void renderRootColumn(DrawContext ctx, PlayerQi qi, int mouseX, int mouseY, List<Text> tooltip) {
		int x = left + RIGHT_X;
		int y = top + 30;
		SpiritRoot root = qi.getRoot();
		Talent talent = qi.getTalent();

		ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.root_header"), x, y, 0xC8C8C8, false);
		y += 12;
		if (root == null) {
			ctx.drawTextWrapped(textRenderer, Text.translatable("gui.celestialarts.root_unawakened"), x, y, 112, 0x9A9A9A);
			y += 30;
		} else {
			int ix = x;
			for (SpiritRoot.Kind k : root.getKinds()) {
				ctx.drawTexture(rootIcon(k), ix, y, 22, 22, 0, 0, 32, 32, 32, 32);
				if (hover(mouseX, mouseY, ix, y, 22, 22)) {
					tooltip.add(k.getName());
					tooltip.add(Text.translatable("gui.celestialarts.root_attuned", k.getElement().getName()).formatted(Formatting.GRAY));
					tooltip.add(Text.translatable("gui.celestialarts.root_bonus", Math.round((root.damageMultiplier(k.getElement()) - 1) * 100), Math.round((1 - root.qiCostMultiplier(k.getElement())) * 100)).formatted(Formatting.GREEN));
				}
				ix += 25;
			}
			ctx.drawText(textRenderer, root.getTypeName(), ix + 2, y + 2, 0xFFFFFF, false);
			ctx.drawText(textRenderer, root.getGradeName(), ix + 2, y + 12, 0xFFFFFF, false);
			y += 26;
			ctx.drawText(textRenderer, root.getKindsText(), x, y, 0xFFFFFF, false);
			y += 10;
			ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.root_purity", Math.round((root.purityRegenMultiplier() - 1) * 100)), x, y, 0x9A9A9A, false);
			y += 12;
		}

		ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.talent_header"), x, y, 0xC8C8C8, false);
		y += 12;
		ctx.drawTexture(talentSeal(talent.getRarity()), x, y, 22, 22, 0, 0, 32, 32, 32, 32);
		ctx.drawText(textRenderer, talent.getName(), x + 26, y + 2, 0xFFFFFF, false);
		ctx.drawText(textRenderer, talent.getRarity().getName(), x + 26, y + 12, 0xFFFFFF, false);
		if (hover(mouseX, mouseY, x, y, 112, 22)) {
			tooltip.add(talent.getName());
			tooltip.add(talent.getDescription());
		}
		y += 26;
		int lines = drawWrapped(ctx, talent.getDescription(), x, y, 112, 0x9A9A9A, 3);
		y += lines * 9 + 4;

		int apt = CultivationStats.aptitude(qi);
		ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.aptitude_header"), x, y, 0xC8C8C8, false);
		y += 12;
		int ac = apt >= 80 ? 0xFFD36B : apt >= 60 ? 0x8AF3FF : apt >= 40 ? 0x9CFFB0 : apt >= 20 ? 0xFFFFFF : 0x9A9A9A;
		ctx.drawText(textRenderer, Text.literal(apt + " / 100  ").append(Text.translatable("aptitude.celestialarts." + CultivationStats.aptitudeTier(apt))), x, y, ac, false);
		y += 10;
		bar(ctx, x, y, 112, 5, apt / 100.0F, 0xFF000000 | ac, 0xFF202020);
		y += 9;
		ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.power", Math.round(CultivationStats.powerMultiplier(qi) * 100),
				Math.round(CultivationStats.expMultiplier(qi) * 100)), x, y, 0x9A9A9A, false);
		if (hover(mouseX, mouseY, x, y - 31, 112, 40)) {
			tooltip.add(Text.translatable("gui.celestialarts.aptitude_tip1"));
			tooltip.add(Text.translatable("gui.celestialarts.aptitude_tip2").formatted(Formatting.GRAY));
			tooltip.add(Text.translatable("gui.celestialarts.aptitude_tip3").formatted(Formatting.GRAY));
			if (root != null) {
				for (Element e : Element.values()) {
					float m = CultivationStats.skillDamageMultiplier(qi, e) / CultivationStats.skillDamageMultiplier(qi, null);
					if (Math.abs(m - 1.0F) > 0.001F) {
						tooltip.add(Text.empty().append(e.getName()).append(Text.literal(String.format(": %+d%%", Math.round((m - 1) * 100))).formatted(m > 1 ? Formatting.GREEN : Formatting.RED)));
					}
				}
			}
		}
	}

	// ---------------------------------------------------------- helpers

	private static void bar(DrawContext ctx, int x, int y, int w, int h, float frac, int fill, int back) {
		ctx.fill(x, y, x + w, y + h, back);
		int fw = Math.round((w - 2) * MathHelper.clamp(frac, 0.0F, 1.0F));
		if (fw > 0) ctx.fill(x + 1, y + 1, x + 1 + fw, y + h - 1, fill);
		ctx.drawBorder(x, y, w, h, 0xFF5A4A2A);
	}

	private int drawWrapped(DrawContext ctx, Text text, int x, int y, int width, int color, int maxLines) {
		var lines = textRenderer.wrapLines(text, width);
		int n = Math.min(maxLines, lines.size());
		for (int i = 0; i < n; i++) {
			ctx.drawText(textRenderer, lines.get(i), x, y + i * 9, color, false);
		}
		return n;
	}

	private static boolean hover(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
