package org.jetbrains.plugins.scala.packagesearch;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class PackageSearchSbtBundle extends DynamicBundle {
    @NonNls
    private static final String BUNDLE = "messages.PackageSearchSbtBundle";

    private static final PackageSearchSbtBundle INSTANCE = new PackageSearchSbtBundle();

    private PackageSearchSbtBundle() {
        super(BUNDLE);
    }

    @Nls
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
