package com.telemed.web.controller;

import com.telemed.common.dto.whiteboard.WhiteboardOpDTO;
import com.telemed.common.dto.whiteboard.WhiteboardSnapshotDTO;
import com.telemed.common.result.Result;
import com.telemed.common.vo.whiteboard.WhiteboardHistoryVO;
import com.telemed.common.vo.whiteboard.WhiteboardOpVO;
import com.telemed.service.WhiteboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/whiteboard")
@RequiredArgsConstructor
public class WhiteboardController {

    private final WhiteboardService whiteboardService;

    @GetMapping("/history/{roomId}")
    public Result<WhiteboardHistoryVO> getHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "BLANK") String source,
            @RequestParam(required = false) Long imageId,
            @RequestParam(required = false, defaultValue = "200") Integer limit
    ) {
        WhiteboardHistoryVO history = whiteboardService.getHistory(roomId, source, imageId, limit);
        return Result.ok(history);
    }

    @GetMapping("/ops/{roomId}/range")
    public Result<List<WhiteboardOpVO>> getOpsByRange(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "BLANK") String source,
            @RequestParam(required = false) Long imageId,
            @RequestParam(required = false, defaultValue = "0") long startScore,
            @RequestParam(required = false, defaultValue = "9999999999999") long endScore
    ) {
        List<WhiteboardOpVO> ops = whiteboardService.getOpsByRange(roomId, source, imageId, startScore, endScore);
        return Result.ok(ops);
    }

    @PostMapping("/ops")
    public Result<Void> recordOp(@RequestBody WhiteboardOpDTO opDTO) {
        whiteboardService.recordOp(opDTO);
        return Result.ok();
    }

    @DeleteMapping("/history/{roomId}")
    public Result<Void> clearHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "BLANK") String source,
            @RequestParam(required = false) Long imageId,
            @RequestParam Long operatorId
    ) {
        whiteboardService.clearHistory(roomId, source, imageId, operatorId);
        return Result.ok();
    }

    @PostMapping("/snapshot")
    public Result<String> saveSnapshot(@RequestBody WhiteboardSnapshotDTO snapshotDTO) {
        String objectName = whiteboardService.saveSnapshot(snapshotDTO);
        return Result.ok(objectName);
    }

    @GetMapping("/snapshot/{roomId}")
    public ResponseEntity<byte[]> getSnapshot(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "BLANK") String source,
            @RequestParam(required = false) Long imageId
    ) {
        byte[] data = whiteboardService.getSnapshot(roomId, source, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(data);
    }

    @PostMapping("/undo/{roomId}")
    public Result<Void> undo(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "BLANK") String source,
            @RequestParam(required = false) Long imageId,
            @RequestParam Long operatorId
    ) {
        whiteboardService.undo(roomId, source, imageId, operatorId);
        return Result.ok();
    }

    @PostMapping("/redo/{roomId}")
    public Result<Void> redo(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "BLANK") String source,
            @RequestParam(required = false) Long imageId,
            @RequestParam Long operatorId
    ) {
        whiteboardService.redo(roomId, source, imageId, operatorId);
        return Result.ok();
    }
}
