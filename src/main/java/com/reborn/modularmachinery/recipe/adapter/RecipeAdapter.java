package com.reborn.modularmachinery.recipe.adapter;

import com.reborn.modularmachinery.recipe.MachineRecipe;
import com.reborn.modularmachinery.recipe.MachineRequirement;
import com.reborn.modularmachinery.recipe.RecipeModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Collection;
import java.util.List;

/**
 * Base class for Recipe Adapters, mirroring original HellFirePvP 1.12.2 RecipeAdapter.
 * Allows importing recipes from external recipe types (like vanilla smelting) or other machines,
 * applying modifiers and additional requirements.
 */
public abstract class RecipeAdapter {

    private final ResourceLocation registryName;

    public RecipeAdapter(ResourceLocation registryName) {
        this.registryName = registryName;
    }

    public ResourceLocation getRegistryName() {
        return registryName;
    }

    public abstract Collection<MachineRecipe> createRecipesFor(
            RecipeManager recipeManager,
            ResourceLocation owningMachineName,
            List<RecipeModifier> modifiers,
            List<MachineRequirement> additionalRequirements);
}
