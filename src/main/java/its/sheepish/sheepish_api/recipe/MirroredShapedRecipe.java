package its.sheepish.sheepish_api.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.world.World;

public class MirroredShapedRecipe extends ShapedRecipe {
    private final RawShapedRecipe rawRecipe;
    private final boolean mirrorHorizontal;
    private final boolean mirrorVertical;

    public MirroredShapedRecipe(String group, CraftingRecipeCategory category, RawShapedRecipe raw, ItemStack result, boolean showNotification, boolean mirrorHorizontal, boolean mirrorVertical) {
        super(group, category, raw, result, showNotification);
        this.rawRecipe = raw;
        this.mirrorHorizontal = mirrorHorizontal;
        this.mirrorVertical = mirrorVertical;
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        // 1. Check if the original layout matches (handled by superclass)
        if (super.matches(input, world)) {
            return true;
        }

        // 2. If horizontal mirroring is enabled, check the horizontally flipped grid
        if (this.mirrorHorizontal && super.matches(flipInput(input, true, false), world)) {
            return true;
        }

        // 3. If vertical mirroring is enabled, check the vertically flipped grid
        if (this.mirrorVertical && super.matches(flipInput(input, false, true), world)) {
            return true;
        }

        // 4. If both are enabled, check the fully inverted grid
        return this.mirrorHorizontal && this.mirrorVertical && super.matches(flipInput(input, true, true), world);
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

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MIRRORED_SHAPED_SERIALIZER;
    }

    // We store rawRecipe locally so we can grab it for the Codec serialization
    public RawShapedRecipe getRawRecipe() {
        return this.rawRecipe;
    }

    public boolean isMirrorHorizontal() {
        return this.mirrorHorizontal;
    }

    public boolean isMirrorVertical() {
        return this.mirrorVertical;
    }
}