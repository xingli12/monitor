package com.filecollection.controller.dto;

import lombok.Data;

@Data
public class TaskResponse<T> {
    
    private static final int SUCCESS_CODE = 200;
    private static final int ERROR_CODE = 500;
    private static final String SUCCESS_MESSAGE = "success";
    
    private int code;
    private String message;
    private T data;
    
    public static <T> TaskResponse<T> success(T data) {
        TaskResponse<T> response = new TaskResponse<>();
        response.setCode(SUCCESS_CODE);
        response.setMessage(SUCCESS_MESSAGE);
        response.setData(data);
        return response;
    }
    
    public static <T> TaskResponse<T> error(String message) {
        TaskResponse<T> response = new TaskResponse<>();
        response.setCode(ERROR_CODE);
        response.setMessage(message);
        return response;
    }
}
