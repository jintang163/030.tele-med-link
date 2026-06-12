package com.telemed.service;

import com.telemed.common.dto.whiteboard.WhiteboardOpDTO;
import com.telemed.common.dto.whiteboard.WhiteboardSnapshotDTO;
import com.telemed.common.vo.whiteboard.WhiteboardHistoryVO;
import com.telemed.common.vo.whiteboard.WhiteboardOpVO;

import java.util.List;

public interface WhiteboardService {

    void recordOp(WhiteboardOpDTO opDTO);

    List<WhiteboardOpVO> getOpsByRange(String roomId, String source, Long imageId, long startScore, long endScore);

    WhiteboardHistoryVO getHistory(String roomId, String source, Long imageId, Integer limit);

    void clearHistory(String roomId, String source, Long imageId, Long operatorId);

    String saveSnapshot(WhiteboardSnapshotDTO snapshotDTO);

    byte[] getSnapshot(String roomId, String source, Long imageId);

    void undo(String roomId, String source, Long imageId, Long operatorId);

    void redo(String roomId, String source, Long imageId, Long operatorId);
}
