package com.khanhpham.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.khanhpham.MekanismExtension;
import com.khanhpham.icon.IconRegistry;
import mekanism.additions.common.registries.AdditionsEntityTypes;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismItems;
import mekanism.common.resource.PrimaryResource;
import mekanism.common.resource.ResourceType;
import mekanism.generators.common.registries.GeneratorsBlocks;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.ICriterionInstance;
import net.minecraft.advancements.IRequirementsStrategy;
import net.minecraft.advancements.criterion.*;
import net.minecraft.data.AdvancementProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DirectoryCache;
import net.minecraft.data.IDataProvider;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static mekanism.generators.common.registries.GeneratorsBlocks.HEAT_GENERATOR;


public class ModAdvancementProvider extends AdvancementProvider {

    private final DataGenerator generator;
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private final Logger LOGGER = LogManager.getLogger();

    public ModAdvancementProvider(DataGenerator generatorIn) {
        super(generatorIn);
        generator = generatorIn;

    }

    @Override
    public String getName() {
        ;
        return "Advancement Plus Mekanism Extension";
    }


    @Override
    public void act(DirectoryCache cache) throws IOException {
        Path path = this.generator.getOutputFolder();
        Set<ResourceLocation> set = Sets.newHashSet();
        Consumer<Advancement> consumer = (advancement) -> {
            if (!set.add(advancement.getId())) {
                throw new IllegalStateException("Duplicate advancement " + advancement.getId());
            } else {
                Path path1 = getPath(path, advancement);

                try {
                    IDataProvider.save(GSON, cache, advancement.copy().serialize(), path1);
                } catch (IOException ioexception) {
                    LOGGER.error("Couldn't save advancement {}", path1, ioexception);
                }

            }
        };

        new Generator().accept(consumer);
    }

    private static Path getPath(Path pathIn, Advancement advancementIn) {
        return pathIn.resolve("data/" + MekanismExtension.MOD_ID + "/advancements/" + advancementIn.getId().getPath() + ".json");
    }

    public static final class Generator implements Consumer<Consumer<Advancement>> {

        public Generator() {
        }

        /**
         * @see net.minecraft.data.advancements.StoryAdvancements
         * @see net.minecraft.data.advancements.NetherAdvancements
         * @see net.minecraft.data.advancements.AdventureAdvancements
         * @see InventoryChangeTrigger
         * @see mekanism.additions.common.registries.AdditionsEntityTypes
         */

        final FrameType task = FrameType.TASK;
        final FrameType goal = FrameType.GOAL;
        final FrameType challenge = FrameType.CHALLENGE;


        @Override
        public void accept(Consumer<Advancement> consumer) {
            Advancement root = makeRootTrigger(allTheBiomes).withDisplay(IconRegistry.ICON.get(), getTitle("root"), getDesc(), new ResourceLocation("mekanism:textures/block/block_steel.png"), FrameType.CHALLENGE, true, true, false)
                    .withRequirementsStrategy(IRequirementsStrategy.OR)
                    .register(consumer, getId("root"));

            Advancement heartOfMekanism = advancement(consumer, root, getMekanismResources(ResourceType.INGOT, PrimaryResource.OSMIUM), "heart_of_mekanism", task, "has_osmium_ingot", null, getId("heart_of_mekanism"));

            Advancement copping = advancement(consumer, heartOfMekanism, getMekanismResources(ResourceType.INGOT, PrimaryResource.COPPER), "copping", task, "has_copper_ingot", null, "copping");

            //GENERATORS
            Advancement firstGenerator = advancement(consumer, copping, HEAT_GENERATOR.getItem(), "first_generator", task, "has_heat_gen", null, generatorId("first_generator"));

            Advancement kindaBetterGen = advancement(consumer, firstGenerator, GeneratorsBlocks.SOLAR_GENERATOR.getItem(), "kinda_better_generator", task, "has_solar_gen", null, generatorId("solar"));

            Advancement betterSolarGen = advancement(consumer, kindaBetterGen, GeneratorsBlocks.ADVANCED_SOLAR_GENERATOR.getItem(), "better_solar_gen", task, "has_advanced_solar_gen", null, generatorId("solar_advanced"));

            Advancement windyGen = advancement(consumer, betterSolarGen, GeneratorsBlocks.WIND_GENERATOR.getItem(), "windy_gen", task, "has_wind_gen", null, generatorId("wind"));

            //MACHINES

            Advancement infusingMetal = advancement(consumer, heartOfMekanism, MekanismBlocks.METALLURGIC_INFUSER.getItem(), "infusing_metal", goal, "has_infuser", null, machineId("metallurgic_infuser"));

            Advancement chamberOfRichness = advancement(consumer, infusingMetal, MekanismBlocks.ENRICHMENT_CHAMBER.getItem(), "chamber_of_richness", goal, "has_enrichment_chamber", null, machineId("enrichment_chamber"));

            Advancement crushingMatter = advancement(consumer, chamberOfRichness, MekanismBlocks.CRUSHER.getItem(), "crushing_matter", goal, "has_crusher", null, machineId("crusher"));

            //ALLOYS
            Advancement infused = advancement(consumer, infusingMetal, MekanismItems.INFUSED_ALLOY.get(), "infused_alloy", task, "has_alloy_tier1", setRequireItem(MekanismItems.INFUSED_ALLOY.get()), alloyId("tier1"));

            Advancement reinforced = advancement(consumer, infused, MekanismItems.REINFORCED_ALLOY.get(), "reinforced_alloy", task, "has_reinforced_alloy", null, alloyId("reinforced"));


            //CONTROL CIRCUITS
            Advancement basicCC = advancement(consumer, infusingMetal, MekanismItems.BASIC_CONTROL_CIRCUIT.get(), "basic_cc", task, "has_basic_cc", null, ccId("basic"));

            Advancement advancedCC = advancement(consumer, basicCC, MekanismItems.ADVANCED_CONTROL_CIRCUIT.get(), "advanced_cc", task, "has_advanced_cc", null, ccId("advanced"));

            Advancement eliteCC = advancement(consumer, advancedCC, MekanismItems.ELITE_CONTROL_CIRCUIT.get(), "elite_cc", task, "has_elite_cc", null, ccId("elite"));

            Advancement ultimate = advancement(consumer, eliteCC, MekanismItems.ULTIMATE_CONTROL_CIRCUIT.get(), "ultimate_cc", goal, "has_ultimate_cc", null, ccId("ultimate"));


            //ADDITION MOBS
            Advancement tinyMonster = test()
                    .withParent(root)
                    .withDisplay(Items.GOLDEN_SWORD, getTitle("tiny_monster"), getDesc(), null, task, true, true, false)
                    .register(consumer, getId("tiny_monster"));

        }

        @SuppressWarnings("deprecation")
        Advancement.Builder test() {
            List<EntityType<?>> entityType = ImmutableList.of(AdditionsEntityTypes.BABY_CREEPER.getEntityType(), AdditionsEntityTypes.BABY_ENDERMAN.getEntityType(), AdditionsEntityTypes.BABY_SKELETON.getEntityType(), AdditionsEntityTypes.BABY_WITHER_SKELETON.getEntityType(), AdditionsEntityTypes.BABY_STRAY.getEntityType());
            Advancement.Builder builder = getBuilder();
            for (EntityType<?> entity : entityType) {
                builder.withCriterion(Registry.ENTITY_TYPE.getKey(entity).toString(), KilledTrigger.Instance.playerKilledEntity(EntityPredicate.Builder.create().type(entity)));
            }

            return builder;
        }

        /**
         * @param criterion - instance of Custom/Vanilla Criterion Trigger
         *                  If you want advancement require item as same with icon, just leave it null
         */
        private Advancement advancement(Consumer<Advancement> consumer, Advancement parent, @Nullable IItemProvider icon, String key, FrameType frame, String criterionId, ICriterionInstance criterion, String id) {


            return criterion == null
                    ? getBuilder()
                    .withParent(parent)
                    .withDisplay(icon, getTitle(key), getDesc(), null, frame, true, true, false)
                    .withCriterion(criterionId, InventoryChangeTrigger.Instance.forItems(icon))
                    .register(consumer, id)

                    : getBuilder()
                    .withParent(parent)
                    .withDisplay(icon, getTitle(key), getDesc(), null, frame, true, true, false)
                    .withCriterion(criterionId, criterion)
                    .register(consumer, id);
        }


        private InventoryChangeTrigger.Instance setRequireItem(IItemProvider item) {
            return InventoryChangeTrigger.Instance.forItems(item);
        }


        private Advancement.Builder makeRootTrigger(List<RegistryKey<Biome>> biomes) {
            Advancement.Builder builder = getBuilder();
            for (RegistryKey<Biome> biome : biomes) {

                builder.withCriterion(biome.getLocation().toString(), PositionTrigger.Instance.forLocation(LocationPredicate.forBiome(biome)));
            }
            return builder;
        }

        private IItemProvider getMekanismResources(ResourceType resourceType, PrimaryResource primaryResource) {
            return MekanismItems.PROCESSED_RESOURCES.get(resourceType, primaryResource);

        }

        private Advancement.Builder getBuilder() {
            return Advancement.Builder.builder();
        }


        String key;

        private TranslationTextComponent getTitle(String key) {
            this.key = key;
            return new TranslationTextComponent("advancement.plus." + key + ".title");
        }

        private TranslationTextComponent getDesc() {
            return new TranslationTextComponent("advancement.plus." + key + ".desc");
        }

        private String getId(String key) {
            return MekanismExtension.MOD_ID + ":" + key;
        }

        private String generatorId(String key) {
            checkId(key);
            return MekanismExtension.MOD_ID + ":generators/" + key;
        }

        private String machineId(String key) {
            checkId(key);
            return MekanismExtension.MOD_ID + ":machines/" + key;
        }

        private String alloyId(String key) {
            checkId(key);
            return MekanismExtension.MOD_ID + ":alloy/" + key;
        }

        private String ccId(String key) {
            checkId(key);
            return MekanismExtension.MOD_ID + ":circuit_board/" + key;
        }

        private void checkId(String key) {
            for (int i = 0; i < key.length(); i++) {
                if (key.charAt(i) == ' ') {
                    throw new IllegalStateException("Id " + key + " can not have space!");
                }
            }
        }

        private final List<RegistryKey<Biome>> allTheBiomes = ImmutableList.of(
                Biomes.SNOWY_TUNDRA,
                Biomes.ICE_SPIKES,
                Biomes.SNOWY_TAIGA,
                Biomes.SNOWY_TAIGA_MOUNTAINS,
                Biomes.FROZEN_RIVER,
                Biomes.SNOWY_BEACH,
                Biomes.MOUNTAINS,
                Biomes.GRAVELLY_MOUNTAINS,
                Biomes.WOODED_MOUNTAINS,
                Biomes.MODIFIED_GRAVELLY_MOUNTAINS,
                Biomes.TAIGA,
                Biomes.TAIGA_MOUNTAINS,
                Biomes.GIANT_TREE_TAIGA,
                Biomes.GIANT_SPRUCE_TAIGA,
                Biomes.STONE_SHORE,
                Biomes.PLAINS,
                Biomes.FOREST,
                Biomes.SUNFLOWER_PLAINS,
                Biomes.FLOWER_FOREST,
                Biomes.BIRCH_FOREST,
                Biomes.TALL_BIRCH_FOREST,
                Biomes.DARK_FOREST,
                Biomes.DARK_FOREST_HILLS,
                Biomes.SWAMP,
                Biomes.SWAMP_HILLS,
                Biomes.JUNGLE,
                Biomes.MODIFIED_JUNGLE,
                Biomes.JUNGLE_EDGE,
                Biomes.MODIFIED_JUNGLE_EDGE,
                Biomes.BAMBOO_JUNGLE,
                Biomes.RIVER,
                Biomes.BEACH,
                Biomes.MUSHROOM_FIELDS,
                Biomes.MUSHROOM_FIELD_SHORE,
                Biomes.DESERT,
                Biomes.DESERT_LAKES,
                Biomes.SAVANNA,
                Biomes.SHATTERED_SAVANNA,
                Biomes.ERODED_BADLANDS,
                Biomes.BADLANDS,
                Biomes.WOODED_BADLANDS_PLATEAU,
                Biomes.MODIFIED_WOODED_BADLANDS_PLATEAU,
                Biomes.BADLANDS_PLATEAU,
                Biomes.SAVANNA_PLATEAU,
                Biomes.MODIFIED_BADLANDS_PLATEAU,
                Biomes.SHATTERED_SAVANNA_PLATEAU,
                Biomes.OCEAN,
                Biomes.LUKEWARM_OCEAN,
                Biomes.DEEP_LUKEWARM_OCEAN,
                Biomes.WARM_OCEAN,
                Biomes.DEEP_OCEAN,
                Biomes.COLD_OCEAN,
                Biomes.DEEP_COLD_OCEAN,
                Biomes.FROZEN_OCEAN,
                Biomes.DEEP_FROZEN_OCEAN,
                Biomes.SNOWY_MOUNTAINS,
                Biomes.GIANT_TREE_TAIGA_HILLS,
                Biomes.DESERT_HILLS,
                Biomes.JUNGLE_HILLS,
                Biomes.BAMBOO_JUNGLE_HILLS,
                Biomes.BIRCH_FOREST_HILLS,
                Biomes.WOODED_HILLS,
                Biomes.GIANT_SPRUCE_TAIGA_HILLS,
                Biomes.TAIGA_HILLS,
                Biomes.SNOWY_TAIGA_HILLS,
                Biomes.TALL_BIRCH_HILLS,

                //NETHER
                Biomes.NETHER_WASTES,
                Biomes.SOUL_SAND_VALLEY,
                Biomes.CRIMSON_FOREST,
                Biomes.WARPED_FOREST,
                Biomes.BASALT_DELTAS,

                //END
                Biomes.THE_END,
                Biomes.END_BARRENS,
                Biomes.SMALL_END_ISLANDS,
                Biomes.END_MIDLANDS,
                Biomes.END_HIGHLANDS,

                //VOID ??
                Biomes.THE_VOID
        );
    }
}
