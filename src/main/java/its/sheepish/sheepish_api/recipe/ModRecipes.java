package its.sheepish.sheepish_api.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

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

    public static final MapCodec<ShapedShapelessRecipe> SHAPED_SHAPELESS_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                    CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(ShapedRecipe::getCategory),
                    RawShapedRecipe.CODEC.forGetter(ShapedShapelessRecipe::getShapedPart),
                    Ingredient.DISALLOW_EMPTY_CODEC.listOf().fieldOf("shapeless_ingredients").xmap(
                            ingredients -> {
                                DefaultedList<Ingredient> list = DefaultedList.of();
                                list.addAll(ingredients);
                                return list;
                            },
                            list -> list
                    ).forGetter(ShapedShapelessRecipe::getShapelessPart),
                    ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(recipe -> recipe.getResult(null)),
                    Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(ShapedRecipe::showNotification)
            ).apply(instance, ShapedShapelessRecipe::new)
    );

    public static final PacketCodec<RegistryByteBuf, ShapedShapelessRecipe> SHAPED_SHAPELESS_PACKET_CODEC = new PacketCodec<>() {
        @Override
        public ShapedShapelessRecipe decode(RegistryByteBuf buf) {
            String group = buf.readString();
            CraftingRecipeCategory category = CraftingRecipeCategory.PACKET_CODEC.decode(buf);
            RawShapedRecipe shapedPart = RawShapedRecipe.PACKET_CODEC.decode(buf);

            int shapelessSize = buf.readVarInt();
            DefaultedList<Ingredient> shapelessPart = DefaultedList.ofSize(shapelessSize, Ingredient.EMPTY);
            for (int i = 0; i < shapelessSize; i++) {
                shapelessPart.set(i, Ingredient.PACKET_CODEC.decode(buf));
            }

            ItemStack result = ItemStack.PACKET_CODEC.decode(buf);
            boolean showNotification = buf.readBoolean();

            return new ShapedShapelessRecipe(group, category, shapedPart, shapelessPart, result, showNotification);
        }

        @Override
        public void encode(RegistryByteBuf buf, ShapedShapelessRecipe recipe) {
            buf.writeString(recipe.getGroup());
            CraftingRecipeCategory.PACKET_CODEC.encode(buf, recipe.getCategory());
            RawShapedRecipe.PACKET_CODEC.encode(buf, recipe.getShapedPart());

            DefaultedList<Ingredient> shapelessPart = recipe.getShapelessPart();
            buf.writeVarInt(shapelessPart.size());
            for (Ingredient ingredient : shapelessPart) {
                Ingredient.PACKET_CODEC.encode(buf, ingredient);
            }

            ItemStack.PACKET_CODEC.encode(buf, recipe.getResult(null));
            buf.writeBoolean(recipe.showNotification());
        }
    };

    public static final RecipeSerializer<ShapedShapelessRecipe> SHAPED_SHAPELESS_SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<ShapedShapelessRecipe> codec() {
            return SHAPED_SHAPELESS_CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, ShapedShapelessRecipe> packetCodec() {
            return SHAPED_SHAPELESS_PACKET_CODEC;
        }
    };

    // 4. Initialize registration method
    public static void registerRecipes() {
        Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(MOD_ID, "mirrored_shaped"),
                MIRRORED_SHAPED_SERIALIZER
        );
        Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(MOD_ID, "shaped_shapeless"),
                SHAPED_SHAPELESS_SERIALIZER
        );

        LOGGER.info("Adding custom recipe serializers");
    }
}