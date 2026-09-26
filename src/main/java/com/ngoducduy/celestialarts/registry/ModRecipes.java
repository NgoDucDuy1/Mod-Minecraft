package com.ngoducduy.celestialarts.registry;

import com.ngoducduy.celestialarts.CelestialArts;
import com.ngoducduy.celestialarts.alchemy.AlchemyRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModRecipes {
	public static final Identifier ALCHEMY_ID = CelestialArts.id("alchemy");
	public static final RecipeType<AlchemyRecipe> ALCHEMY_TYPE = Registry.register(Registries.RECIPE_TYPE, ALCHEMY_ID, new RecipeType<AlchemyRecipe>() {
		@Override
		public String toString() {
			return ALCHEMY_ID.toString();
		}
	});
	public static final RecipeSerializer<AlchemyRecipe> ALCHEMY_SERIALIZER = Registry.register(Registries.RECIPE_SERIALIZER, ALCHEMY_ID, new AlchemyRecipe.Serializer());

	private ModRecipes() {
	}

	public static void register() {
	}
}
