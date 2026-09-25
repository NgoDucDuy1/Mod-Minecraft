package com.ngoducduy.celestialarts.client.gui;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.client.ClientPackets;
import com.ngoducduy.celestialarts.cultivation.PlayerQi;
import com.ngoducduy.celestialarts.cultivation.QiHolder;
import com.ngoducduy.celestialarts.skill.Skill;
import com.ngoducduy.celestialarts.skill.SkillRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Dao Manual: shows every learned art, the six skill slots and cultivation status.
 * Click an art then a slot to bind it (or right-click a slot to clear it).
 *
 * <p>Background texture 256x256 with the panel occupying (0,0)-(256,200).</p>
 */
public class SkillBookScreen extends Screen {
	private static final Identifier BG = CelestialArts.id("textures/gui/skill_book.png");
	private static final int PANEL_W = 256;
	private static final int PANEL_H = 222;
	private static final int CELL = 36;
	private static final int COLS = 4;
	private static final int ROWS = 4;
	private static final int ICON = 32;
	private static final int PAGE_SIZE = COLS * ROWS;

	private final List<Skill> learned = new ArrayList<>();
	@Nullable
	private Skill selected;
	private int left;
	private int top;
	private ButtonWidget breakthroughButton;
	private ButtonWidget prevPage;
	private ButtonWidget nextPage;
	private int page;

	public SkillBookScreen() {
		super(Text.translatable("gui.celestialarts.title"));
	}

	@Override
	protected void init() {
		super.init();
		this.left = (this.width - PANEL_W) / 2;
		this.top = (this.height - PANEL_H) / 2;
		refreshLearned();
		this.breakthroughButton = this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.celestialarts.breakthrough"), b -> {
			ClientPackets.sendBreakthrough();
			this.close();
		}).dimensions(left + 166, top + 186, 78, 20).build());
		this.prevPage = this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> setPage(page - 1))
				.dimensions(left + 122, top + 24, 14, 12).build());
		this.nextPage = this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> setPage(page + 1))
				.dimensions(left + 142, top + 24, 14, 12).build());
		setPage(page);
	}

	private int pageCount() {
		return Math.max(1, (learned.size() + PAGE_SIZE - 1) / PAGE_SIZE);
	}

	private void setPage(int p) {
		page = Math.floorMod(p, pageCount());
		boolean multi = pageCount() > 1;
		prevPage.visible = multi;
		nextPage.visible = multi;
	}

	/** Skill shown in grid cell {@code i} of the current page, or null. */
	@Nullable
	private Skill cell(int i) {
		int idx = page * PAGE_SIZE + i;
		return idx < learned.size() ? learned.get(idx) : null;
	}

	private void refreshLearned() {
		learned.clear();
		if (client == null || client.player == null) return;
		PlayerQi qi = QiHolder.get(client.player);
		for (Skill s : SkillRegistry.all()) {
			if (qi.hasLearned(s.getId())) learned.add(s);
		}
		learned.sort(Comparator.comparingInt((Skill s) -> s.getRealm().getLevel()).thenComparing(s -> s.getId().getPath()));
	}

	private PlayerQi qi() {
		return QiHolder.get(client.player);
	}

	// --------------------------------------------------------------- layout

	private int gridX(int col) {
		return left + 12 + col * CELL;
	}

	private int gridY(int row) {
		return top + 40 + row * CELL;
	}

	private int slotX() {
		return left + 176;
	}

	private int slotY(int i) {
		return top + 40 + i * 22;
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		this.renderBackground(ctx);
		RenderSystem.enableBlend();
		ctx.drawTexture(BG, left, top, 0, 0, PANEL_W, PANEL_H, 256, 256);
		ctx.drawText(textRenderer, this.title, left + 12, top + 10, 0xFFE9A8, false);
		ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.learned"), left + 12, top + 28, 0xC8C8C8, false);
		ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.slots"), slotX(), top + 28, 0xC8C8C8, false);

		PlayerQi qi = qi();
		Skill hovered = null;

		// Learned grid.
		if (learned.isEmpty()) {
			ctx.drawTextWrapped(textRenderer, Text.translatable("gui.celestialarts.empty"), gridX(0), gridY(0), CELL * COLS, 0x9A9A9A);
		}
		if (pageCount() > 1) {
			String pg = (page + 1) + "/" + pageCount();
			ctx.drawCenteredTextWithShadow(textRenderer, pg, left + 139, top + 14, 0xC8C8C8);
		}
		for (int i = 0; i < PAGE_SIZE; i++) {
			Skill s = cell(i);
			if (s == null) break;
			int x = gridX(i % COLS);
			int y = gridY(i / COLS);
			boolean usable = s.getRealm().getLevel() <= qi.getRealm().getLevel();
			boolean hover = mouseX >= x && mouseX < x + ICON && mouseY >= y && mouseY < y + ICON;
			if (s == selected) ctx.fill(x - 2, y - 2, x + ICON + 2, y + ICON + 2, 0x66FFE9A8);
			else if (hover) ctx.fill(x - 2, y - 2, x + ICON + 2, y + ICON + 2, 0x33FFFFFF);
			if (!usable) ctx.setShaderColor(0.4F, 0.4F, 0.4F, 1.0F);
			ctx.drawTexture(s.getIconTexture(), x, y, ICON, ICON, 0, 0, 32, 32, 32, 32);
			ctx.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			ctx.drawBorder(x - 1, y - 1, ICON + 2, ICON + 2, usable ? 0xFF5A4A2A : 0xFF3A3A3A);
			if (hover) hovered = s;
		}

		// Slots.
		for (int i = 0; i < PlayerQi.SLOT_COUNT; i++) {
			int x = slotX();
			int y = slotY(i);
			Skill s = qi.getSlotSkill(i);
			boolean hover = mouseX >= x && mouseX < x + 70 && mouseY >= y && mouseY < y + 20;
			ctx.fill(x, y, x + 70, y + 20, hover ? 0x55FFFFFF : 0x33000000);
			ctx.drawBorder(x, y, 70, 20, selected != null && hover ? 0xFFFFE9A8 : 0xFF5A4A2A);
			ctx.drawText(textRenderer, String.valueOf(i + 1), x + 3, y + 6, 0xFFE9A8, false);
			if (s != null) {
				ctx.drawTexture(s.getIconTexture(), x + 12, y + 2, 16, 16, 0, 0, 32, 32, 32, 32);
				String name = s.getName().getString();
				name = textRenderer.trimToWidth(name, 38);
				ctx.drawText(textRenderer, name, x + 30, y + 6, 0xE0E0E0, false);
				if (hover && selected == null) hovered = s;
			}
		}

		// Status line.
		int sy = top + 200;
		ctx.drawText(textRenderer, qi.getRealm().getName(), left + 13, sy - 10, qi.getRealm().getRgb(), false);
		ctx.drawText(textRenderer, Text.translatable("gui.celestialarts.qi").append(": " + Math.round(qi.getQi()) + "/" + Math.round(qi.getMaxQi())), left + 13, sy, 0x9BE4FF, false);
		int need = qi.getExpForBreakthrough();
		Text expLine = need < 0
				? Text.translatable("gui.celestialarts.exp").append(": ").append(Text.translatable("gui.celestialarts.max_realm"))
				: Text.translatable("gui.celestialarts.exp").append(": " + qi.getExp() + "/" + need);
		ctx.drawText(textRenderer, expLine, left + 13, sy + 10, 0xFFE9A8, false);
		// Usage hint below the panel when the window is tall enough for it.
		if (this.height - (top + PANEL_H) >= 16) {
			Text hint = Text.translatable("gui.celestialarts.hint");
			ctx.drawCenteredTextWithShadow(textRenderer, hint, this.width / 2, top + PANEL_H + 5, 0xA0A0A0);
		}
		breakthroughButton.active = qi.canBreakthrough();

		super.render(ctx, mouseX, mouseY, delta);
		if (hovered != null) ctx.drawTooltip(textRenderer, tooltip(hovered, qi), mouseX, mouseY);
		RenderSystem.disableBlend();
	}

	private List<Text> tooltip(Skill s, PlayerQi qi) {
		List<Text> lines = new ArrayList<>();
		lines.add(s.getName().copy().formatted(s.getElement().getFormatting(), Formatting.BOLD));
		lines.add(Text.translatable("tooltip.celestialarts.type", s.getType().getName(), s.getElement().getName()).formatted(Formatting.GRAY));
		lines.add(Text.translatable("tooltip.celestialarts.cost", Math.round(s.getQiCost()), String.format("%.1f", s.getCooldownTicks() / 20.0F)).formatted(Formatting.AQUA));
		boolean ok = s.getRealm().getLevel() <= qi.getRealm().getLevel();
		lines.add(Text.translatable("tooltip.celestialarts.realm", s.getRealm().getName()).formatted(ok ? Formatting.GREEN : Formatting.RED));
		if (!ok) lines.add(Text.translatable("gui.celestialarts.locked").formatted(Formatting.RED));
		if (s.isChannel()) lines.add(Text.translatable("gui.celestialarts.channel").formatted(Formatting.YELLOW));
		lines.add(Text.empty());
		for (String part : s.getDescription().getString().split("\n")) {
			lines.add(Text.literal(part).formatted(Formatting.ITALIC, Formatting.DARK_GRAY));
		}
		return lines;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (pageCount() > 1 && mouseX >= gridX(0) - 4 && mouseX < gridX(COLS) && mouseY >= gridY(0) - 4 && mouseY < gridY(ROWS)) {
			setPage(page + (amount < 0 ? 1 : -1));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		PlayerQi qi = qi();
		// Grid.
		for (int i = 0; i < PAGE_SIZE; i++) {
			Skill s = cell(i);
			if (s == null) break;
			int x = gridX(i % COLS);
			int y = gridY(i / COLS);
			if (mouseX >= x && mouseX < x + ICON && mouseY >= y && mouseY < y + ICON) {
				if (button == 0) {
					selected = selected == s ? null : s;
					playClick();
					return true;
				}
			}
		}
		// Slots.
		for (int i = 0; i < PlayerQi.SLOT_COUNT; i++) {
			int x = slotX();
			int y = slotY(i);
			if (mouseX >= x && mouseX < x + 70 && mouseY >= y && mouseY < y + 20) {
				if (button == 1) {
					qi.setSlot(i, null);
					ClientPackets.sendSetSlot(i, null);
					playClick();
					return true;
				}
				if (button == 0 && selected != null) {
					// Remove the skill from any other slot so it is bound only once.
					for (int j = 0; j < PlayerQi.SLOT_COUNT; j++) {
						if (j != i && selected.getId().equals(qi.getSlot(j))) {
							qi.setSlot(j, null);
							ClientPackets.sendSetSlot(j, null);
						}
					}
					qi.setSlot(i, selected.getId());
					ClientPackets.sendSetSlot(i, selected.getId());
					selected = null;
					playClick();
					return true;
				}
			}
		}
		return false;
	}

	private void playClick() {
		if (client != null) {
			client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0F));
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
