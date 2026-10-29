package net.onelitefeather.vulpes.gradle;

import javax.inject.Inject;
import org.gradle.api.Named;
import org.gradle.api.provider.Property;

/**
 * Settings of one content project, declared with {@code vulpes { project("<name>") { … } }}.
 */
public abstract class VulpesProjectSettings implements Named {

    private final String name;

    @Inject
    public VulpesProjectSettings(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Java package of this content project. Overrides the default package and {@code vulpes.packageName}.
     */
    public abstract Property<String> getPackageName();
}
