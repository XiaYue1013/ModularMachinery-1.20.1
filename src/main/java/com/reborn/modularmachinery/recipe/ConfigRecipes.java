package com.reborn.modularmachinery.recipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.reborn.modularmachinery.ModularMachineryReborn;
import com.reborn.modularmachinery.recipe.adapter.RecipeAdapterRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Recipes a pack author keeps as loose files: {@code config/modularmachinery/recipes/} or {@code config/modular_machinery_reborn/recipes/}.
 *
 * <p>Exposes these loose files as a server data pack. Automatically injects the required {@code "type": "modularmachinery:machine"}
 * for 1.12.2 legacy recipe files that do not declare a root {@code type}.
 */
public final class ConfigRecipes {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String DIRECTORY = "recipes";
    private static final String JSON_SUFFIX = ".json";
    /** The value every machine recipe's root {@code type} must carry. */
    public static final String RECIPE_TYPE = ModularMachineryReborn.MOD_ID + ":machine";

    private ConfigRecipes() {
    }

    /** Primary config recipe directory. */
    public static Path directory() {
        return FMLPaths.CONFIGDIR.get().resolve(ModularMachineryReborn.MOD_ID).resolve(DIRECTORY);
    }

    /** All candidate recipe directories (modularmachinery and modular_machinery_reborn). */
    public static List<Path> directories() {
        List<Path> dirs = new ArrayList<>();
        try {
            Path p1 = FMLPaths.CONFIGDIR.get().resolve(ModularMachineryReborn.MOD_ID).resolve(DIRECTORY);
            dirs.add(p1);
            Path p2 = FMLPaths.CONFIGDIR.get().resolve("modularmachinery").resolve(DIRECTORY);
            if (!dirs.contains(p2)) dirs.add(p2);
            Path p3 = FMLPaths.CONFIGDIR.get().resolve("modular_machinery_reborn").resolve(DIRECTORY);
            if (!dirs.contains(p3)) dirs.add(p3);
        } catch (Throwable ignored) {}
        return dirs;
    }

    /** Every recipe file, keyed the way the pack framework addresses server data resources. */
    public static Map<ResourceLocation, byte[]> readAll() {
        Map<ResourceLocation, byte[]> files = new LinkedHashMap<>();
        for (Path root : directories()) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                for (Path file : stream.filter(Files::isRegularFile).toList()) {
                    String name = file.getFileName().toString();
                    if (!name.endsWith(JSON_SUFFIX) || name.endsWith(".adapter.json")) {
                        continue;
                    }
                    String relative = root.relativize(file).toString().replace('\\', '/');
                    ResourceLocation location;
                    try {
                        location = new ResourceLocation(ModularMachineryReborn.MOD_ID, DIRECTORY + "/" + relative);
                    } catch (RuntimeException exception) {
                        LOGGER.warn("[{}] Skipping {} — its path is not a valid resource location",
                                ModularMachineryReborn.MOD_ID, file);
                        continue;
                    }
                    if (files.containsKey(location)) {
                        continue; // Primary config directory takes precedence
                    }
                    try {
                        byte[] rawData = Files.readAllBytes(file);
                        byte[] transformedData = validateAndTransform(file, rawData);
                        if (transformedData != null) {
                            files.put(location, transformedData);
                        }
                    } catch (IOException exception) {
                        LOGGER.error("[{}] Could not read recipe file {}", ModularMachineryReborn.MOD_ID, file, exception);
                    }
                }
            } catch (IOException exception) {
                LOGGER.error("[{}] Could not read recipe directory {}", ModularMachineryReborn.MOD_ID, root, exception);
            }
        }
        return files;
    }

    /**
     * Checks recipe JSON and auto-injects root {@code type} for legacy 1.12 recipes.
     */
    private static byte[] validateAndTransform(Path file, byte[] data) {
        JsonObject root;
        try {
            root = GSON.fromJson(new String(data, StandardCharsets.UTF_8), JsonObject.class);
        } catch (RuntimeException exception) {
            LOGGER.error("[{}] {} is not valid JSON and will not load", ModularMachineryReborn.MOD_ID, file, exception);
            return null;
        }
        if (root == null) {
            LOGGER.error("[{}] {} is empty and will not load", ModularMachineryReborn.MOD_ID, file);
            return null;
        }

        boolean modified = false;
        if (!root.has("type")) {
            root.addProperty("type", RECIPE_TYPE);
            modified = true;
            LOGGER.debug("[{}] Injected missing 'type': '{}' into legacy recipe file {}",
                    ModularMachineryReborn.MOD_ID, RECIPE_TYPE, file.getFileName());
        } else {
            String type = root.get("type").getAsString();
            if ("modularmachinery:machine".equals(type) || "modular_machinery_reborn:machine".equals(type)) {
                if (!RECIPE_TYPE.equals(type)) {
                    root.addProperty("type", RECIPE_TYPE);
                    modified = true;
                }
            }
        }

        return modified ? GSON.toJson(root).getBytes(StandardCharsets.UTF_8) : data;
    }

    /** Contributes the directory as a required server data pack, when it holds anything. */
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        int found = readAll().size();
        if (found == 0) {
            return;
        }
        LOGGER.info("[{}] Exposing {} recipe file(s) from config as a data pack",
                ModularMachineryReborn.MOD_ID, found);
        event.addRepositorySource(packs -> packs.accept(Pack.create(
                ModularMachineryReborn.MOD_ID + "_config_recipes",
                Component.literal("Modular Machinery: recipes from the config directory"),
                true,
                ConfigRecipePack::new,
                new Pack.Info(Component.literal("Recipes found in config/" + ModularMachineryReborn.MOD_ID
                        + "/recipes/"), 15, FeatureFlags.VANILLA_SET),
                PackType.SERVER_DATA,
                Pack.Position.TOP,
                true,
                PackSource.BUILT_IN)));
    }

    /**
     * Registers a reload listener to reload adapter accessors and generate adapted recipes.
     */
    public static void registerReloadListener(AddReloadListenerEvent event) {
        event.addListener((PreparableReloadListener) (stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
            return CompletableFuture.runAsync(() -> {
                RecipeAdapterRegistry.loadAccessors();
            }, backgroundExecutor).thenCompose(stage::wait).thenRunAsync(() -> {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    RecipeManager recipeManager = server.getRecipeManager();
                    List<Recipe<?>> all = new ArrayList<>(recipeManager.getRecipes());
                    List<MachineRecipe> adapted = RecipeAdapterRegistry.generateAdaptedRecipes(recipeManager);
                    boolean modified = false;
                    if (!adapted.isEmpty()) {
                        all.addAll(adapted);
                        modified = true;
                    }
                    if (net.minecraftforge.fml.ModList.get().isLoaded("crafttweaker")) {
                        List<MachineRecipe> ctRecipes = com.reborn.modularmachinery.compat.crafttweaker.CraftTweakerHelper.getRecipes();
                        if (!ctRecipes.isEmpty()) {
                            all.addAll(ctRecipes);
                            modified = true;
                        }
                    }
                    if (modified) {
                        recipeManager.replaceRecipes(all);
                        LOGGER.info("[{}] Injected dynamic & adapted recipes into RecipeManager",
                                ModularMachineryReborn.MOD_ID);
                    }
                }
            }, gameExecutor);
        });
    }
}
