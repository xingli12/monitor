package com.filecollection.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfigResponse {
    private long rateLimit;
    private int upstreamCount;
    private int downstreamCount;
}
