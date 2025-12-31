package com.proyecto.StoreCollection.Exceptions;
public class MissingEmailConfigException extends RuntimeException {
    public MissingEmailConfigException(String message) {
        super(message);
    }
}