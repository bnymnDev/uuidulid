package io.github.bnymndev.uuidulid.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated {@link CharSequence} must be a well-formed ULID.
 *
 * <p>Parsing follows {@link io.github.bnymndev.uuidulid.Ulid#isValid(CharSequence)}: 26 Crockford
 * base32 characters, case-insensitive, with the usual {@code I}/{@code L}/{@code O} aliases.
 * {@code null} is considered valid, as is customary for Bean Validation constraints; combine with
 * {@code @NotNull} if the value is required.
 *
 * <p>Meant for request fields that have to stay {@code String}. Where the field can be typed as
 * {@code Ulid}, prefer that: the Jackson module then rejects malformed input before validation
 * runs.
 *
 * <pre>{@code
 * public class MoveRequest {
 *     @NotNull @ValidUlid
 *     private String targetId;
 * }
 * }</pre>
 */
@Documented
@Constraint(validatedBy = UlidValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ValidUlid.List.class)
public @interface ValidUlid {

    String message() default "must be a valid ULID";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Allows several {@code @ValidUlid} annotations on the same element, e.g. for different groups. */
    @Documented
    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        ValidUlid[] value();
    }
}
