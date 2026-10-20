package com.sablednah.zombiemod.client.jei;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sablednah.zombiemod.client.MobDropsState;
import com.sablednah.zombiemod.net.MobDropsPayload;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

import net.minecraft.resources.Identifier;

/**
 * JEI's "Mob Drops" category. JEI finds this by its annotation and loads it only when JEI is
 * installed, so it is the one place {@code mezz.jei} may be imported - nothing else in the mod names
 * a JEI class, and without JEI this never loads.
 *
 * <p><b>Recipes arrive after JEI has started</b>, because they come from the server: the full set at
 * login and after {@code /reload}, and one entry at a time as a player makes first kills. So none
 * are registered up front. They are added to the live recipe manager as {@link MobDropsState}
 * reports them, and the whole current set is added again whenever JEI rebuilds its runtime, which
 * it does on every resource or recipe reload and which discards anything added to the last one.
 *
 * <p>JEI has no "remove", only hide. A hidden recipe that comes back - a reload that changed nothing
 * about a mob - is unhidden rather than added twice.
 */
@JeiPlugin
public final class MobDropsJeiPlugin implements IModPlugin {

    private IRecipeManager recipes;
    private final Set<MobDropsPayload.Entry> hidden = new HashSet<>();

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath("zombiemod", "mob_drops");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new MobDropsCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        recipes = runtime.getRecipeManager();
        hidden.clear();
        List<MobDropsPayload.Entry> now = MobDropsState.entries();
        if (!now.isEmpty()) {
            recipes.addRecipes(MobDropsCategory.TYPE, now);
        }
        MobDropsState.listen(this::changed);
    }

    @Override
    public void onRuntimeUnavailable() {
        MobDropsState.listen(null);
        recipes = null;
        hidden.clear();
    }

    private void changed(MobDropsState.Change change) {
        if (recipes == null) {
            return;
        }
        if (!change.removed().isEmpty()) {
            recipes.hideRecipes(MobDropsCategory.TYPE, change.removed());
            hidden.addAll(change.removed());
        }
        List<MobDropsPayload.Entry> back = new ArrayList<>();
        List<MobDropsPayload.Entry> fresh = new ArrayList<>();
        for (MobDropsPayload.Entry e : change.added()) {
            (hidden.remove(e) ? back : fresh).add(e);
        }
        if (!back.isEmpty()) {
            recipes.unhideRecipes(MobDropsCategory.TYPE, back);
        }
        if (!fresh.isEmpty()) {
            recipes.addRecipes(MobDropsCategory.TYPE, fresh);
        }
    }
}
