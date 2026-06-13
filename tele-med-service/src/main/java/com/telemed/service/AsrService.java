package com.telemed.service;

import com.telemed.common.dto.asr.AsrResultDTO;
import com.telemed.common.dto.asr.AsrTaskDTO;
import com.telemed.common.vo.asr.AsrQualityReportVO;

public interface AsrService {

    void createAsrTask(AsrTaskDTO taskDTO);

    AsrResultDTO executeAsrTranscription(AsrTaskDTO taskDTO);

    AsrQualityReportVO processConsultationAsrAndQuality(Long consultationId);

    AsrResultDTO getMockAsrResult(Long consultationId, Integer durationSeconds);
}
