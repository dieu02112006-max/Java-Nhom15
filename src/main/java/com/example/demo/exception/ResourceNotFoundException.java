package com.example.demo.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " không tồn tại với id = " + id);
    }

    public ResourceNotFoundException(String resource, String reference) {
        super(resource + " không tồn tại với mã = " + reference);
    }
}
