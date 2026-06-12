package com.telemed.common.vo.whiteboard;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WhiteboardHistoryVO {

    private String roomId;

    private String source;

    private Long imageId;

    private Integer totalOps;

    private List<WhiteboardOpVO> operations;

    private LocalDateTime lastModified;
}
