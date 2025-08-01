package starduster.circuitmod.recipe;

import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import starduster.circuitmod.Circuitmod;

public class ModRecipes {
    public static final RecipeSerializer<BloomeryRecipe> BLOOMERY_SERIALIZER = Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(Circuitmod.MOD_ID, "bloomery"),
        new BloomeryRecipe.Serializer());
    public static final RecipeSerializer<CrusherRecipe> CRUSHER_SERIALIZER = Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(Circuitmod.MOD_ID, "crusher"),
        new CrusherRecipe.Serializer());
    public static final RecipeSerializer<ElectricFurnaceRecipe> ELECTRIC_FURNACE_SERIALIZER = Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(Circuitmod.MOD_ID, "electric_furnace"),
        new AbstractCookingRecipe.Serializer<>(ElectricFurnaceRecipe::new, 100));


    public static final RecipeType<BloomeryRecipe> BLOOMERY_TYPE = Registry.register(Registries.RECIPE_TYPE, Identifier.of(Circuitmod.MOD_ID, "bloomery"),
            new RecipeType<BloomeryRecipe>() {@Override public String toString() {return "bloomery";}});
    public static final RecipeType<CrusherRecipe> CRUSHER_TYPE = Registry.register(Registries.RECIPE_TYPE, Identifier.of(Circuitmod.MOD_ID, "crusher"),
            new RecipeType<CrusherRecipe>() {@Override public String toString() {return "crusher";}});
    public static final RecipeType<ElectricFurnaceRecipe> ELECTRIC_FURNACE_TYPE = Registry.register(Registries.RECIPE_TYPE, Identifier.of(Circuitmod.MOD_ID, "electric_furnace"),
            new RecipeType<ElectricFurnaceRecipe>() {@Override public String toString() {return "electric_furnace";}});

    public static void initialize() { Circuitmod.LOGGER.info("Registering mod recipes"); }

}
