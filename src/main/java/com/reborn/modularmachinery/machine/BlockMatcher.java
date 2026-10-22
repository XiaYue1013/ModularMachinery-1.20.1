package com.reborn.modularmachinery.machine;

import com.google.gson.JsonParseException;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One accepted block or tag of a machine structure.
 *
 * <p>Supports:
 * <ul>
 *   <li>Block + properties: {@code namespace:block[key=value,...]} or {@code namespace:block}</li>
 *   <li>Block tags: {@code #namespace:tag_name} or {@code tag:namespace:tag_name}</li>
 *   <li>Legacy 1.12.2 {@code @meta} syntax: automatically converted for casings, hatches, buses and vanilla blocks.</li>
 *   <li>Namespace alias compatibility between {@code modularmachinery} and {@code modular_machinery_reborn}.</li>
 * </ul>
 */
public final class BlockMatcher {

    private final @Nullable Block block;
    private final Map<Property<?>, Comparable<?>> required;
    private final @Nullable TagKey<Block> tag;
    private final String descriptor;

    private BlockMatcher(Block block, Map<Property<?>, Comparable<?>> required, TagKey<Block> tag, String descriptor) {
        this.block = block;
        this.required = required != null ? Collections.unmodifiableMap(required) : Collections.emptyMap();
        this.tag = tag;
        this.descriptor = descriptor;
    }

    public static BlockMatcher parse(String raw) {
        if (raw == null) {
            throw new JsonParseException("Null block descriptor in 'elements'");
        }
        String descriptor = raw.trim();
        if (descriptor.isEmpty()) {
            throw new JsonParseException("Empty block descriptor in 'elements'");
        }

        // Tag syntax: #tag:name or tag:namespace:path or #namespace:path
        if (descriptor.startsWith("#") || descriptor.startsWith("tag:")) {
            String tagStr = descriptor.startsWith("#") ? descriptor.substring(1).trim() : descriptor.substring(4).trim();
            if (tagStr.startsWith(":")) {
                tagStr = tagStr.substring(1).trim();
            }
            ResourceLocation tagLoc = ResourceLocation.tryParse(tagStr);
            if (tagLoc == null) {
                throw new JsonParseException("Malformed block tag '" + tagStr + "' in descriptor '" + raw + "'");
            }
            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagLoc);
            return new BlockMatcher(null, Collections.emptyMap(), tagKey, descriptor);
        }

        // Legacy 1.12.2 @meta conversion
        if (descriptor.indexOf('@') >= 0) {
            descriptor = convertLegacyMeta(descriptor);
        }

        String idPart = descriptor;
        String propsPart = null;
        int bracket = descriptor.indexOf('[');
        if (bracket >= 0) {
            if (!descriptor.endsWith("]")) {
                throw new JsonParseException("Unclosed '[' in block descriptor '" + raw + "'");
            }
            idPart = descriptor.substring(0, bracket).trim();
            propsPart = descriptor.substring(bracket + 1, descriptor.length() - 1);
        }

        ResourceLocation id = ResourceLocation.tryParse(idPart);
        if (id == null) {
            throw new JsonParseException("Malformed block id '" + idPart + "' in descriptor '" + raw + "'");
        }

        Block block = ForgeRegistries.BLOCKS.getValue(id);
        if (block == null || block == Blocks.AIR) {
            // Check alias between modularmachinery and modular_machinery_reborn
            if ("modularmachinery".equals(id.getNamespace())) {
                ResourceLocation altId = new ResourceLocation("modular_machinery_reborn", id.getPath());
                block = ForgeRegistries.BLOCKS.getValue(altId);
            } else if ("modular_machinery_reborn".equals(id.getNamespace())) {
                ResourceLocation altId = new ResourceLocation("modularmachinery", id.getPath());
                block = ForgeRegistries.BLOCKS.getValue(altId);
            }
        }

        if (block == null || block == Blocks.AIR) {
            throw new JsonParseException("Unknown block '" + id + "' in descriptor '" + raw + "'");
        }

        Map<Property<?>, Comparable<?>> required = new LinkedHashMap<>();
        if (propsPart != null && !propsPart.isBlank()) {
            for (String pair : propsPart.split(",")) {
                String[] split = pair.split("=", 2);
                if (split.length != 2) {
                    throw new JsonParseException("Expected property=value in '" + pair.trim()
                            + "' of descriptor '" + raw + "'");
                }
                String key = split[0].trim();
                String value = split[1].trim();
                Property<?> property = block.getStateDefinition().getProperty(key);
                if (property == null) {
                    throw new JsonParseException("Block '" + id + "' has no blockstate property '" + key + "'");
                }
                Comparable<?> parsed = property.getValue(value).orElseThrow(() -> new JsonParseException(
                        "Block '" + id + "' property '" + key + "' has no value '" + value + "'"));
                required.put(property, parsed);
            }
        }
        return new BlockMatcher(block, required, null, descriptor);
    }

    private static String convertLegacyMeta(String descriptor) {
        int atIndex = descriptor.indexOf('@');
        if (atIndex < 0) return descriptor;

        String idPart = descriptor.substring(0, atIndex).trim();
        String metaStr = descriptor.substring(atIndex + 1).trim();
        int meta = 0;
        try {
            meta = Integer.parseInt(metaStr);
        } catch (NumberFormatException ignored) {}

        String path = idPart;
        String namespace = "modularmachinery";
        int colon = idPart.indexOf(':');
        if (colon >= 0) {
            namespace = idPart.substring(0, colon);
            path = idPart.substring(colon + 1);
        }

        if (path.equals("blockcasing")) {
            String[] casings = {"plain", "vent", "firebox", "gearbox", "reinforced", "circuitry"};
            String type = (meta >= 0 && meta < casings.length) ? casings[meta] : "plain";
            return namespace + ":" + path + "[casing=" + type + "]";
        }

        if (path.equals("blockinputbus") || path.equals("blockoutputbus")) {
            String[] busSizes = {"tiny", "small", "normal", "reinforced", "big", "huge", "ludicrous"};
            String size = (meta >= 0 && meta < busSizes.length) ? busSizes[meta] : "tiny";
            return namespace + ":" + path + "[size=" + size + "]";
        }

        if (path.equals("blockfluidinputhatch") || path.equals("blockfluidoutputhatch")) {
            String[] fluidSizes = {"tiny", "small", "normal", "reinforced", "big", "huge", "ludicrous", "vacuum"};
            String size = (meta >= 0 && meta < fluidSizes.length) ? fluidSizes[meta] : "tiny";
            return namespace + ":" + path + "[size=" + size + "]";
        }

        if (path.equals("blockenergyinputhatch") || path.equals("blockenergyoutputhatch")) {
            String[] energySizes = {"tiny", "small", "normal", "reinforced", "big", "huge", "ludicrous", "ultimate"};
            String size = (meta >= 0 && meta < energySizes.length) ? energySizes[meta] : "tiny";
            return namespace + ":" + path + "[size=" + size + "]";
        }

        // Vanilla conversions for common 1.12 metadata blocks
        if (idPart.equals("minecraft:stone")) {
            String[] stoneVariants = {"stone", "granite", "polished_granite", "diorite", "polished_diorite", "andesite", "polished_andesite"};
            if (meta >= 0 && meta < stoneVariants.length) {
                return "minecraft:" + stoneVariants[meta];
            }
        }
        if (idPart.equals("minecraft:planks")) {
            String[] plankVariants = {"oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks", "dark_oak_planks"};
            if (meta >= 0 && meta < plankVariants.length) {
                return "minecraft:" + plankVariants[meta];
            }
        }
        if (idPart.equals("minecraft:wool")) {
            String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
            if (meta >= 0 && meta < colors.length) {
                return "minecraft:" + colors[meta] + "_wool";
            }
        }
        if (idPart.equals("minecraft:stained_glass")) {
            String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
            if (meta >= 0 && meta < colors.length) {
                return "minecraft:" + colors[meta] + "_stained_glass";
            }
        }
        if (idPart.equals("minecraft:stained_hardened_clay")) {
            String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
            if (meta >= 0 && meta < colors.length) {
                return "minecraft:" + colors[meta] + "_terracotta";
            }
        }
        if (idPart.equals("minecraft:concrete")) {
            String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
            if (meta >= 0 && meta < colors.length) {
                return "minecraft:" + colors[meta] + "_concrete";
            }
        }

        // Fallback: strip meta if no translation exists
        return idPart;
    }

    public boolean matches(BlockState state) {
        if (this.tag != null) {
            return state.is(this.tag);
        }
        if (this.block == null || state.getBlock() != this.block) {
            return false;
        }
        for (Map.Entry<Property<?>, Comparable<?>> entry : this.required.entrySet()) {
            Property<?> property = entry.getKey();
            if (!state.hasProperty(property) || !state.getValue(property).equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public Block block() {
        if (this.block != null) {
            return this.block;
        }
        BlockState rep = representativeState();
        return rep != null ? rep.getBlock() : Blocks.AIR;
    }

    public @Nullable TagKey<Block> tag() {
        return this.tag;
    }

    /**
     * The state a structure preview should draw for this matcher: the block's default state with every required
     * property applied, or the first block in the tag if matching a tag.
     */
    public BlockState representativeState() {
        if (this.tag != null) {
            var tagHolder = ForgeRegistries.BLOCKS.tags().getTag(this.tag);
            var first = tagHolder.stream().findFirst();
            if (first.isPresent()) {
                return first.get().defaultBlockState();
            }
            return Blocks.IRON_BLOCK.defaultBlockState();
        }
        if (this.block == null) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState state = this.block.defaultBlockState();
        for (Map.Entry<Property<?>, Comparable<?>> entry : this.required.entrySet()) {
            state = apply(state, entry.getKey(), entry.getValue());
        }
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState apply(BlockState state, Property<?> property, Comparable<?> value) {
        return state.hasProperty(property) ? state.setValue((Property) property, (Comparable) value) : state;
    }

    /** The descriptor exactly as it appeared in the machine definition. */
    public String descriptor() {
        return this.descriptor;
    }

    @Override
    public String toString() {
        return this.descriptor;
    }
}
