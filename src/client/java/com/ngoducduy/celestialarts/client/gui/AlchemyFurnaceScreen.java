package com.ngoducduy.celestialarts.client.gui;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.alchemy.AlchemyRecipe;
import com.ngoducduy.celestialarts.alchemy.FlameTier;
import com.ngoducduy.celestialarts.alchemy.FurnaceGrade;
import com.ngoducduy.celestialarts.alchemy.FurnaceType;
import com.ngoducduy.celestialarts.alchemy.PillQuality;
import com.ngoducduy.celestialarts.block.entity.AlchemyFurnaceBlockEntity;
import com.ngoducduy.celestialarts.screen.AlchemyFurnaceScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

import static com.ngoducduy.celestialarts.block.entity.AlchemyFurnaceBlockEntity.*;

/**
 * The alchemist's view of the furnace: the recipe's heat curve with its tolerance band, the live
 * heat trace drawn over it, the qi window, and the gauges. Four buttons drive the run.
 */
public class AlchemyFurnaceScreen extends HandledScreen<AlchemyFurnaceScreenHandler> {
	private static final Identifier TEXTURE = CelestialArts.id("textures/gui/alchemy_furnace.png");
	private static final int GX = 60, GY = 18, GW = 128, GH = 56;
	private static final int BAR_X = 60, BAR_W = 128;

	private final List<float[]> trace = new ArrayList<>(); // (progress, heat)
	private int lastState = -1;
	private ButtonWidget startButton;
	private ButtonWidget qiButton;

	public AlchemyFurnaceScreen(AlchemyFurnaceScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		this.backgroundWidth = 200;
		this.backgroundHeight = 222;
		this.titleX = 8;
		this.titleY = 6;
	}

	@Override
	protected void init() {
		super.init();
		int bx = x + 60, by = y + 108;
		addDrawableChild(ButtonWidget.builder(Text.translatable("furnace.celestialarts.btn.less"), b -> click(BTN_LESS)).dimensions(bx, by, 30, 14).build());
		addDrawableChild(ButtonWidget.builder(Text.translatable("furnace.celestialarts.btn.more"), b -> click(BTN_MORE)).dimensions(bx + 32, by, 30, 14).build());
		qiButton = addDrawableChild(ButtonWidget.builder(Text.translatable("furnace.celestialarts.btn.qi"), b -> click(BTN_QI)).dimensions(bx + 64, by, 30, 14).build());
		startButton = addDrawableChild(ButtonWidget.builder(Text.translatable("furnace.celestialarts.btn.start"), b -> click(handler.isRefining() ? BTN_ABORT : BTN_START)).dimensions(bx + 96, by, 32, 14).build());
	}

	private void click(int id) {
		if (client != null && client.interactionManager != null) {
			client.interactionManager.clickButton(handler.syncId, id);
		}
	}

	@Override
	protected void handledScreenTick() {
		super.handledScreenTick();
		int state = handler.get(P_STATE);
		if (state != lastState) {
			if (state == STATE_REFINING) trace.clear();
			lastState = state;
		}
		if (state == STATE_REFINING) {
			float p = handler.get(P_PROGRESS) / 1000.0F;
			float h = handler.get(P_HEAT) / 10.0F;
			if (trace.isEmpty() || p - trace.get(trace.size() - 1)[0] >= 1.0F / GW) trace.add(new float[]{p, h});
		}
		boolean refining = state == STATE_REFINING;
		qiButton.active = refining;
		startButton.setMessage(Text.translatable(refining ? "furnace.celestialarts.btn.abort" : "furnace.celestialarts.btn.start"));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context);
		super.render(context, mouseX, mouseY, delta);
		drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, 256, 256);
		AlchemyRecipe recipe = client != null && client.world != null ? handler.matchRecipe(client.world).orElse(null) : null;
		drawGraph(context, recipe);
		drawBars(context, recipe);
	}

	private void drawGraph(DrawContext context, AlchemyRecipe recipe) {
		int gx = x + GX, gy = y + GY;
		int state = handler.get(P_STATE);
		int typeIdx = handler.get(P_TYPE);
		float tolMul = FurnaceType.values()[MathHelper.clamp(typeIdx, 0, FurnaceType.values().length - 1)].getToleranceMul();
		// grid
		for (int i = 1; i < 4; i++) {
			context.fill(gx, gy + i * GH / 4, gx + GW, gy + i * GH / 4 + 1, 0x22FFFFFF);
		}
		if (recipe != null) {
			// qi window at the bottom
			int qs = gx + Math.round(recipe.qiWindow().start() * GW), qe = gx + Math.round(recipe.qiWindow().end() * GW);
			context.fill(qs, gy + GH - 4, qe, gy + GH, 0x8060D8FF);
			// tolerance band + target line
			for (int i = 0; i < GW; i++) {
				float p = (i + 0.5F) / GW;
				float t = recipe.targetHeat(p);
				float tol = recipe.tolerance(p) * tolMul;
				int y0 = gy + GH - Math.round((t + tol) / 100.0F * GH);
				int y1 = gy + GH - Math.round((t - tol) / 100.0F * GH);
				context.fill(gx + i, MathHelper.clamp(y0, gy, gy + GH), gx + i + 1, MathHelper.clamp(y1, gy, gy + GH), 0x50FFB040);
				int yt = gy + GH - Math.round(t / 100.0F * GH);
				context.fill(gx + i, MathHelper.clamp(yt, gy, gy + GH - 1), gx + i + 1, MathHelper.clamp(yt, gy, gy + GH - 1) + 1, 0xFFFFC050);
			}
		}
		// heat trace
		int prevX = -1, prevY = -1;
		for (float[] pt : trace) {
			int px = gx + Math.round(pt[0] * (GW - 1));
			int py = gy + GH - 1 - Math.round(pt[1] / 100.0F * (GH - 1));
			if (prevX >= 0) {
				int yA = Math.min(prevY, py), yB = Math.max(prevY, py);
				context.fill(px, yA, px + 1, yB + 1, 0xFFFF5A2A);
			} else {
				context.fill(px, py, px + 1, py + 1, 0xFFFF5A2A);
			}
			prevX = px;
			prevY = py;
		}
		if (state == STATE_REFINING) {
			float p = handler.get(P_PROGRESS) / 1000.0F;
			int cx = gx + Math.round(p * (GW - 1));
			context.fill(cx, gy, cx + 1, gy + GH, 0x90FFFFFF);
			float h = handler.get(P_HEAT) / 10.0F;
			int hy = gy + GH - 1 - Math.round(h / 100.0F * (GH - 1));
			context.fill(cx - 1, hy - 1, cx + 2, hy + 2, 0xFFFFFFFF);
		}
	}

	private void drawBars(DrawContext context, AlchemyRecipe recipe) {
		int bx = x + BAR_X;
		float heat = handler.get(P_HEAT) / 10.0F;
		float qi = handler.get(P_QI) / 10.0F;
		float dmg = handler.get(P_DAMAGE) / 10.0F;
		bar(context, bx, y + 80, heat / 100.0F, 0xFFFF7A1A, 0xFFFFC050);
		int qiMin = handler.get(P_QI_PHASE);
		bar(context, bx, y + 90, qi / 100.0F, 0xFF3AB8E8, 0xFF9BE4FF);
		if (qiMin >= 0) {
			int mx = bx + Math.round(qiMin / 100.0F * (BAR_W - 2));
			context.fill(mx, y + 89, mx + 1, y + 97, 0xFFFFFFFF);
		}
		bar(context, bx, y + 100, dmg / RUIN_DAMAGE, 0xFFB8202A, 0xFFFF6A6A);
		// fire level pips beside the flame slot
		int level = handler.get(P_FIRE_LEVEL);
		for (int i = 0; i < MAX_FIRE_LEVEL; i++) {
			int px = x + 40, py = y + 96 - i * 4;
			context.fill(px, py, px + 6, py + 3, i < level ? 0xFFFF8A2A : 0x40FFFFFF);
		}
	}

	private void bar(DrawContext context, int bx, int by, float frac, int color, int light) {
		int w = Math.round(MathHelper.clamp(frac, 0.0F, 1.0F) * (BAR_W - 2));
		if (w <= 0) return;
		context.fill(bx + 1, by + 1, bx + 1 + w, by + 6, color);
		context.fill(bx + 1, by + 1, bx + 1 + w, by + 2, light);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		int typeIdx = MathHelper.clamp(handler.get(P_TYPE), 0, FurnaceType.values().length - 1);
		int gradeIdx = MathHelper.clamp(handler.get(P_GRADE), 0, FurnaceGrade.values().length - 1);
		Text name = Text.translatable("block.celestialarts.alchemy_furnace_" + FurnaceType.values()[typeIdx].getKey() + "_" + FurnaceGrade.values()[gradeIdx].getKey());
		context.drawText(textRenderer, name, titleX, titleY, 0xFFE9A8, false);
		// live heat number in the graph corner
		String heatStr = String.format("%.0f", handler.get(P_HEAT) / 10.0F);
		context.drawText(textRenderer, heatStr, GX + GW - 2 - textRenderer.getWidth(heatStr), GY + 2, 0xFFC090, false);
		// fire level digit beside the pips
		context.drawText(textRenderer, String.valueOf(handler.get(P_FIRE_LEVEL)), 48, 91, 0xFFB070, false);
		// recipe + status lines between the buttons and the inventory
		AlchemyRecipe recipe = client != null && client.world != null ? handler.matchRecipe(client.world).orElse(null) : null;
		if (recipe != null) {
			Text rn = Text.translatable("furnace.celestialarts.recipe", recipe.getOutput(client.world.getRegistryManager()).getName());
			context.drawText(textRenderer, rn, 8, 121, 0xA0A0A0, false);
		}
		context.drawText(textRenderer, statusText(), 8, 130, 0xE0E0E0, false);
	}

	@Override
	protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
		super.drawMouseoverTooltip(context, mouseX, mouseY);
		int mx = mouseX - x, my = mouseY - y;
		if (mx >= BAR_X && mx <= BAR_X + BAR_W) {
			Text tip = null;
			if (my >= 79 && my <= 87) tip = Text.translatable("furnace.celestialarts.heat").append(": " + handler.get(P_HEAT) / 10);
			else if (my >= 89 && my <= 97) tip = Text.translatable("furnace.celestialarts.qi").append(": " + handler.get(P_QI) / 10);
			else if (my >= 99 && my <= 107) tip = Text.translatable("furnace.celestialarts.damage").append(": " + handler.get(P_DAMAGE) / 10 + " / " + (int) RUIN_DAMAGE);
			if (tip != null) context.drawTooltip(textRenderer, tip, mouseX, mouseY);
		}
		if (mx >= 38 && mx <= 56 && my >= 82 && my <= 100) {
			context.drawTooltip(textRenderer, Text.translatable("furnace.celestialarts.fire_level", handler.get(P_FIRE_LEVEL)), mouseX, mouseY);
		}
	}

	private Text statusText() {
		int state = handler.get(P_STATE);
		int err = handler.get(P_ERROR);
		if (err != ERR_NONE) {
			return switch (err) {
				case ERR_NO_RECIPE -> Text.translatable("furnace.celestialarts.state.no_recipe");
				case ERR_NO_FIRE -> Text.translatable("furnace.celestialarts.state.no_fire");
				case ERR_WEAK_FIRE -> Text.translatable("furnace.celestialarts.state.weak_fire", requiredTier());
				case ERR_FURNACE_GRADE -> Text.translatable("furnace.celestialarts.state.furnace_grade", requiredGrade());
				case ERR_OUTPUT_FULL -> Text.translatable("furnace.celestialarts.state.output_full");
				default -> Text.empty();
			};
		}
		return switch (state) {
			case STATE_REFINING -> handler.get(P_QI_PHASE) >= 0 && handler.get(P_QI) / 10.0F < handler.get(P_QI_PHASE)
					? Text.translatable("furnace.celestialarts.state.qi_phase")
					: Text.translatable("furnace.celestialarts.state.refining", handler.get(P_PROGRESS) / 10);
			case STATE_DONE -> Text.translatable("furnace.celestialarts.state.done", PillQuality.byIndex(Math.max(0, handler.get(P_QUALITY))).getName());
			case STATE_RUINED -> Text.translatable("furnace.celestialarts.state.ruined");
			default -> Text.translatable("furnace.celestialarts.state.idle");
		};
	}

	private int requiredTier() {
		if (client == null || client.world == null) return 0;
		return handler.matchRecipe(client.world).map(AlchemyRecipe::fireTier).orElse(0);
	}

	private int requiredGrade() {
		if (client == null || client.world == null) return 0;
		return handler.matchRecipe(client.world).map(AlchemyRecipe::grade).orElse(0);
	}

	@SuppressWarnings("unused")
	private static FlameTier tierOf(int tier) {
		for (FlameTier t : FlameTier.values()) if (t.getTier() == tier) return t;
		return FlameTier.MORTAL;
	}
}
