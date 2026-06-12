package com.telemed.common.dto.whiteboard;

import lombok.Data;

@Data
public class WhiteboardSnapshotDTO {

    private String roomId;

    private String source;

    private Long imageId;

    private String snapshotData;

    private String format;

    private String fileName;

    private Long operatorId;

    private String operatorName;

    private Long consultationId;

    private Boolean insertToRecord;
}
