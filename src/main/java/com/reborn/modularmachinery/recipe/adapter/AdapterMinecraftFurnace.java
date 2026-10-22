package com.reborn.modularmachinery.recipe.adapter;

import com.reborn.modularmachinery.ModularMachineryReborn;
import com.reborn.modularmachinery.recipe.IOType;
import com.reborn.modularmachinery.recipe.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Adapts Minecraft smelting recipes into Modular Machinery recipes.
 * Mirrors hellfirepvp.modularmachinery.common.crafting.adapter.AdapterMinecraftFurnace.
 */
public class AdapterMinecraftFurnace extends RecipeAdapter {

    public AdapterMinecraftFurnace() {
        super(new ResourceLocation("minecraft", "furnace"));
    }

    @Override
    public Collection<MachineRecipe> createRecipesFor(
            RecipeManager recipeManager,
            ResourceLocation owningMachineName,
            List<RecipeModifier> modifiers,
            List<MachineRequirement> additionalRequirements) {

        List<MachineRecipe> results = new ArrayList<>();
        if (recipeManager == null) {
            return results;
        }

        RecipeModifiers mods = RecipeModifiers.of(modifiers);
        int inc = 0;

        for (SmeltingRecipe smelting : recipeManager.getAllRecipesFor(RecipeType.SMELTING)) {
            if (smelting.getIngredients().isEmpty()) {
                continue;
            }
            Ingredient inputIngredient = smelting.getIngredients().get(0);
            if (inputIngredient.isEmpty()) {
                continue;
            }

            ItemStack outputStack = smelting.getResultItem(RegistryAccess.EMPTY);
            if (outputStack.isEmpty()) {
                continue;
            }

            int baseTime = 120;
            int tickTime = Math.max(1, mods != null ? (int) Math.round(mods.applyDuration(baseTime)) : baseTime);

            int inCount = 1;
            int inEnergy = 20;

            List<MachineRequirement> requirements = new ArrayList<>();

            // Energy input
            int energyPerTick = Math.max(0, mods != null ? (int) Math.round(mods.apply(RecipeModifier.Target.ENERGY, IOType.INPUT, inEnergy, false)) : inEnergy);
            if (energyPerTick > 0) {
                requirements.add(new EnergyRequirement(IOType.INPUT, energyPerTick));
            }

            // Item input
            int modifiedInCount = Math.max(1, mods != null ? (int) Math.round(mods.apply(RecipeModifier.Target.ITEM, IOType.INPUT, inCount, false)) : inCount);
            requirements.add(ItemRequirement.input(inputIngredient, modifiedInCount, null, null));

            // Item output
            ItemStack outCopy = outputStack.copy();
            int modifiedOutCount = Math.max(1, mods != null ? (int) Math.round(mods.apply(RecipeModifier.Target.ITEM, IOType.OUTPUT, outCopy.getCount(), false)) : outCopy.getCount());
            outCopy.setCount(modifiedOutCount);
            requirements.add(ItemRequirement.output(outCopy, modifiedOutCount, modifiedOutCount, 1.0F, null));

            // Additional requirements from adapter config
            if (additionalRequirements != null) {
                requirements.addAll(additionalRequirements);
            }

            ResourceLocation recipeId = new ResourceLocation(ModularMachineryReborn.MOD_ID,
                    "adapter.furnace." + owningMachineName.getPath() + "." + inc++);

            MachineRecipe recipe = new MachineRecipe(
                    recipeId,
                    owningMachineName.toString(),
                    recipeId.getPath(),
                    tickTime,
                    requirements,
                    MachineRecipe.MACHINE_LIMIT,
                    0,
                    List.of(),
                    false
            );
            results.add(recipe);
        }

        return results;
    }
}
