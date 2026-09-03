package io.github.bnymndev.uuidulid.validation;

import io.github.bnymndev.uuidulid.Ulid;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validator behind {@link ValidUlid}. */
public class UlidValidator implements ConstraintValidator<ValidUlid, CharSequence> {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || Ulid.isValid(value);
    }
}
