package com.filecollection.controller.dto;

import lombok.Data;

@Data
public class TaskResponse<T> {
    private int code;
    private String message;
    private T data;
    
    public static <T> TaskResponse<T> success(T data) {
        TaskResponse<T> response = new TaskResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }
    
    public static <T> TaskResponse<T> error(String message) {
        TaskResponse<T> response = new TaskResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }
}
