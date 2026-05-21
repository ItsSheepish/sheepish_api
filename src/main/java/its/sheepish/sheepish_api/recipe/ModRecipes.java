package its.sheepish.sheepish_api.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static its.sheepish.API.LOGGER;
import static its.sheepish.API.MOD_ID;

public class ModRecipes {
    // 1. Build the MapCodec for reading JSONs via DataPack
    public static final MapCodec<MirroredShapedRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                    CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(ShapedRecipe::getCategory),
                    RawShapedRecipe.CODEC.forGetter(MirroredShapedRecipe::getRawRecipe),
                    ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(recipe -> recipe.getResult(null)),
                    Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification),
                    Codec.BOOL.optionalFieldOf("mirror_horizontal", false).forGetter(MirroredShapedRecipe::isMirrorHorizontal),
                    Codec.BOOL.optionalFieldOf("mirror_vertical", false).forGetter(MirroredShapedRecipe::isMirrorVertical)
            ).apply(instance, MirroredShapedRecipe::new)
    );

    // 2. Build the PacketCodec for syncing recipes over the network connection
    public static final PacketCodec<RegistryByteBuf, MirroredShapedRecipe> PACKET_CODEC = new PacketCodec<>() {
        @Override
        public MirroredShapedRecipe decode(RegistryByteBuf buf) {
            String group = buf.readString();
            CraftingRecipeCategory category = CraftingRecipeCategory.PACKET_CODEC.decode(buf);
            RawShapedRecipe raw = RawShapedRecipe.PACKET_CODEC.decode(buf);
            ItemStack result = ItemStack.PACKET_CODEC.decode(buf);
            boolean showNotification = buf.readBoolean();
            boolean mirrorHorizontal = buf.readBoolean();
            boolean mirrorVertical = buf.readBoolean();

            return new MirroredShapedRecipe(group, category, raw, result, showNotification, mirrorHorizontal, mirrorVertical);
        }

        @Override
        public void encode(RegistryByteBuf buf, MirroredShapedRecipe recipe) {
            buf.writeString(recipe.getGroup());
            CraftingRecipeCategory.PACKET_CODEC.encode(buf, recipe.getCategory());
            RawShapedRecipe.PACKET_CODEC.encode(buf, recipe.getRawRecipe());
            ItemStack.PACKET_CODEC.encode(buf, recipe.getResult(null));
            buf.writeBoolean(recipe.showNotification());
            buf.writeBoolean(recipe.isMirrorHorizontal());
            buf.writeBoolean(recipe.isMirrorVertical());
        }
    };

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