package its.sheepish.sheepish_api.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public class MirroredShapedRecipe implements CraftingRecipe {
    private final ShapedRecipe baseRecipe;
    private final boolean mirrorHorizontal;
    private final boolean mirrorVertical;

    public MirroredShapedRecipe(ShapedRecipe baseRecipe, boolean mirrorHorizontal, boolean mirrorVertical) {
        this.baseRecipe = baseRecipe;
        this.mirrorHorizontal = mirrorHorizontal;
        this.mirrorVertical = mirrorVertical;
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        // 1. Check if the original layout matches
        if (this.baseRecipe.matches(input, world)) {
            return true;
        }

        // 2. If horizontal mirroring is enabled, check the horizontally flipped grid
        if (this.mirrorHorizontal) {
            if (this.baseRecipe.matches(flipInput(input, true, false), world)) {
                return true;
            }
        }

        // 3. If vertical mirroring is enabled, check the vertically flipped grid
        if (this.mirrorVertical) {
            if (this.baseRecipe.matches(flipInput(input, false, true), world)) {
                return true;
            }
        }

        // 4. If both are enabled, check the fully inverted grid
        if (this.mirrorHorizontal && this.mirrorVertical) {
            if (this.baseRecipe.matches(flipInput(input, true, true), world)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper method to flip the CraftingRecipeInput grid layout.
     */
    private CraftingRecipeInput flipInput(CraftingRecipeInput input, boolean horizontal, boolean vertical) {
        int width = input.getWidth();
        int height = input.getHeight();
        java.util.List<ItemStack> flippedStacks = new java.util.ArrayList<>(java.util.Collections.nCopies(width * height, ItemStack.EMPTY));

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int targetX = horizontal ? (width - 1 - x) : x;
                int targetY = vertical ? (height - 1 - y) : y;

                ItemStack stack = input.getStackInSlot(x + y * width);
                flippedStacks.set(targetX + targetY * width, stack);
            }
        }
        return CraftingRecipeInput.create(width, height, flippedStacks);
    }

    // --- Delegate standard recipe logic to the base ShapedRecipe ---

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        return this.baseRecipe.craft(input, registries);
    }

    @Override
    public boolean fits(int width, int height) {
        return this.baseRecipe.fits(width, height);
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registries) {
        return this.baseRecipe.getResult(registries);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MIRRORED_SHAPED_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING; // Reuses the vanilla crafting table type
    }

    public ShapedRecipe getBaseRecipe() {
        return this.baseRecipe;
    }

    public boolean isMirrorHorizontal() {
        return this.mirrorHorizontal;
    }

    public boolean isMirrorVertical() {
        return this.mirrorVertical;
    }

    @Override
    public net.minecraft.recipe.book.CraftingRecipeCategory getCategory() {
        return this.baseRecipe.getCategory();
    }

    @Override
    public net.minecraft.util.collection.DefaultedList<net.minecraft.recipe.Ingredient> getIngredients() {
        return this.baseRecipe.getIngredients();
    }

    // Inside its.sheepish.sheepish_api.recipe.MirroredShapedRecipe
    public int getWidth() {
        return this.baseRecipe.getWidth();
    }

    public int getHeight() {
        return this.baseRecipe.getHeight();
    }
}