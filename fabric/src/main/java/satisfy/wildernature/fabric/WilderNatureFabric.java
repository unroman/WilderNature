package satisfy.wildernature.fabric;

import com.google.common.base.Preconditions;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import satisfy.wildernature.WilderNature;
import satisfy.wildernature.fabric.config.ConfigFabric;
import satisfy.wildernature.fabric.world.PlacedFeatures;
import satisfy.wildernature.registry.EntityRegistry;
import satisfy.wildernature.registry.TagsRegistry;
import satisfy.wildernature.util.WilderNatureIdentifier;

import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class WilderNatureFabric implements ModInitializer {
    private static Predicate<BiomeSelectionContext> getWilderNatureSelector(String path) {
        return BiomeSelectors.tag(TagKey.create(Registries.BIOME, new WilderNatureIdentifier(path)));
    }

    @Override
    public void onInitialize() {
        AutoConfig.register(ConfigFabric.class, GsonConfigSerializer::new);
        WilderNature.init();
        WilderNature.commonInit();
        addSpawns();
        addBiomeModification();
    }

    void addBiomeModification() {
        ConfigFabric config = AutoConfig.getConfigHolder(ConfigFabric.class).getConfig();
        BiomeModification world = BiomeModifications.create(new WilderNatureIdentifier("world_features"));
        Predicate<BiomeSelectionContext> spawns_patch_hazelnut_bush = getBloomingNatureSelector();

        if (config.spawnHazelnutBush) {
            world.add(ModificationPhase.ADDITIONS, spawns_patch_hazelnut_bush, ctx -> ctx.getGenerationSettings().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, PlacedFeatures.PATCH_HAZELNUT_BUSH));
        } else {
            world.add(ModificationPhase.REMOVALS, spawns_patch_hazelnut_bush, ctx -> ctx.getGenerationSettings().removeFeature(GenerationStep.Decoration.VEGETAL_DECORATION, PlacedFeatures.PATCH_HAZELNUT_BUSH));
        }
    }
    
    void addSpawns() {
        ConfigFabric config = AutoConfig.getConfigHolder(ConfigFabric.class).getConfig();
        addMobSpawn(BiomeTags.IS_BEACH, EntityRegistry.PELICAN.get(), config.PelicanSpawnWeight, 3, 5);
        addMobSpawn(TagsRegistry.SPAWNS_DEER, EntityRegistry.DEER.get(), config.DeerSpawnWeight, 2, 4);
        addMobSpawn(TagsRegistry.SPAWNS_RACCOON, EntityRegistry.RACCOON.get(), config.RaccoonSpawnWeight, 2, 3);
        addMobSpawn(TagsRegistry.SPAWNS_SQUIRREL, EntityRegistry.SQUIRREL.get(),  config.SquirrelSpawnWeight, 2, 2);
        addMobSpawn(TagsRegistry.SPAWNS_RED_WOLF, EntityRegistry.RED_WOLF.get(),  config.RedWolfSpawnWeight, 3, 4);
        addMobSpawn(TagsRegistry.SPAWNS_OWL, EntityRegistry.OWL.get(),  config.OwlSpawnWeight, 3, 3);
        addMobSpawn(TagsRegistry.SPAWNS_BOAR, EntityRegistry.BOAR.get(), config.BoarSpawnWeight, 5, 5);
        addMobSpawn(TagsRegistry.SPAWNS_BISON, EntityRegistry.BISON.get(), config.BisonSpawnWeight, 3, 5);
        addMobSpawn(TagsRegistry.SPAWNS_TURKEY, EntityRegistry.TURKEY.get(), config.TurkeySpawnWeight, 3, 5);

        if (config.removeSavannaAnimals) {
            removeSpawn(BiomeTags.IS_SAVANNA, List.of(EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN, EntityType.COW));
        }
        if (config.removeForestAnimals) {
            removeSpawn(BiomeTags.IS_FOREST, List.of(EntityType.PIG, EntityType.CHICKEN));
        }
        if (config.removeSwampAnimals) {
            removeSpawn(ConventionalBiomeTags.SWAMP, List.of(EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN, EntityType.COW));
        }
        if (config.removeJungleAnimals) {
            removeSpawn(BiomeTags.IS_JUNGLE, List.of(EntityType.PIG, EntityType.CHICKEN, EntityType.COW));
        }
        if (config.addJungleAnimals) {
            addMobSpawn(BiomeTags.IS_JUNGLE, EntityType.FROG, 8, 3, 4);
        }

        SpawnPlacements.register(EntityRegistry.SQUIRREL.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AmbientCreature::checkMobSpawnRules);
        SpawnPlacements.register(EntityRegistry.OWL.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING, AmbientCreature::checkMobSpawnRules);
        SpawnPlacements.register(EntityRegistry.TURKEY.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AmbientCreature::checkMobSpawnRules);
        SpawnPlacements.register(EntityRegistry.RACCOON.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AmbientCreature::checkMobSpawnRules);
        SpawnPlacements.register(EntityRegistry.PELICAN.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AmbientCreature::checkMobSpawnRules);
        SpawnPlacements.register(EntityRegistry.DEER.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AmbientCreature::checkMobSpawnRules);
        SpawnPlacements.register(EntityRegistry.RED_WOLF.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AmbientCreature::checkMobSpawnRules);
        SpawnPlacements.register(EntityRegistry.BOAR.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AmbientCreature::checkMobSpawnRules);
        SpawnPlacements.register(EntityRegistry.BISON.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AmbientCreature::checkMobSpawnRules);
    }

    void addMobSpawn(TagKey<Biome> tag, EntityType<?> entityType, int weight, int minGroupSize, int maxGroupSize) {
        BiomeModifications.addSpawn(biomeSelector -> biomeSelector.hasTag(tag), MobCategory.CREATURE, entityType, weight, minGroupSize, maxGroupSize);
    }

    void removeSpawn(TagKey<Biome> tag, List<EntityType<?>> entityTypes) {
        entityTypes.forEach(entityType -> {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            Preconditions.checkState(BuiltInRegistries.ENTITY_TYPE.containsKey(id), "Unregistered entity type: %s", entityType);
            BiomeModifications.create(id).add(ModificationPhase.REMOVALS, biomeSelector -> biomeSelector.hasTag(tag), context -> context.getSpawnSettings().removeSpawnsOfEntityType(entityType));
        });
    }

    private static Predicate<BiomeSelectionContext> getBloomingNatureSelector() {
        return BiomeSelectors.tag(TagKey.create(Registries.BIOME, new WilderNatureIdentifier("spawns_patch_hazelnut_bush")));
    }
}
