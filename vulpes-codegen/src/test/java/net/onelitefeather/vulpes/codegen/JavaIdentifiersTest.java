package net.onelitefeather.vulpes.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class JavaIdentifiersTest {

    @Test
    void constantNameReplacesDashAndDotWithUnderscore() {
        assertEquals("MY_ITEM_V2", JavaIdentifiers.constantName("my-item.v2"),
                "every character outside [A-Z0-9_] must become an underscore");
    }

    @Test
    void constantNameCollapsesRepeatedUnderscores() {
        assertEquals("A_B_C", JavaIdentifiers.constantName("a--b..c"), "repeated underscores must collapse");
    }

    @Test
    void constantNamePrefixesLeadingDigitWithUnderscore() {
        assertEquals("_2FAST", JavaIdentifiers.constantName("2fast"), "a leading digit needs an underscore before it");
    }

    @Test
    void constantNameUpperCasesKeywordsWithoutSuffix() {
        assertEquals("CLASS", JavaIdentifiers.constantName("class"), "upper case names are never Java keywords");
    }

    @Test
    void constantNameKeepsAlreadyValidName() {
        assertEquals("MY_SWORD", JavaIdentifiers.constantName("MY_SWORD"), "a valid name only changes its case");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-", ".", "__", "--.."})
    void constantNameRejectsKeysWithoutLetterOrDigit(String key) {
        assertThrows(CodegenException.class, () -> JavaIdentifiers.constantName(key),
                "a key without any letter or digit must not produce an identifier silently");
    }

    @Test
    void claimReturnsIdentifierWhenNoOtherKeyUsesIt() {
        JavaIdentifiers.Claims claims = new JavaIdentifiers.Claims();

        assertEquals("MY_ITEM", claims.claim("MY_ITEM", "my-item"), "the first key that claims a name receives it");
    }

    @Test
    void claimAcceptsSameKeyTwice() {
        JavaIdentifiers.Claims claims = new JavaIdentifiers.Claims();
        claims.claim("MY_ITEM", "my-item");

        assertEquals("MY_ITEM", claims.claim("MY_ITEM", "my-item"), "the same key twice is not a collision");
    }

    @Test
    void claimThrowsNamingBothKeysOnCollision() {
        JavaIdentifiers.Claims claims = new JavaIdentifiers.Claims();
        claims.claim("MY_ITEM", "my-item");

        CodegenException exception = assertThrows(CodegenException.class,
                () -> claims.claim("MY_ITEM", "my.item"), "two keys with one name must abort the generation");

        assertTrue(exception.getMessage().contains("my-item"), "message must name the first key: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("my.item"), "message must name the second key: " + exception.getMessage());
    }
}
