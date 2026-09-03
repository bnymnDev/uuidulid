package io.github.bnymndev.uuidulid.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ValidUlidTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @ParameterizedTest
    @ValueSource(strings = {"01ARZ3NDEKTSV4RRFFQ69G5FAV", "01arz3ndektsv4rrffq69g5fav", "0lARZ3NDEKTSV4RRFFQ69G5FAV"})
    void acceptsWellFormedUlids(String value) {
        assertThat(VALIDATOR.validate(new Request(value))).isEmpty();
    }

    @Test
    void acceptsNullLikeOtherConstraints() {
        assertThat(VALIDATOR.validate(new Request(null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-ulid", "01ARZ3NDEKTSV4RRFFQ69G5FA", "8ZZZZZZZZZZZZZZZZZZZZZZZZZ", "01ARZ3NDEKTSV4RRFFQ69G5FAU"})
    void rejectsMalformedValues(String value) {
        Set<ConstraintViolation<Request>> violations = VALIDATOR.validate(new Request(value));

        assertThat(violations).hasSize(1);
        ConstraintViolation<Request> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("id");
        assertThat(violation.getMessage()).isEqualTo("must be a valid ULID");
    }

    @Test
    void messageCanBeCustomised() {
        Set<ConstraintViolation<CustomMessage>> violations = VALIDATOR.validate(new CustomMessage("nope"));

        assertThat(violations).extracting(ConstraintViolation::getMessage).containsExactly("bad id");
    }

    static final class Request {
        @ValidUlid
        final String id;

        Request(String id) {
            this.id = id;
        }
    }

    static final class CustomMessage {
        @ValidUlid(message = "bad id")
        final String id;

        CustomMessage(String id) {
            this.id = id;
        }
    }
}
