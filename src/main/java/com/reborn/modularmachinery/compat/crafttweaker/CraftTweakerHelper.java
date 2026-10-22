package com.reborn.modularmachinery.compat.crafttweaker;

import com.reborn.modularmachinery.ModularMachineryReborn;
import com.reborn.modularmachinery.recipe.MachineRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CraftTweakerHelper {

    private static final List<MachineRecipe> RECIPES = new CopyOnWriteArrayList<>();

    private CraftTweakerHelper() {
    }

    public static void addRecipe(MachineRecipe recipe) {
        RECIPES.add(recipe);
        ModularMachineryReborn.LOGGER.info("[ModularMachinery] Queued CraftTweaker recipe: {}", recipe.getId());

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            RecipeManager recipeManager = server.getRecipeManager();
            List<Recipe<?>> all = new ArrayList<>(recipeManager.getRecipes());
            all.add(recipe);
            recipeManager.replaceRecipes(all);
            ModularMachineryReborn.LOGGER.info("[ModularMachinery] Injected CraftTweaker recipe into RecipeManager: {}", recipe.getId());
        }
    }

    public static List<MachineRecipe> getRecipes() {
        return RECIPES;
    }
}
