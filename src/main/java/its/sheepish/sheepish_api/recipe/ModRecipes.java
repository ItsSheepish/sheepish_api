package its.sheepish.sheepish_api.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static its.sheepish.API.LOGGER;
import static its.sheepish.API.MOD_ID;

public class ModRecipes {
    // 1. Build the MapCodec for reading JSONs via DataPack
    public static final MapCodec<MirroredShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> 
        instance.group(
            ShapedRecipe.Serializer.CODEC.forGetter(MirroredShapedRecipe::getBaseRecipe),
            Codec.BOOL.optionalFieldOf("mirror_horizontal", false).forGetter(MirroredShapedRecipe::isMirrorHorizontal),
            Codec.BOOL.optionalFieldOf("mirror_vertical", false).forGetter(MirroredShapedRecipe::isMirrorVertical)
        ).apply(instance, MirroredShapedRecipe::new)
    );

    // 2. Build the PacketCodec for syncing recipes over the network connection
    // Build the PacketCodec for syncing recipes over the network connection
    public static final PacketCodec<RegistryByteBuf, MirroredShapedRecipe> PACKET_CODEC = PacketCodec.tuple(
            RecipeSerializer.SHAPED.packetCodec(), MirroredShapedRecipe::getBaseRecipe,
            PacketCodecs.BOOL, MirroredShapedRecipe::isMirrorHorizontal,
            PacketCodecs.BOOL, MirroredShapedRecipe::isMirrorVertical,
            MirroredShapedRecipe::new
    );

    // 3. Define the Serializer instance
    public static final RecipeSerializer<MirroredShapedRecipe> MIRRORED_SHAPED_SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<MirroredShapedRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, MirroredShapedRecipe> packetCodec() {
            return PACKET_CODEC;
        }
    };

    // 4. Initialize registration method
    public static void registerRecipes() {
        Registry.register(
            Registries.RECIPE_SERIALIZER, 
            Identifier.of(MOD_ID, "mirrored_shaped"), 
            MIRRORED_SHAPED_SERIALIZER
        );
        LOGGER.info("Adding mirrored shaped recipes");
    }
}