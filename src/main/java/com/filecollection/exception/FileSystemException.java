package com.filecollection.exception;

public class FileSystemException extends RuntimeException {
    
    public FileSystemException(String message) {
        super(message);
    }
    
    public FileSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
