package starduster.circuitmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import starduster.circuitmod.Circuitmod;

public class ModEntityTypes {
    
    public static final EntityType<MiningExplosiveEntity> MINING_EXPLOSIVE = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(Circuitmod.MOD_ID, "mining_explosive"),
        EntityType.Builder.<MiningExplosiveEntity>create(MiningExplosiveEntity::new, SpawnGroup.MISC)
            .dimensions(0.25F, 0.25F) // Same size as snowball
            .maxTrackingRange(4)
            .trackingTickInterval(10)
            .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(Circuitmod.MOD_ID, "mining_explosive")))
    );
    
    public static void initialize() {
        Circuitmod.LOGGER.info("Registering mod entity types");
    }
} 