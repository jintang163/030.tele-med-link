package com.telemed.common.vo.whiteboard;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WhiteboardOpVO {

    private String opId;

    private String roomId;

    private String source;

    private Long imageId;

    private String operation;

    private String toolType;

    private List<Map<String, Double>> points;

    private Map<String, Object> properties;

    private String color;

    private Double strokeWidth;

    private String text;

    private Long operatorId;

    private String operatorName;

    private Long timestamp;

    private Double score;
}
