package net.onelitefeather.vulpes.codegen;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import net.onelitefeather.vulpes.format.Pack;

/**
 * Writes one {@code ResourcePackInfo} per resource pack of the release, and the list of all of them.
 */
final class ResourcePackGenerator {

    private static final ClassName RESOURCE_PACK_INFO = ClassName.get("net.kyori.adventure.resource", "ResourcePackInfo");
    private static final ClassName UUID_TYPE = ClassName.get(UUID.class);
    private static final ClassName URI_TYPE = ClassName.get(URI.class);

    private ResourcePackGenerator() {
    }

    /**
     * Generates {@code <Project>ResourcePacks}. The id of a pack is the name-based UUID of its SHA-1, so it stays the
     * same for the same pack.
     *
     * @throws CodegenException if two pack ids share a constant name
     */
    static GeneratedSource generate(GenerationContext context, List<Pack> packs) {
        JavaIdentifiers.Claims claims = new JavaIdentifiers.Claims();
        TypeSpec.Builder type = TypeSpec.classBuilder(context.prefix() + "ResourcePacks")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc(JavaSources.classJavadoc(context, "Resource packs of the Vulpes content project {@code "
                        + context.project() + "}."));
        List<String> names = packs.stream().map(pack -> {
            String name = claims.claim(JavaNames.constantName(pack.id()), pack.id());
            String id = UUID.nameUUIDFromBytes(pack.sha1().getBytes(UTF_8)).toString();
            type.addField(FieldSpec.builder(RESOURCE_PACK_INFO, name, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$T.resourcePackInfo($T.fromString($L), $T.create($L), $L)", RESOURCE_PACK_INFO,
                            UUID_TYPE, JavaLiterals.string(id), URI_TYPE, JavaLiterals.string(pack.url()),
                            JavaLiterals.string(pack.sha1()))
                    .build());
            return name;
        }).toList();
        CodeBlock all = names.isEmpty() ? CodeBlock.of("$T.of()", ClassName.get(List.class))
                : CodeBlock.of("$T.of($L)", ClassName.get(List.class), names.stream().collect(Collectors.joining(", ")));
        type.addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(List.class), RESOURCE_PACK_INFO), "ALL",
                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL).initializer(all).build());
        return JavaSources.render(context.packageName(), type.build());
    }
}
