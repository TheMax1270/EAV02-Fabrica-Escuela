package com.eav02.backend.post.validation;

import java.net.URI;

import com.eav02.backend.post.dto.PostRequest;
import com.eav02.backend.post.entity.PostType;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PostRequestValidator implements ConstraintValidator<ValidPost, PostRequest> {
    @Override
    public boolean isValid(PostRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;
        boolean valid = true;
        context.disableDefaultConstraintViolation();
        if (request.type() == PostType.TECHNICAL_DISCUSSION) {
            if (request.title() == null || request.title().isBlank()) {
                error(context, "title", "El titulo es obligatorio para una discusion tecnica");
                valid = false;
            }
            if (request.categories().isEmpty()) {
                error(context, "categories", "Indica al menos una categoria o tecnologia");
                valid = false;
            }
        }
        if (request.link() != null && !validLink(request.link())) {
            error(context, "link", "Debe ser una URL valida con http:// o https://");
            valid = false;
        }
        return valid;
    }

    private boolean validLink(String value) {
        try {
            var uri = URI.create(value);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException failure) {
            return false;
        }
    }

    private void error(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
    }
}
