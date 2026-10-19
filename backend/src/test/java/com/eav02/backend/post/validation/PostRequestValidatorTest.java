package com.eav02.backend.post.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.eav02.backend.post.dto.PostRequest;
import com.eav02.backend.post.entity.PostType;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class PostRequestValidatorTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<String> invalidFields(PostRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath).map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void acceptsAGeneralPostWithoutTitleOrCategories() {
        assertTrue(invalidFields(new PostRequest(null, "contenido")).isEmpty());
    }

    @Test
    void acceptsACompleteTechnicalDiscussion() {
        var request = new PostRequest("Titulo", "contenido", PostType.TECHNICAL_DISCUSSION,
                "https://example.com/doc", List.of("Java"));
        assertTrue(invalidFields(request).isEmpty());
    }

    @Test
    void technicalDiscussionRequiresTitleAndCategories() {
        var request = new PostRequest(" ", "contenido", PostType.TECHNICAL_DISCUSSION, null, List.of());
        assertEquals(Set.of("title", "categories"), invalidFields(request));
    }

    @Test
    void technicalDiscussionRequiresAtLeastACategoryEvenWithTitle() {
        var request = new PostRequest("Titulo", "contenido", PostType.TECHNICAL_DISCUSSION, null, null);
        assertEquals(Set.of("categories"), invalidFields(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/file",
            "javascript:alert(1)",
            "https://user:secret@example.com",
            "https:///sin-host",
            "no es una url",
            "example.com"
    })
    void rejectsInvalidLinks(String link) {
        var request = new PostRequest("Titulo", "contenido", PostType.GENERAL, link, List.of());
        assertEquals(Set.of("link"), invalidFields(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://example.com", "https://example.com/a?b=c#d"})
    void acceptsHttpAndHttpsLinks(String link) {
        var request = new PostRequest("Titulo", "contenido", PostType.GENERAL, link, List.of());
        assertTrue(invalidFields(request).isEmpty());
    }

    @Test
    void aNullRequestIsConsideredValidByThisConstraint() {
        assertTrue(new PostRequestValidator().isValid(null, null));
    }
}
