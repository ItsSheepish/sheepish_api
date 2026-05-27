package its.sheepish.sheepish_api.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import its.sheepish.sheepish_api.misc.SecondsToTicks;
import its.sheepish.sheepish_api.recipe.MirroredShapedRecipe;
import its.sheepish.sheepish_api.recipe.ShapedShapelessRecipe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.recipe.RecipeType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

@EmiEntrypoint
public class SheepishEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.getRecipeManager().listAllOfType(RecipeType.CRAFTING).forEach(entry -> {
            if (entry.value() instanceof MirroredShapedRecipe mirrored) {
                Identifier customEmiId = Identifier.of(
                        entry.id().getNamespace(),
                        "/" + entry.id().getPath() + "_mirrored"
                );
                registry.removeRecipes(entry.id());
                registry.addRecipe(new MirroredEmiRecipe(customEmiId, mirrored));
            }
            else if (entry.value() instanceof ShapedShapelessRecipe shapedShapeless) {
                Identifier customEmiId = Identifier.of(
                        entry.id().getNamespace(),
                        "/" + entry.id().getPath() + "_shaped_shapeless"
                );
                registry.removeRecipes(entry.id());
                registry.addRecipe(new ShapedShapelessEmiRecipe(customEmiId, shapedShapeless));
            }
        });
    }
}

class MirroredEmiRecipe implements EmiRecipe {
    private static final EmiTexture MIRROR_ICON = new EmiTexture(
            Identifier.of("sheepish_api", "textures/gui/mirror_icon.png"),
            0, 0, 16, 16, 16, 16, 16, 16
    );

    private static final Text TICK = Text.literal(" ✔").formatted(Formatting.GREEN);
    private static final Text CROSS = Text.literal(" ✘").formatted(Formatting.RED);

    private final Identifier id;
    private final List<EmiIngredient> input;
    private final EmiStack output;
    private final int width;
    private final int height;
    private final boolean mirrorHorizontal;
    private final boolean mirrorVertical;

    public MirroredEmiRecipe(Identifier id, MirroredShapedRecipe recipe) {
        this.id = id;
        this.width = recipe.getWidth();
        this.height = recipe.getHeight();
        this.mirrorHorizontal = recipe.isMirrorHorizontal();
        this.mirrorVertical = recipe.isMirrorVertical();
        this.output = EmiStack.of(recipe.getResult(null));

        List<EmiIngredient> recipeIngredients = recipe.getIngredients().stream().map(EmiIngredient::of).toList();
        this.input = new ArrayList<>(9);

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (x < this.width && y < this.height) {
                    int index = x + y * this.width;
                    if (index < recipeIngredients.size()) {
                        this.input.add(recipeIngredients.get(index));
                        continue;
                    }
                }
                this.input.add(EmiIngredient.of(net.minecraft.recipe.Ingredient.EMPTY));
            }
        }
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VanillaEmiRecipeCategories.CRAFTING;
    }

    @Override
    public @Nullable Identifier getId() {
        return this.id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return this.input;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(this.output);
    }

    @Override
    public int getDisplayWidth() {
        return 118;
    }

    @Override
    public int getDisplayHeight() {
        return 54;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 18);

        for (int i = 0; i < 9; i++) {
            int x = (i % 3) * 18;
            int y = (i / 3) * 18;
            widgets.addSlot(this.input.get(i), x, y);
        }

        widgets.addSlot(this.output, 92, 14).large(true).recipeContext(this);

        MutableText horizontalLine = Text.translatable("tooltip.sheep_api.recipe.horizontal_mirror")
                .append(":")
                .append(this.mirrorHorizontal ? TICK : CROSS)
                .formatted(this.mirrorHorizontal ? Formatting.GREEN : Formatting.RED);

        MutableText verticalLine = Text.translatable("tooltip.sheep_api.recipe.vertical_mirror")
                .append(":")
                .append(this.mirrorVertical ? TICK : CROSS)
                .formatted(this.mirrorVertical ? Formatting.GREEN : Formatting.RED);

        widgets.addTexture(MIRROR_ICON, 98, 0)
                .tooltipText(List.of(
                        Text.translatable("tooltip.sheep_api.recipe.mirrorable"),
                        horizontalLine,
                        verticalLine
                ));
    }
}

class ShapedShapelessEmiRecipe implements EmiRecipe {
    private final Identifier id;
    private final List<EmiIngredient> flatInputs;
    private final EmiStack output;

    private final List<EmiIngredient> fixedGrid = new ArrayList<>(Collections.nCopies(9, EmiIngredient.of(net.minecraft.recipe.Ingredient.EMPTY)));
    private final List<EmiIngredient> shapelessIngredients;
    private final List<Integer> availableSlots = new ArrayList<>();

    public ShapedShapelessEmiRecipe(Identifier id, ShapedShapelessRecipe recipe) {
        this.id = id;
        this.output = EmiStack.of(recipe.getResult(null));
        this.flatInputs = recipe.getIngredients().stream().map(EmiIngredient::of).toList();

        int patternWidth = recipe.getShapedPart().getWidth();
        int patternHeight = recipe.getShapedPart().getHeight();
        List<EmiIngredient> shapedPart = recipe.getShapedPart().getIngredients().stream().map(EmiIngredient::of).toList();

        for (int y = 0; y < patternHeight; y++) {
            for (int x = 0; x < patternWidth; x++) {
                int patternIdx = x + y * patternWidth;
                int gridIdx = x + y * 3;
                if (patternIdx < shapedPart.size()) {
                    fixedGrid.set(gridIdx, shapedPart.get(patternIdx));
                }
            }
        }

        for (int i = 0; i < 9; i++) {
            if (fixedGrid.get(i).isEmpty()) {
                availableSlots.add(i);
            }
        }

        this.shapelessIngredients = recipe.getShapelessPart().stream().map(EmiIngredient::of).toList();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VanillaEmiRecipeCategories.CRAFTING;
    }

    @Override
    public @Nullable Identifier getId() {
        return this.id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return this.flatInputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(this.output);
    }

    @Override
    public int getDisplayWidth() {
        return 118;
    }

    @Override
    public int getDisplayHeight() {
        return 54;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 18);

        for (int i = 0; i < 9; i++) {
            int x = (i % 3) * 18;
            int y = (i / 3) * 18;

            EmiIngredient baseIngredient = fixedGrid.get(i);

            if (!baseIngredient.isEmpty()) {
                widgets.addSlot(baseIngredient, x, y);
            } else if (availableSlots.contains(i) && !shapelessIngredients.isEmpty()) {
                int slotOrderIndex = availableSlots.indexOf(i);

                // Create our dynamic shuffling slot widget
                CyclingSlotWidget cyclingSlot = new CyclingSlotWidget(x, y, slotOrderIndex, availableSlots, shapelessIngredients);

                // Use EMI's built-in text appender builder. It correctly appends line items below item names safely.
                cyclingSlot.appendTooltip(Text.translatable("tooltip.sheep_api.recipe.shapeless_ingredient"));

                widgets.add(cyclingSlot);
            } else {
                widgets.addSlot(baseIngredient, x, y);
            }
        }

        widgets.addSlot(this.output, 92, 14).large(true).recipeContext(this);
    }

    /**
     * Custom slot widget that intercepts EMI's ingredient getter dynamically.
     * Displays a custom icon emblem in the slot corner and supports standard appends.
     */
    private static class CyclingSlotWidget extends SlotWidget {
        // Points to assets/sheepish_api/textures/gui/shapeless_icon.png (make sure this texture is 16x16)
        private static final EmiTexture SHAPELESS_ICON = new EmiTexture(
                Identifier.of("sheepish_api", "textures/gui/shapeless_icon.png"),
                0, 0, 16, 16, 16, 16, 16, 16
        );

        private final int slotOrderIndex;
        private final List<Integer> availableSlots;
        private final List<EmiIngredient> shapelessIngredients;

        public CyclingSlotWidget(int x, int y, int slotOrderIndex, List<Integer> availableSlots, List<EmiIngredient> shapelessIngredients) {
            super(EmiIngredient.of(net.minecraft.recipe.Ingredient.EMPTY), x, y);
            this.slotOrderIndex = slotOrderIndex;
            this.availableSlots = availableSlots;
            this.shapelessIngredients = shapelessIngredients;
        }

        @Override
        public EmiIngredient getStack() {
            // Convert our 5 seconds target into total milliseconds per cycle stage
            // 5 seconds * 20 ticks/sec * 50 ms/tick = 5000ms
            long msPerCycle = SecondsToTicks.toTicks(5) * 50L;

            long timeDivision = System.currentTimeMillis() / msPerCycle;
            int timeOffset = (int) (timeDivision % availableSlots.size());
            int targetIngredientIdx = (slotOrderIndex - timeOffset + availableSlots.size()) % availableSlots.size();

            if (targetIngredientIdx < shapelessIngredients.size()) {
                EmiIngredient ingredient = shapelessIngredients.get(targetIngredientIdx);
                List<EmiStack> renderStacks = ingredient.getEmiStacks();

                if (!renderStacks.isEmpty()) {
                    // Internal sub-cycling for tags (like #logs) changes every 1 second
                    long msPerSubCycle = SecondsToTicks.toTicks(1)  * 50L;
                    int miniCycle = (int) ((System.currentTimeMillis() / msPerSubCycle) % renderStacks.size());
                    return renderStacks.get(miniCycle);
                }
            }
            return super.getStack();
        }

        @Override
        public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
            // 1. Draw the slot background bounding box
            super.render(drawContext, mouseX, mouseY, delta);

            // 2. Render our active shifting item stack
            EmiIngredient activeIngredient = getStack();
            if (!activeIngredient.isEmpty() && !activeIngredient.getEmiStacks().isEmpty()) {
                EmiStack visibleStack = activeIngredient.getEmiStacks().get(0);
                visibleStack.render(drawContext, x + 1, y + 1, delta);

                // 3. Overlay our custom symbol on top of the items by pushing it forward on the Z-axis
                drawContext.getMatrices().push();
                // Translate Z by 200 units to comfortably clear the item model and any item glint effects
                drawContext.getMatrices().translate(0, 0, 200.0F);

                // Render the icon using the corrected top-right boundary positions (x + 12, y + 1)
                SHAPELESS_ICON.render(drawContext, x + 1, y + 1, delta);

                drawContext.getMatrices().pop();
            }
        }
    }
}