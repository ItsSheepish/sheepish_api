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
import dev.emi.emi.api.widget.WidgetHolder;
import its.sheepish.sheepish_api.recipe.MirroredShapedRecipe;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.recipe.RecipeType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
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
        });
    }
}

class MirroredEmiRecipe implements EmiRecipe {
    private static final EmiTexture MIRROR_ICON = new EmiTexture(
            Identifier.of("sheepish_api", "textures/gui/mirror_icon.png"),
            0, 0, 16, 16, 16, 16, 16, 16
    );

    // Standard styling constants
    private static final Text TICK = Text.literal(" ✔").formatted(Formatting.GREEN);
    private static final Text CROSS = Text.literal(" ✘").formatted(Formatting.RED);

    private final Identifier id;
    private final List<EmiIngredient> input;
    private final EmiStack output;
    private final int width;
    private final int height;

    // Store mirror capabilities for rendering the tooltips later
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

        // Build the tooltip cleanly with proper text component appending
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