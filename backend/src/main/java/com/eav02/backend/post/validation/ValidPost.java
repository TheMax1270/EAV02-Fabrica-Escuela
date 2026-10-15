package com.eav02.backend.post.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PostRequestValidator.class)
public @interface ValidPost {
    String message() default "Publicacion invalida";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
