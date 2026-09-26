package com.ngoducduy.celestialarts.alchemy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ngoducduy.celestialarts.registry.ModRecipes;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Đan phương – one way of refining one pill. Besides the herbs it carries the whole balancing
 * profile the furnace plays against: a piecewise heat curve (target ± tolerance over the run),
 * a window near the end in which spirit qi must be kept above a threshold (ngưng đan), the
 * volatility of the heat and the minimum flame tier. Data: {@code data/celestialarts/recipes/alchemy}.
 */
public class AlchemyRecipe implements Recipe<Inventory> {
	/** Herb slots of the furnace inventory that the recipe reads. */
	public static final int HERB_SLOTS = 6;

	public record IngredientStack(Item item, int count) {
	}

	/** One stretch of the run: {@code span} is its share of the total time (all spans sum to 1). */
	public record HeatPhase(float span, float target, float tolerance) {
	}

	public record QiWindow(float start, float end, float min) {
		public boolean contains(float progress) {
			return progress >= start && progress <= end;
		}
	}

	private final Identifier id;
	private final List<IngredientStack> ingredients;
	private final ItemStack result;
	private final int grade;
	private final int fireTier;
	private final int time;
	private final List<HeatPhase> heat;
	private final QiWindow qi;
	private final float volatility;

	public AlchemyRecipe(Identifier id, List<IngredientStack> ingredients, ItemStack result, int grade, int fireTier, int time,
						 List<HeatPhase> heat, QiWindow qi, float volatility) {
		this.id = id;
		this.ingredients = List.copyOf(ingredients);
		this.result = result;
		this.grade = grade;
		this.fireTier = fireTier;
		this.time = time;
		this.heat = List.copyOf(heat);
		this.qi = qi;
		this.volatility = volatility;
	}

	public List<IngredientStack> ingredientStacks() {
		return ingredients;
	}

	public int grade() {
		return grade;
	}

	public int fireTier() {
		return fireTier;
	}

	public int time() {
		return time;
	}

	public List<HeatPhase> heatPhases() {
		return heat;
	}

	public QiWindow qiWindow() {
		return qi;
	}

	public float volatility() {
		return volatility;
	}

	/** Target heat at {@code progress} 0..1. Phases blend into each other over a short ramp so the curve is continuous. */
	public float targetHeat(float progress) {
		progress = MathHelper.clamp(progress, 0.0F, 1.0F);
		float start = 0.0F;
		float prev = heat.get(0).target();
		for (HeatPhase phase : heat) {
			float end = start + phase.span();
			if (progress <= end || phase == heat.get(heat.size() - 1)) {
				float ramp = Math.min(0.08F, phase.span() * 0.4F);
				float t = ramp <= 0 ? 1.0F : MathHelper.clamp((progress - start) / ramp, 0.0F, 1.0F);
				return MathHelper.lerp(t, prev, phase.target());
			}
			prev = phase.target();
			start = end;
		}
		return prev;
	}

	public float tolerance(float progress) {
		progress = MathHelper.clamp(progress, 0.0F, 1.0F);
		float start = 0.0F;
		for (HeatPhase phase : heat) {
			float end = start + phase.span();
			if (progress <= end) return phase.tolerance();
			start = end;
		}
		return heat.get(heat.size() - 1).tolerance();
	}

	/** Herb counts in the furnace's herb slots. */
	public static Map<Item, Integer> countHerbs(Inventory inventory) {
		Map<Item, Integer> counts = new HashMap<>();
		for (int i = 0; i < HERB_SLOTS && i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (!stack.isEmpty()) counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		return counts;
	}

	/** Exactly the recipe's species, each in at least the required amount. */
	@Override
	public boolean matches(Inventory inventory, World world) {
		Map<Item, Integer> counts = countHerbs(inventory);
		if (counts.size() != ingredients.size()) return false;
		for (IngredientStack ing : ingredients) {
			if (counts.getOrDefault(ing.item(), 0) < ing.count()) return false;
		}
		return true;
	}

	/** Removes the recipe's herbs from the herb slots. */
	public void consume(Inventory inventory) {
		for (IngredientStack ing : ingredients) {
			int left = ing.count();
			for (int i = 0; i < HERB_SLOTS && left > 0; i++) {
				ItemStack stack = inventory.getStack(i);
				if (stack.isOf(ing.item())) {
					int take = Math.min(left, stack.getCount());
					stack.decrement(take);
					left -= take;
				}
			}
		}
	}

	@Override
	public ItemStack craft(Inventory inventory, DynamicRegistryManager registryManager) {
		return result.copy();
	}

	@Override
	public boolean fits(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getOutput(DynamicRegistryManager registryManager) {
		return result;
	}

	@Override
	public Identifier getId() {
		return id;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.ALCHEMY_SERIALIZER;
	}

	@Override
	public RecipeType<?> getType() {
		return ModRecipes.ALCHEMY_TYPE;
	}

	@Override
	public boolean isIgnoredInRecipeBook() {
		return true;
	}

	public static final class Serializer implements RecipeSerializer<AlchemyRecipe> {
		@Override
		public AlchemyRecipe read(Identifier id, JsonObject json) {
			List<IngredientStack> ingredients = new ArrayList<>();
			for (var el : JsonHelper.getArray(json, "ingredients")) {
				JsonObject o = el.getAsJsonObject();
				Identifier itemId = new Identifier(JsonHelper.getString(o, "item"));
				Item item = Registries.ITEM.getOrEmpty(itemId).orElseThrow(() -> new IllegalArgumentException("Unknown item " + itemId + " in " + id));
				ingredients.add(new IngredientStack(item, JsonHelper.getInt(o, "count", 1)));
			}
			JsonObject res = JsonHelper.getObject(json, "result");
			Identifier resId = new Identifier(JsonHelper.getString(res, "item"));
			Item resItem = Registries.ITEM.getOrEmpty(resId).orElseThrow(() -> new IllegalArgumentException("Unknown result " + resId + " in " + id));
			ItemStack result = new ItemStack(resItem, JsonHelper.getInt(res, "count", 1));
			List<HeatPhase> heat = new ArrayList<>();
			JsonArray arr = JsonHelper.getArray(json, "heat");
			float spanSum = 0.0F;
			for (var el : arr) {
				JsonObject o = el.getAsJsonObject();
				HeatPhase phase = new HeatPhase(JsonHelper.getFloat(o, "span"), JsonHelper.getFloat(o, "target"), JsonHelper.getFloat(o, "tol"));
				spanSum += phase.span();
				heat.add(phase);
			}
			if (heat.isEmpty()) throw new IllegalArgumentException("Recipe " + id + " has no heat phases");
			if (Math.abs(spanSum - 1.0F) > 0.02F) throw new IllegalArgumentException("Recipe " + id + " heat spans sum to " + spanSum);
			JsonObject q = JsonHelper.getObject(json, "qi");
			QiWindow qi = new QiWindow(JsonHelper.getFloat(q, "start"), JsonHelper.getFloat(q, "end"), JsonHelper.getFloat(q, "min"));
			return new AlchemyRecipe(id, ingredients, result, JsonHelper.getInt(json, "grade"), JsonHelper.getInt(json, "fire_tier", 1),
					JsonHelper.getInt(json, "time"), heat, qi, JsonHelper.getFloat(json, "volatility", 0.3F));
		}

		@Override
		public AlchemyRecipe read(Identifier id, PacketByteBuf buf) {
			List<IngredientStack> ingredients = buf.readList(b -> new IngredientStack(Registries.ITEM.get(b.readIdentifier()), b.readVarInt()));
			ItemStack result = buf.readItemStack();
			int grade = buf.readVarInt();
			int fireTier = buf.readVarInt();
			int time = buf.readVarInt();
			List<HeatPhase> heat = buf.readList(b -> new HeatPhase(b.readFloat(), b.readFloat(), b.readFloat()));
			QiWindow qi = new QiWindow(buf.readFloat(), buf.readFloat(), buf.readFloat());
			float volatility = buf.readFloat();
			return new AlchemyRecipe(id, ingredients, result, grade, fireTier, time, heat, qi, volatility);
		}

		@Override
		public void write(PacketByteBuf buf, AlchemyRecipe recipe) {
			buf.writeCollection(recipe.ingredients, (b, ing) -> {
				b.writeIdentifier(Registries.ITEM.getId(ing.item()));
				b.writeVarInt(ing.count());
			});
			buf.writeItemStack(recipe.result);
			buf.writeVarInt(recipe.grade);
			buf.writeVarInt(recipe.fireTier);
			buf.writeVarInt(recipe.time);
			buf.writeCollection(recipe.heat, (b, p) -> {
				b.writeFloat(p.span());
				b.writeFloat(p.target());
				b.writeFloat(p.tolerance());
			});
			buf.writeFloat(recipe.qi.start());
			buf.writeFloat(recipe.qi.end());
			buf.writeFloat(recipe.qi.min());
			buf.writeFloat(recipe.volatility);
		}
	}
}
