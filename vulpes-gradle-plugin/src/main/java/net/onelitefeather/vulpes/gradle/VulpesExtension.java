package net.onelitefeather.vulpes.gradle;

import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

/**
 * The {@code vulpes} extension: package settings of the generated code.
 */
public abstract class VulpesExtension {

    private final NamedDomainObjectContainer<VulpesProjectSettings> projects;

    @Inject
    public VulpesExtension(ObjectFactory objects) {
        this.projects = objects.domainObjectContainer(VulpesProjectSettings.class,
                name -> objects.newInstance(VulpesProjectSettings.class, name));
    }

    /**
     * Java package of the code of the only content project. Fails when several content projects are declared.
     */
    public abstract Property<String> getPackageName();

    /**
     * The template that generates the code: {@code minestom} (default) or {@code keys}.
     */
    public abstract Property<String> getTemplate();

    /**
     * Settings of the content projects, by project name.
     */
    public NamedDomainObjectContainer<VulpesProjectSettings> getProjects() {
        return projects;
    }

    /**
     * Configures the settings of the content project {@code name}.
     */
    public void project(String name, Action<? super VulpesProjectSettings> action) {
        action.execute(projects.maybeCreate(name));
    }
}
