package com.telemed.service;

public interface VideoTranscodeService {

    void mergeAndTranscodeToHls(Long recordingId);

    void processRecordingTranscode(Long recordingId);
}
