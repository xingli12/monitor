package com.filecollection.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExecuteRequest {
    private List<String> upstreamNames;
}
