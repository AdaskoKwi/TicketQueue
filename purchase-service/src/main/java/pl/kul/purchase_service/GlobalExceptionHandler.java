package pl.kul.purchase_service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.kul.purchase_service.model.exception.PurchaseOfferExpiredException;
import pl.kul.purchase_service.model.exception.PurchaseOfferNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PurchaseOfferNotFoundException.class)
    public ResponseEntity<?> handleOfferNotFoundException(PurchaseOfferNotFoundException ex) {
        return ResponseEntity
                .status(404)
                .body(ex.getMessage());
    }

    @ExceptionHandler(PurchaseOfferExpiredException.class)
    public ResponseEntity<?> handleOfferExpiredException(PurchaseOfferExpiredException ex) {
        return ResponseEntity
                .status(410)
                .body(ex.getMessage());
    }
}
