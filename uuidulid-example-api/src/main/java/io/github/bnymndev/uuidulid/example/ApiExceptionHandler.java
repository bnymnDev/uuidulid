package io.github.bnymndev.uuidulid.example;

import io.github.bnymndev.uuidulid.Ulid;
import io.github.bnymndev.uuidulid.example.note.NoteNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns identifier problems into RFC 9457 problem details. Everything else is covered by
 * Spring Boot's own problem-details support ({@code spring.mvc.problemdetails.enabled=true}).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail invalidArgument(MethodArgumentTypeMismatchException ex) {
        String detail = Ulid.class.equals(ex.getRequiredType())
                ? "'" + ex.getValue() + "' is not a valid ULID"
                : "'" + ex.getValue() + "' is not valid for parameter '" + ex.getName() + "'";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid identifier");
        return problem;
    }

    @ExceptionHandler(NoteNotFoundException.class)
    ProblemDetail notFound(NoteNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Note not found");
        return problem;
    }
}
