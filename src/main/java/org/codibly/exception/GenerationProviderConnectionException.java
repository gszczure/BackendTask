package org.codibly.exception;

import org.springframework.web.client.RestClientException;

public class GenerationProviderConnectionException extends RuntimeException {
    public GenerationProviderConnectionException(String message, RestClientException ex) {
        super(message, ex);
    }
}
