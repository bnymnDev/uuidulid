package io.github.bnymndev.uuidulid.example.postgres;

/** A syntactically valid identifier that cannot have been issued by this service. */
class InvalidPublicIdException extends RuntimeException {

    InvalidPublicIdException(String value) {
        super("'" + value + "' is not an identifier issued by this service");
    }
}
