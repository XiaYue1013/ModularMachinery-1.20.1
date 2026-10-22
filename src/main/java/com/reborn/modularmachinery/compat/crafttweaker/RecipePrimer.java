package com.reborn.modularmachinery.compat.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.reborn.modularmachinery.ModularMachineryReborn;
import com.reborn.modularmachinery.recipe.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import org.openzen.zencode.java.ZenCodeType;

import java.util.ArrayList;
import java.util.List;

@ZenRegister
@ZenCodeType.Name("mods.modularmachinery.RecipePrimer")
public class RecipePrimer {

    private final String recipeRegistryName;
    private final String associatedMachine;
    private final int tickTime;
    private final int priority;
    private final boolean cancelIfPerTickFails;

    private int maxParallelism = 0;
    private final List<String> tooltips = new ArrayList<>();
    private final List<MachineRequirement> requirements = new ArrayList<>();

    private MachineRequirement lastRequirement = null;

    public RecipePrimer(String recipeRegistryName, String associatedMachine, int tickTime, int priority, boolean cancelIfPerTickFails) {
        this.recipeRegistryName = recipeRegistryName;
        this.associatedMachine = associatedMachine;
        this.tickTime = Math.max(1, tickTime);
        this.priority = priority;
        this.cancelIfPerTickFails = cancelIfPerTickFails;
    }

    @ZenCodeType.Method
    public RecipePrimer setChance(float chance) {
        if (lastRequirement instanceof ItemRequirement itemReq) {
            // Chance is handled during item input/output
        }
        return this;
    }

    @ZenCodeType.Method
    public RecipePrimer addTooltip(String tooltip) {
        if (tooltip != null) {
            this.tooltips.add(tooltip);
        }
        return this;
    }

    @ZenCodeType.Method
    public RecipePrimer setMaxParallelism(int maxParallelism) {
        this.maxParallelism = Math.max(0, maxParallelism);
        return this;
    }

    // Energy
    @ZenCodeType.Method
    public RecipePrimer addEnergyPerTickInput(int perTick) {
        EnergyRequirement req = new EnergyRequirement(IOType.INPUT, perTick);
        this.requirements.add(req);
        this.lastRequirement = req;
        return this;
    }

    @ZenCodeType.Method
    public RecipePrimer addEnergyPerTickOutput(int perTick) {
        EnergyRequirement req = new EnergyRequirement(IOType.OUTPUT, perTick);
        this.requirements.add(req);
        this.lastRequirement = req;
        return this;
    }

    // Fluid
    @ZenCodeType.Method
    public RecipePrimer addFluidInput(IFluidStack stack) {
        FluidStack fs = stack.getInternal();
        FluidRequirement req = new FluidRequirement(IOType.INPUT, fs, fs.getAmount(), 1.0F, false);
        this.requirements.add(req);
        this.lastRequirement = req;
        return this;
    }

    @ZenCodeType.Method
    public RecipePrimer addFluidOutput(IFluidStack stack) {
        FluidStack fs = stack.getInternal();
        FluidRequirement req = new FluidRequirement(IOType.OUTPUT, fs, fs.getAmount(), 1.0F, false);
        this.requirements.add(req);
        this.lastRequirement = req;
        return this;
    }

    // Item
    @ZenCodeType.Method
    public RecipePrimer addItemInput(IItemStack stack) {
        ItemStack is = stack.getInternal();
        NbtPredicate predicate = is.hasTag() ? NbtPredicate.fromCompoundTag(is.getTag()) : null;
        ItemRequirement req = ItemRequirement.input(Ingredient.of(is), is.getCount(), 1.0F, predicate, is.getTag());
        this.requirements.add(req);
        this.lastRequirement = req;
        return this;
    }

    @ZenCodeType.Method
    public RecipePrimer addItemInput(IIngredient ingredient) {
        return addItemInput(ingredient, 1);
    }

    @ZenCodeType.Method
    public RecipePrimer addItemInput(IIngredient ingredient, int amount) {
        Ingredient ing = ingredient.asVanillaIngredient();
        ItemRequirement req = ItemRequirement.input(ing, amount, null, null);
        this.requirements.add(req);
        this.lastRequirement = req;
        return this;
    }

    @ZenCodeType.Method
    public RecipePrimer addItemOutput(IItemStack stack) {
        return addItemOutput(stack, 1.0F);
    }

    @ZenCodeType.Method
    public RecipePrimer addItemOutput(IItemStack stack, float chance) {
        ItemStack is = stack.getInternal();
        ItemRequirement req = ItemRequirement.output(is, is.getCount(), is.getCount(), chance, null);
        this.requirements.add(req);
        this.lastRequirement = req;
        return this;
    }

    @ZenCodeType.Method
    public void build() {
        ResourceLocation id = this.recipeRegistryName.contains(":")
                ? ResourceLocation.tryParse(this.recipeRegistryName)
                : new ResourceLocation(ModularMachineryReborn.MOD_ID, this.recipeRegistryName);
        if (id == null) {
            ModularMachineryReborn.LOGGER.error("[ModularMachinery] Malformed recipe name: {}", this.recipeRegistryName);
            return;
        }

        MachineRecipe recipe = new MachineRecipe(
                id,
                this.associatedMachine,
                this.recipeRegistryName,
                this.tickTime,
                this.requirements,
                this.maxParallelism,
                this.priority,
                this.tooltips,
                this.cancelIfPerTickFails
        );

        CraftTweakerHelper.addRecipe(recipe);
    }
}
