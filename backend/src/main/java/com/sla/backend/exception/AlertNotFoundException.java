package com.sla.backend.exception;

public class AlertNotFoundException extends RuntimeException{

    public AlertNotFoundException(Long id){
        super("Alert not found for id : "+id);
    }
}
