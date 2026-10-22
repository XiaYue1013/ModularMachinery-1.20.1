package com.reborn.modularmachinery.recipe.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.reborn.modularmachinery.ModularMachineryReborn;
import com.reborn.modularmachinery.recipe.IOType;
import com.reborn.modularmachinery.recipe.MachineRequirement;
import com.reborn.modularmachinery.recipe.MachineRequirementParser;
import com.reborn.modularmachinery.recipe.RecipeModifier;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses and holds an adapter declaration from a *.adapter.json file.
 */
public class RecipeAdapterAccessor {

    private final ResourceLocation owningMachine;
    private final ResourceLocation adapterKey;
    private final List<RecipeModifier> modifiers;
    private final List<MachineRequirement> additionalRequirements;

    public RecipeAdapterAccessor(ResourceLocation owningMachine, ResourceLocation adapterKey,
                                 List<RecipeModifier> modifiers, List<MachineRequirement> additionalRequirements) {
        this.owningMachine = owningMachine;
        this.adapterKey = adapterKey;
        this.modifiers = List.copyOf(modifiers);
        this.additionalRequirements = List.copyOf(additionalRequirements);
    }

    public ResourceLocation owningMachine() {
        return owningMachine;
    }

    public ResourceLocation adapterKey() {
        return adapterKey;
    }

    public List<RecipeModifier> modifiers() {
        return modifiers;
    }

    public List<MachineRequirement> additionalRequirements() {
        return additionalRequirements;
    }

    public static RecipeAdapterAccessor fromJson(JsonObject json, String source) {
        if (!json.has("machine")) {
            throw new JsonParseException("Missing 'machine' in adapter file " + source);
        }
        if (!json.has("adapter")) {
            throw new JsonParseException("Missing 'adapter' in adapter file " + source);
        }

        String machineStr = json.get("machine").getAsString();
        ResourceLocation machineId = machineStr.contains(":") ? new ResourceLocation(machineStr)
                : new ResourceLocation(ModularMachineryReborn.MOD_ID, machineStr);

        String adapterStr = json.get("adapter").getAsString();
        ResourceLocation adapterKey = adapterStr.contains(":") ? new ResourceLocation(adapterStr)
                : new ResourceLocation(ModularMachineryReborn.MOD_ID, adapterStr);

        List<RecipeModifier> modifiers = new ArrayList<>();
        if (json.has("modifiers") && json.get("modifiers").isJsonArray()) {
            JsonArray modArray = json.getAsJsonArray("modifiers");
            for (JsonElement el : modArray) {
                if (el.isJsonObject()) {
                    modifiers.add(parseModifier(el.getAsJsonObject(), source));
                }
            }
        }

        List<MachineRequirement> requirements = new ArrayList<>();
        if (json.has("requirements") && json.get("requirements").isJsonArray()) {
            JsonArray reqArray = json.getAsJsonArray("requirements");
            for (JsonElement el : reqArray) {
                if (el.isJsonObject()) {
                    requirements.add(MachineRequirementParser.parse(el.getAsJsonObject(), new ResourceLocation(source)));
                }
            }
        }

        return new RecipeAdapterAccessor(machineId, adapterKey, modifiers, requirements);
    }

    private static RecipeModifier parseModifier(JsonObject obj, String source) {
        String targetStr = obj.has("target") ? obj.get("target").getAsString() : "duration";
        if (targetStr.contains(":")) {
            targetStr = targetStr.substring(targetStr.indexOf(':') + 1);
        }

        RecipeModifier.Target target = switch (targetStr.toLowerCase()) {
            case "item" -> RecipeModifier.Target.ITEM;
            case "fluid" -> RecipeModifier.Target.FLUID;
            case "energy" -> RecipeModifier.Target.ENERGY;
            default -> RecipeModifier.Target.DURATION;
        };

        IOType io = null;
        if (obj.has("io")) {
            String ioStr = obj.get("io").getAsString();
            if ("input".equalsIgnoreCase(ioStr)) io = IOType.INPUT;
            else if ("output".equalsIgnoreCase(ioStr)) io = IOType.OUTPUT;
        }

        int operation = obj.has("operation") ? obj.get("operation").getAsInt() : 1;
        float value = obj.has("multiplier") ? obj.get("multiplier").getAsFloat()
                : (obj.has("value") ? obj.get("value").getAsFloat() : 1.0F);

        RecipeModifier.Operation op = operation == 0 ? RecipeModifier.Operation.ADD : RecipeModifier.Operation.MULTIPLY;
        boolean affectsChance = obj.has("chance") && obj.get("chance").getAsBoolean();
        return new RecipeModifier(target, io, value, op, affectsChance);
    }
}
