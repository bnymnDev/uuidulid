package io.github.bnymndev.uuidulid.example.postgres;

import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps identifier problems to RFC 9457 problem details. Validation errors and unreadable bodies
 * are handled by Spring's own problem-details support ({@code spring.mvc.problemdetails.enabled}).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiErrorHandler {

    /** A path variable or parameter that is not even a well-formed ULID. */
    @ExceptionHandler(TypeMismatchException.class)
    ProblemDetail malformed(TypeMismatchException e) {
        String type = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "value";
        return problem(HttpStatus.BAD_REQUEST, "Invalid identifier",
                "'" + e.getValue() + "' is not a valid " + type);
    }

    /** Well-formed, but not one of ours. */
    @ExceptionHandler(InvalidPublicIdException.class)
    ProblemDetail invalid(InvalidPublicIdException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid identifier", e.getMessage());
    }

    @ExceptionHandler(NoteNotFoundException.class)
    ProblemDetail notFound(NoteNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Note not found", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
