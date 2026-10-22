package com.reborn.modularmachinery.recipe.adapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.reborn.modularmachinery.ModularMachineryReborn;
import com.reborn.modularmachinery.machine.MachineRegistry;
import com.reborn.modularmachinery.recipe.ConfigRecipes;
import com.reborn.modularmachinery.recipe.MachineRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Registry and loader for Recipe Adapters.
 */
public final class RecipeAdapterRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private static final Map<ResourceLocation, RecipeAdapter> ADAPTERS = new LinkedHashMap<>();
    private static final List<RecipeAdapterAccessor> LOADED_ACCESSORS = new ArrayList<>();

    static {
        registerAdapter(new AdapterMinecraftFurnace());
    }

    public static void registerAdapter(RecipeAdapter adapter) {
        ADAPTERS.put(adapter.getRegistryName(), adapter);
    }

    public static RecipeAdapter getAdapter(ResourceLocation key) {
        return ADAPTERS.get(key);
    }

    /**
     * Scans config/modularmachinery/recipes/ for *.adapter.json files.
     */
    public static void loadAccessors() {
        LOADED_ACCESSORS.clear();
        Path root;
        try {
            root = ConfigRecipes.directory();
        } catch (Throwable t) {
            return;
        }
        if (!Files.isDirectory(root)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(root)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".adapter.json")) {
                    continue;
                }
                try {
                    String jsonStr = Files.readString(file, StandardCharsets.UTF_8);
                    JsonObject json = GSON.fromJson(jsonStr, JsonObject.class);
                    if (json != null) {
                        RecipeAdapterAccessor accessor = RecipeAdapterAccessor.fromJson(json, name);
                        LOADED_ACCESSORS.add(accessor);
                        LOGGER.info("[{}] Loaded recipe adapter {} for machine {}",
                                ModularMachineryReborn.MOD_ID, accessor.adapterKey(), accessor.owningMachine());
                    }
                } catch (Exception ex) {
                    LOGGER.error("[{}] Failed to load adapter file {}", ModularMachineryReborn.MOD_ID, file, ex);
                }
            }
        } catch (IOException e) {
            LOGGER.error("[{}] Error reading adapter files from {}", ModularMachineryReborn.MOD_ID, root, e);
        }
    }

    /**
     * Generates all adapted recipes and applies them.
     */
    public static List<MachineRecipe> generateAdaptedRecipes(RecipeManager recipeManager) {
        List<MachineRecipe> generated = new ArrayList<>();
        if (recipeManager == null) {
            return generated;
        }

        // Dynamically register adapters for all defined machines so any machine can adapt another machine
        for (com.reborn.modularmachinery.machine.MachineDefinition def : MachineRegistry.all()) {
            ResourceLocation machineId = def.id();
            if (!ADAPTERS.containsKey(machineId)) {
                ADAPTERS.put(machineId, new DynamicMachineRecipeAdapter(machineId, machineId));
            }
        }

        for (RecipeAdapterAccessor accessor : LOADED_ACCESSORS) {
            RecipeAdapter adapter = ADAPTERS.get(accessor.adapterKey());
            if (adapter == null) {
                LOGGER.warn("[{}] Unknown adapter '{}' requested by machine '{}'",
                        ModularMachineryReborn.MOD_ID, accessor.adapterKey(), accessor.owningMachine());
                continue;
            }

            Collection<MachineRecipe> recipes = adapter.createRecipesFor(
                    recipeManager,
                    accessor.owningMachine(),
                    accessor.modifiers(),
                    accessor.additionalRequirements()
            );
            if (recipes != null && !recipes.isEmpty()) {
                generated.addAll(recipes);
                LOGGER.info("[{}] Adapter '{}' generated {} recipes for '{}'",
                        ModularMachineryReborn.MOD_ID, accessor.adapterKey(), recipes.size(), accessor.owningMachine());
            }
        }

        return generated;
    }
}
