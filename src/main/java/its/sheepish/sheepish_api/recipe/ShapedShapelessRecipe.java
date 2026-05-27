package its.sheepish.sheepish_api.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ShapedShapelessRecipe extends ShapedRecipe {
    private final RawShapedRecipe shapedPart;
    private final DefaultedList<Ingredient> shapelessPart;
    // Cache the combined list so we don't recreate it every frame in the UI
    private DefaultedList<Ingredient> combinedIngredients = null;

    public ShapedShapelessRecipe(String group, CraftingRecipeCategory category, RawShapedRecipe shapedPart, DefaultedList<Ingredient> shapelessPart, ItemStack result, boolean showNotification) {
        super(group, category, shapedPart, result, showNotification);
        this.shapedPart = shapedPart;
        this.shapelessPart = shapelessPart;

        // 1. Calculate how many physical slots the shaped structure occupies
        int nonAirShapedSlots = 0;
        for (Ingredient ingredient : shapedPart.getIngredients()) {
            if (!ingredient.isEmpty()) {
                nonAirShapedSlots++;
            }
        }

        // 2. Maximum possible capacity in a standard 3x3 crafting grid
        int maxGridCapacity = 9;
        int remainingAvailableSlots = maxGridCapacity - nonAirShapedSlots;

        // 3. Throw a clear exception if the shapeless extra requirements overflow the grid boundaries
        if (shapelessPart.size() > remainingAvailableSlots) {
            throw new IllegalArgumentException(String.format(
                    "Invalid ShapedShapeless Recipe! The shaped pattern uses %d physical slots, leaving only %d slots open. " +
                            "However, %d shapeless ingredients were requested. It is physically impossible to fit this recipe in a 3x3 crafting grid.",
                    nonAirShapedSlots,
                    remainingAvailableSlots,
                    shapelessPart.size()
            ));
        }
    }

    /**
     * Overriding this updates the recipe book preview grid.
     */
    @Override
    public DefaultedList<Ingredient> getIngredients() {
        if (this.combinedIngredients == null) {
            DefaultedList<Ingredient> shapedIngredients = this.shapedPart.getIngredients();

            // 1. Create a container filled with empty slots for a full 3x3 layout (9 slots)
            this.combinedIngredients = DefaultedList.ofSize(9, Ingredient.EMPTY);

            // 2. Map the localized pattern shape into the 3x3 preview grid layout
            int patternWidth = this.shapedPart.getWidth();
            int patternHeight = this.shapedPart.getHeight();

            for (int y = 0; y < patternHeight; y++) {
                for (int x = 0; x < patternWidth; x++) {
                    int patternIndex = x + y * patternWidth;
                    int previewGridIndex = x + y * 3; // Shift into 3x3 space
                    this.combinedIngredients.set(previewGridIndex, shapedIngredients.get(patternIndex));
                }
            }

            // 3. Distribute the extra shapeless ingredients into remaining empty slots
            int shapelessIdx = 0;
            for (int i = 0; i < 9 && shapelessIdx < this.shapelessPart.size(); i++) {
                // Find a slot that isn't being used by the shaped structure (or is air)
                if (this.combinedIngredients.get(i).isEmpty()) {
                    this.combinedIngredients.set(i, this.shapelessPart.get(shapelessIdx));
                    shapelessIdx++;
                }
            }

            // 4. Fallback: If your shaped recipe takes all 9 slots but has shapeless extras,
            // append them to the end so they aren't lost from data transfers.
            while (shapelessIdx < this.shapelessPart.size()) {
                this.combinedIngredients.add(this.shapelessPart.get(shapelessIdx));
                shapelessIdx++;
            }
        }
        return this.combinedIngredients;
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        int inputWidth = input.getWidth();
        int inputHeight = input.getHeight();
        int patternWidth = this.shapedPart.getWidth();
        int patternHeight = this.shapedPart.getHeight();

        for (int startX = 0; startX <= inputWidth - patternWidth; ++startX) {
            for (int startY = 0; startY <= inputHeight - patternHeight; ++startY) {
                if (checkMatchAtOffset(input, startX, startY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkMatchAtOffset(CraftingRecipeInput input, int startX, int startY) {
        int inputWidth = input.getWidth();
        int patternWidth = this.shapedPart.getWidth();
        int patternHeight = this.shapedPart.getHeight();

        boolean[] usedSlots = new boolean[input.getSize()];

        for (int x = 0; x < patternWidth; ++x) {
            for (int y = 0; y < patternHeight; ++y) {
                int patternIndex = x + y * patternWidth;
                Ingredient ingredient = this.shapedPart.getIngredients().get(patternIndex);

                if (ingredient.isEmpty()) {
                    continue;
                }

                int inputX = startX + x;
                int inputY = startY + y;
                int inputIndex = inputX + inputY * inputWidth;
                ItemStack inputStack = input.getStackInSlot(inputIndex);

                if (!ingredient.test(inputStack)) {
                    return false;
                }

                usedSlots[inputIndex] = true;
            }
        }

        List<ItemStack> remainingItems = new ArrayList<>();
        for (int i = 0; i < input.getSize(); i++) {
            if (!usedSlots[i]) {
                ItemStack stack = input.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    remainingItems.add(stack);
                }
            }
        }

        if (remainingItems.size() != this.shapelessPart.size()) {
            return false;
        }

        List<Ingredient> unmatchedIngredients = new ArrayList<>(this.shapelessPart);
        for (ItemStack stack : remainingItems) {
            boolean matched = false;
            for (int i = 0; i < unmatchedIngredients.size(); i++) {
                if (unmatchedIngredients.get(i).test(stack)) {
                    unmatchedIngredients.remove(i);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }

        return unmatchedIngredients.isEmpty();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SHAPED_SHAPELESS_SERIALIZER;
    }

    public RawShapedRecipe getShapedPart() {
        return this.shapedPart;
    }

    public DefaultedList<Ingredient> getShapelessPart() {
        return this.shapelessPart;
    }
}