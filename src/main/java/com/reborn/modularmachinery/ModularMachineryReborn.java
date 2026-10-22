package com.reborn.modularmachinery;

import com.mojang.logging.LogUtils;
import com.reborn.modularmachinery.block.ModBlocks;
import com.reborn.modularmachinery.recipe.ModRecipeSerializers;
import com.reborn.modularmachinery.recipe.ModRecipeTypes;
import com.reborn.modularmachinery.machine.MachineLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.reborn.modularmachinery.menu.ModMenus;
import com.reborn.modularmachinery.item.ModItems;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import org.slf4j.Logger;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@Mod(ModularMachineryReborn.MOD_ID)
public final class ModularMachineryReborn {
    public static final String MOD_ID = "modularmachinery";
    public static final String DISPLAY_NAME = "Modular Machinery";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ModularMachineryReborn() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.ITEMS.register(bus);
        ModItems.ITEMS.register(bus);
        ModBlocks.TABS.register(bus);
        ModRecipeTypes.TYPES.register(bus);
        ModRecipeSerializers.SERIALIZERS.register(bus);
        ModBlocks.BLOCK_ENTITIES.register(bus);
        ModMenus.MENUS.register(bus);

        // ModularController compatibility namespace
        ModBlocks.registerMocNamespace(bus);

        // Mod config
        com.reborn.modularmachinery.config.ModConfig.register();

        // Custom networking channel
        com.reborn.modularmachinery.network.ModNetwork.register();

        // Data reload listeners: Machines, Upgrades, Config Recipes & Adapters
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> {
            event.addListener(new MachineLoader());
            event.addListener(new com.reborn.modularmachinery.upgrade.UpgradeLoader());
            com.reborn.modularmachinery.recipe.ConfigRecipes.registerReloadListener(event);
        });

        // Controller resource packs
        bus.addListener(com.reborn.modularmachinery.block.GeneratedPacks::onAddPackFinders);

        // Mod compat setup
        bus.addListener(com.reborn.modularmachinery.compat.ModCompat::onCommonSetup);
        MinecraftForge.EVENT_BUS.addListener(com.reborn.modularmachinery.compat.ModCompat::onRegisterCapabilities);

        // Exposes config/modularmachinery/recipes/ as a data pack
        bus.addListener(com.reborn.modularmachinery.recipe.ConfigRecipes::onAddPackFinders);

        // Commands
        MinecraftForge.EVENT_BUS.addListener(com.reborn.modularmachinery.command.BlueprintCommand::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(com.reborn.modularmachinery.command.DiagnoseCommand::onRegisterCommands);

        // Player session selections
        MinecraftForge.EVENT_BUS.addListener(com.reborn.modularmachinery.selection.ServerSelections::onPlayerLoggedOut);

        // Machine sync to client
        MinecraftForge.EVENT_BUS.addListener(com.reborn.modularmachinery.network.MachineDefinitionSync::onDatapackSync);

        // Backward compatibility remapping for worlds saved with 'modular_machinery_reborn' mod id
        MinecraftForge.EVENT_BUS.addListener((MissingMappingsEvent event) -> {
            for (MissingMappingsEvent.Mapping<Block> mapping : event.getMappings(ForgeRegistries.Keys.BLOCKS, "modular_machinery_reborn")) {
                Block target = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(MOD_ID, mapping.getKey().getPath()));
                if (target != null && target != Blocks.AIR) {
                    mapping.remap(target);
                }
            }
            for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, "modular_machinery_reborn")) {
                Item target = ForgeRegistries.ITEMS.getValue(new ResourceLocation(MOD_ID, mapping.getKey().getPath()));
                if (target != null && target != Items.AIR) {
                    mapping.remap(target);
                }
            }
        });

        // Client hooks
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> MinecraftForge.EVENT_BUS.addListener(
                        com.reborn.modularmachinery.client.ClientMachines::onLoggingOut));

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(com.reborn.modularmachinery.client.ClientSetup::onClientSetup));

        String version = ModList.get().getModContainerById(MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
        LOGGER.info("[{}] {} {} loaded.", MOD_ID, DISPLAY_NAME, version);
        LOGGER.info("[{}] KubeJS integration is optional; when KubeJS is installed, kubejs.plugins.txt registers the machine recipe schema.", MOD_ID);
    }
}
