package its.sheepish;

import its.sheepish.sheepish_api.advancement.InventoryCountTrigger;
import its.sheepish.sheepish_api.recipe.ModRecipes;
import net.fabricmc.api.ModInitializer;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class API implements ModInitializer {
	public static final String MOD_ID = "sheepish_api";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static final InventoryCountTrigger INVENTORY_COUNT = Criteria.register(MOD_ID + ":inventory_count", new InventoryCountTrigger());

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";


	@Override
	public void onInitialize() {
        LOGGER.info(ANSI_BOLD + "BAAAH 🐏" + ANSI_RESET);

        ModRecipes.registerRecipes();
	}
}