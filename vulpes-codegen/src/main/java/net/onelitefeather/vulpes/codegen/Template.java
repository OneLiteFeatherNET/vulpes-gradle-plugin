package net.onelitefeather.vulpes.codegen;

import java.util.List;
import java.util.Set;
import net.onelitefeather.vulpes.format.ContentRelease;

/**
 * A generator profile: which code is generated for a content release and which library the code needs.
 */
public interface Template {

    /**
     * The name the build selects this template with, for example {@code minestom}.
     */
    String name();

    /**
     * The entity types this template generates code for. Content with entries of other types fails.
     */
    Set<EntityType> supportedTypes();

    /**
     * The libraries the plugin adds to the implementation configuration of the content project.
     */
    List<Dependency> runtimeDependencies();

    /**
     * Generates the sources of one content release in the given package.
     */
    List<GeneratedSource> generate(ContentRelease content, String packageName);
}
