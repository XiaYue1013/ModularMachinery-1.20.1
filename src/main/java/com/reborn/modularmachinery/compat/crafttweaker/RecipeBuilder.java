package com.reborn.modularmachinery.compat.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.modularmachinery.RecipeBuilder")
public class RecipeBuilder {

    @ZenCodeType.Method
    public static RecipePrimer newBuilder(String recipeRegistryName, String associatedMachineRegistryName, int processingTickTime) {
        return newBuilder(recipeRegistryName, associatedMachineRegistryName, processingTickTime, 0);
    }

    @ZenCodeType.Method
    public static RecipePrimer newBuilder(String recipeRegistryName, String associatedMachineRegistryName, int processingTickTime, int sortingPriority) {
        return newBuilder(recipeRegistryName, associatedMachineRegistryName, processingTickTime, sortingPriority, false);
    }

    @ZenCodeType.Method
    public static RecipePrimer newBuilder(String recipeRegistryName, String associatedMachineRegistryName, int processingTickTime, int sortingPriority, boolean cancelIfPerTickFails) {
        return new RecipePrimer(recipeRegistryName, associatedMachineRegistryName, processingTickTime, sortingPriority, cancelIfPerTickFails);
    }
}
