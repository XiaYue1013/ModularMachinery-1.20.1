package com.reborn.modularmachinery.recipe.adapter;

import com.reborn.modularmachinery.ModularMachineryReborn;
import com.reborn.modularmachinery.recipe.MachineRecipe;
import com.reborn.modularmachinery.recipe.MachineRequirement;
import com.reborn.modularmachinery.recipe.ModRecipeTypes;
import com.reborn.modularmachinery.recipe.RecipeModifier;
import com.reborn.modularmachinery.recipe.RecipeModifiers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Copies recipes from a source modular machine to a target machine, applying modifiers and additional requirements.
 */
public class DynamicMachineRecipeAdapter extends RecipeAdapter {

    private final ResourceLocation sourceMachine;

    public DynamicMachineRecipeAdapter(ResourceLocation registryName, ResourceLocation sourceMachine) {
        super(registryName);
        this.sourceMachine = sourceMachine;
    }

    public ResourceLocation getSourceMachine() {
        return sourceMachine;
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
        String targetPath = owningMachineName.getNamespace() + "." + owningMachineName.getPath();

        for (MachineRecipe original : recipeManager.getAllRecipesFor(ModRecipeTypes.MACHINE.get())) {
            if (!this.sourceMachine.equals(original.machineId())) {
                continue;
            }

            ResourceLocation newId = new ResourceLocation(ModularMachineryReborn.MOD_ID,
                    original.getId().getPath() + ".copy." + targetPath);

            int newTime = original.recipeTime();
            if (mods != null && !mods.isEmpty()) {
                newTime = Math.max(1, (int) Math.round(mods.applyDuration(newTime)));
            }

            List<MachineRequirement> newReqs = new ArrayList<>(original.requirements());
            if (additionalRequirements != null) {
                newReqs.addAll(additionalRequirements);
            }

            MachineRecipe adapted = new MachineRecipe(
                    newId,
                    owningMachineName.toString(),
                    original.registryName() + "_adapted_" + targetPath,
                    newTime,
                    newReqs,
                    original.maxParallelism(),
                    original.priority(),
                    original.tooltips(),
                    original.cancelIfPerTickFails()
            );
            results.add(adapted);
        }

        return results;
    }
}
