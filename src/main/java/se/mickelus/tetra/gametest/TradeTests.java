package se.mickelus.tetra.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.registries.DeferredRegister;
import se.mickelus.tetra.TetraMod;
import se.mickelus.tetra.blocks.scroll.ScrollData;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * That Tetra sells things again.
 *
 * The trades were code until the port, when the events they hung off were removed and the package
 * with them. They are registry data now, which means nothing fails loudly if they are wrong: a
 * malformed file is logged and skipped, a trade in no tag is simply never offered, and either way
 * the mod boots clean and quietly sells nothing. That is what these tests are for.
 */
public class TradeTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> testFunctions =
            DeferredRegister.create(Registries.TEST_FUNCTION, TetraMod.MOD_ID);

    /** Every tag the trades are appended to, and how many of ours each should carry. */
    private static final List<TagCount> expected = List.of(
            new TagCount(VillagerTradeTags.WANDERING_TRADER_COMMON, 4),
            new TagCount(VillagerTradeTags.WANDERING_TRADER_UNCOMMON, 4),
            new TagCount(VillagerTradeTags.TOOLSMITH_LEVEL_2, 1),
            new TagCount(VillagerTradeTags.TOOLSMITH_LEVEL_5, 1),
            new TagCount(VillagerTradeTags.WEAPONSMITH_LEVEL_3, 1),
            new TagCount(VillagerTradeTags.WEAPONSMITH_LEVEL_5, 1),
            new TagCount(VillagerTradeTags.ARMORER_LEVEL_4, 1),
            new TagCount(VillagerTradeTags.ARMORER_LEVEL_5, 1),
            new TagCount(VillagerTradeTags.FLETCHER_LEVEL_2, 1),
            new TagCount(VillagerTradeTags.FLETCHER_LEVEL_3, 1),
            new TagCount(VillagerTradeTags.FLETCHER_LEVEL_4, 1),
            new TagCount(VillagerTradeTags.LEATHERWORKER_LEVEL_3, 1),
            new TagCount(VillagerTradeTags.LEATHERWORKER_LEVEL_4, 1),
            new TagCount(VillagerTradeTags.LEATHERWORKER_LEVEL_5, 1),
            new TagCount(VillagerTradeTags.MASON_LEVEL_2, 1),
            new TagCount(VillagerTradeTags.MASON_LEVEL_5, 1),
            new TagCount(VillagerTradeTags.SHEPHERD_LEVEL_5, 1),
            new TagCount(VillagerTradeTags.BUTCHER_LEVEL_4, 1),
            new TagCount(VillagerTradeTags.CARTOGRAPHER_LEVEL_2, 1));

    private record TagCount(TagKey<VillagerTrade> tag, int ours) {}

    static {
        testFunctions.register("trades_are_registered", () -> TradeTests::tradesAreRegistered);
        testFunctions.register("trades_join_the_vanilla_tags", () -> TradeTests::tradesJoinTheVanillaTags);
        testFunctions.register("a_scroll_trade_offers_a_scroll", () -> TradeTests::scrollTradeOffersAScroll);
    }

    private static HolderLookup.RegistryLookup<VillagerTrade> trades(GameTestHelper helper) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
    }

    private static boolean isOurs(Holder<VillagerTrade> holder) {
        return holder.getKey() != null && TetraMod.MOD_ID.equals(holder.getKey().identifier().getNamespace());
    }

    /**
     * The data files parse and land in the registry.
     *
     * A file that fails its codec is logged and dropped, and the count is what catches that. It is
     * the number of files under data/tetra/villager_trade, so adding a trade means changing it.
     */
    private static void tradesAreRegistered(GameTestHelper helper) {
        long count = trades(helper).listElements().filter(TradeTests::isOurs).count();
        if (count != 25) {
            helper.fail("expected 25 tetra villager trades in the registry and found " + count
                    + ", so a trade file failed to parse or was never read");
            return;
        }

        helper.succeed();
    }

    /**
     * Each trade is in the tag that makes it reachable, and vanilla's own trades are still there.
     *
     * A trade in no tag is never offered by anyone, so registration alone proves nothing. The
     * second half matters just as much: these tags are vanilla's, and a file carrying "replace"
     * would take the profession over and silently delete its trades rather than joining them.
     */
    private static void tradesJoinTheVanillaTags(GameTestHelper helper) {
        HolderLookup.RegistryLookup<VillagerTrade> registry = trades(helper);

        for (TagCount entry : expected) {
            Optional<HolderSet.Named<VillagerTrade>> found = registry.get(entry.tag());
            if (found.isEmpty()) {
                helper.fail("the tag " + entry.tag().location() + " does not exist");
                return;
            }

            List<Holder<VillagerTrade>> all = found.get().stream().toList();
            long ours = all.stream().filter(TradeTests::isOurs).count();
            if (ours != entry.ours()) {
                helper.fail("expected " + entry.ours() + " tetra trades in " + entry.tag().location()
                        + " and found " + ours);
                return;
            }
            if (all.size() == ours) {
                helper.fail(entry.tag().location() + " holds only tetra trades, so the file replaced"
                        + " the vanilla tag instead of appending to it");
                return;
            }
        }

        helper.succeed();
    }

    /**
     * A trade builds the offer it is supposed to, components and all.
     *
     * The scroll trades are the ones worth checking. What they sell is not just an item id but a
     * scroll carrying its ScrollData, and a component patch that quietly failed to apply would
     * leave the villager selling a blank scroll that does nothing.
     */
    private static void scrollTradeOffersAScroll(GameTestHelper helper) {
        Identifier key = Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "toolsmith/2/hammer_efficiency_scroll");
        Optional<Holder.Reference<VillagerTrade>> found = trades(helper)
                .get(net.minecraft.resources.ResourceKey.create(Registries.VILLAGER_TRADE, key));
        if (found.isEmpty()) {
            helper.fail("the trade " + key + " is not registered");
            return;
        }

        LootParams params = new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
        MerchantOffer offer = found.get().value().getOffer(new LootContext.Builder(params).create(Optional.empty()));
        if (offer == null) {
            helper.fail("the trade refused to build an offer");
            return;
        }

        ItemStack cost = offer.getCostA();
        if (!cost.is(Items.EMERALD) || cost.getCount() != 4) {
            helper.fail("expected the scroll to cost 4 emeralds and it cost " + cost);
            return;
        }

        ItemStack result = offer.getResult();
        ScrollData data = ScrollData.read(result);
        if (!"hammer_efficiency".equals(data.key)) {
            helper.fail("the villager is selling a scroll with key " + data.key
                    + ", so the scroll_data component did not survive the trade file");
            return;
        }
        if (!data.craftingEffects.contains(Identifier.fromNamespaceAndPath(TetraMod.MOD_ID, "hammer_efficiency"))) {
            helper.fail("the scroll carries no hammer_efficiency effect, so it would do nothing");
            return;
        }

        helper.succeed();
    }
}
