package se.mickelus.tetra.client.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.mickelus.tetra.data.DataManager;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedList;
import java.util.List;

/**
 * Keeps track of the baked modular item models so their layer caches can be dropped when module
 * data reloads.
 *
 * This used to be an IGeometryLoader and did the json reading as well. That half is gone: a model
 * type is a MapCodec registered into the item model type registry, so ModularItemModel.Unbaked
 * carries its own codec and this is only the cache bookkeeping.
 */
@ParametersAreNonnullByDefault
public class ModularModelLoader {

    private static final Logger logger = LogManager.getLogger();

    private static List<ModularItemModel> newModels = new LinkedList<>();
    private static List<ModularItemModel> models = new LinkedList<>();

    public static void init() {
        // module data is the last data store that contains model information
        DataManager.instance.moduleData.onReload(ModularModelLoader::clearCaches);
    }

    /**
     * Hack to shuffle models around since models are rebaked wholesale on a resource reload and
     * there's no context available to tell them apart, so that layer caches can be cleared when data
     * is reloaded.
     */
    private static void shuffle() {
        if (!newModels.isEmpty()) {
            models = newModels;
            newModels = new LinkedList<>();
        }
    }

    public synchronized static void clearCaches() {
        logger.info("Clearing model cache for {} items, let's get bakin'", models.size());
        models.forEach(ModularItemModel::clearCache);
        shuffle();
    }

    synchronized static void addModel(ModularItemModel model) {
        newModels.add(model);
    }
}
