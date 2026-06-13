package com.telemed.common.vo.asr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsrQualityIssueVO {

    private Long id;

    private Long reportId;

    private String issueType;

    private String severity;

    private String description;

    private String relatedText;

    private String suggestion;

    private Integer timelineStart;

    private Integer timelineEnd;

    private Boolean resolved;

    private String createTime;
}
