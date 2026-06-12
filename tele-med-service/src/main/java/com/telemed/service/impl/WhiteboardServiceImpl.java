package com.telemed.service.impl;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.common.constant.WhiteboardConstants;
import com.telemed.common.dto.whiteboard.WhiteboardOpDTO;
import com.telemed.common.dto.whiteboard.WhiteboardSnapshotDTO;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.vo.whiteboard.WhiteboardHistoryVO;
import com.telemed.common.vo.whiteboard.WhiteboardOpVO;
import com.telemed.model.entity.ConsultationConclusion;
import com.telemed.model.repository.ConsultationConclusionRepository;
import com.telemed.service.MinioService;
import com.telemed.service.WhiteboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhiteboardServiceImpl implements WhiteboardService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MinioService minioService;
    private final ObjectMapper objectMapper;

    @Override
    public void recordOp(WhiteboardOpDTO opDTO) {
        if (opDTO.getOpId() == null) {
            opDTO.setOpId(IdUtil.simpleUUID());
        }
        if (opDTO.getTimestamp() == null) {
            opDTO.setTimestamp(System.currentTimeMillis());
        }

        String key = buildOpsKey(opDTO.getRoomId(), opDTO.getSource(), opDTO.getImageId());

        try {
            String value = objectMapper.writeValueAsString(opDTO);
            double score = opDTO.getTimestamp().doubleValue();
            redisTemplate.opsForZSet().add(key, value, score);
            redisTemplate.expire(key, WhiteboardConstants.WHITEBOARD_OPS_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("记录白板操作: roomId={}, source={}, imageId={}, opId={}",
                    opDTO.getRoomId(), opDTO.getSource(), opDTO.getImageId(), opDTO.getOpId());
        } catch (Exception e) {
            log.error("记录白板操作失败", e);
        }
    }

    @Override
    public List<WhiteboardOpVO> getOpsByRange(String roomId, String source, Long imageId, long startScore, long endScore) {
        String key = buildOpsKey(roomId, source, imageId);
        Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                .rangeByScoreWithScores(key, startScore, endScore);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        return tuples.stream()
                .map(tuple -> parseOpTuple(tuple))
                .filter(op -> op != null)
                .collect(Collectors.toList());
    }

    @Override
    public WhiteboardHistoryVO getHistory(String roomId, String source, Long imageId, Integer limit) {
        String key = buildOpsKey(roomId, source, imageId);
        Long total = redisTemplate.opsForZSet().size(key);

        WhiteboardHistoryVO vo = new WhiteboardHistoryVO();
        vo.setRoomId(roomId);
        vo.setSource(source);
        vo.setImageId(imageId);
        vo.setTotalOps(total != null ? total.intValue() : 0);

        if (total == null || total == 0) {
            vo.setOperations(Collections.emptyList());
            return vo;
        }

        long end = -1;
        long start = limit != null ? -Math.min(limit, total) : Long.MIN_VALUE;

        Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet()
                .rangeWithScores(key, start, end);

        if (tuples != null && !tuples.isEmpty()) {
            List<WhiteboardOpVO> ops = tuples.stream()
                    .map(this::parseOpTuple)
                    .filter(op -> op != null)
                    .collect(Collectors.toList());
            vo.setOperations(ops);

            if (!ops.isEmpty()) {
                WhiteboardOpVO last = ops.get(ops.size() - 1);
                if (last.getTimestamp() != null) {
                    vo.setLastModified(LocalDateTime.now());
                }
            }
        } else {
            vo.setOperations(Collections.emptyList());
        }

        return vo;
    }

    @Override
    public void clearHistory(String roomId, String source, Long imageId, Long operatorId) {
        String key = buildOpsKey(roomId, source, imageId);
        redisTemplate.delete(key);
        String snapshotKey = buildSnapshotKey(roomId, source, imageId);
        redisTemplate.delete(snapshotKey);
        log.info("清除白板历史: roomId={}, source={}, imageId={}, operatorId={}",
                roomId, source, imageId, operatorId);
    }

    @Override
    public String saveSnapshot(WhiteboardSnapshotDTO snapshotDTO) {
        if (snapshotDTO.getSnapshotData() == null || snapshotDTO.getSnapshotData().isEmpty()) {
            throw new BusinessException("快照数据不能为空");
        }

        String snapshotKey = buildSnapshotKey(snapshotDTO.getRoomId(), snapshotDTO.getSource(), snapshotDTO.getImageId());
        redisTemplate.opsForValue().set(snapshotKey, snapshotDTO.getSnapshotData(), 24, TimeUnit.HOURS);

        String objectName = buildSnapshotObjectName(snapshotDTO);
        try {
            byte[] imageBytes = decodeBase64(snapshotDTO.getSnapshotData());
            String storedName = minioService.uploadBytes(
                    "tele-med-whiteboard",
                    objectName,
                    imageBytes,
                    "image/png"
            );

            if (snapshotDTO.getInsertToRecord() != null && snapshotDTO.getInsertToRecord()) {
                log.info("白板快照已保存，可插入电子病历: roomId={}, objectName={}",
                        snapshotDTO.getRoomId(), storedName);
            }

            return storedName;
        } catch (Exception e) {
            log.error("保存白板快照失败", e);
            throw new BusinessException("保存白板快照失败");
        }
    }

    @Override
    public byte[] getSnapshot(String roomId, String source, Long imageId) {
        String snapshotKey = buildSnapshotKey(roomId, source, imageId);
        Object cached = redisTemplate.opsForValue().get(snapshotKey);
        if (cached != null) {
            try {
                return decodeBase64(cached.toString());
            } catch (Exception e) {
                log.warn("解析快照缓存失败", e);
            }
        }
        throw new BusinessException("快照不存在");
    }

    @Override
    public void undo(String roomId, String source, Long imageId, Long operatorId) {
        // undo逻辑由前端通过广播实现，这里仅记录
        WhiteboardOpDTO opDTO = new WhiteboardOpDTO();
        opDTO.setOpId(IdUtil.simpleUUID());
        opDTO.setRoomId(roomId);
        opDTO.setSource(source);
        opDTO.setImageId(imageId);
        opDTO.setOperation(WhiteboardConstants.OPERATION_UNDO);
        opDTO.setOperatorId(operatorId);
        opDTO.setTimestamp(System.currentTimeMillis());
        recordOp(opDTO);
    }

    @Override
    public void redo(String roomId, String source, Long imageId, Long operatorId) {
        WhiteboardOpDTO opDTO = new WhiteboardOpDTO();
        opDTO.setOpId(IdUtil.simpleUUID());
        opDTO.setRoomId(roomId);
        opDTO.setSource(source);
        opDTO.setImageId(imageId);
        opDTO.setOperation(WhiteboardConstants.OPERATION_REDO);
        opDTO.setOperatorId(operatorId);
        opDTO.setTimestamp(System.currentTimeMillis());
        recordOp(opDTO);
    }

    private String buildOpsKey(String roomId, String source, Long imageId) {
        StringBuilder sb = new StringBuilder(WhiteboardConstants.WHITEBOARD_OPS_PREFIX);
        sb.append(roomId);
        sb.append(":").append(source);
        if (imageId != null) {
            sb.append(":").append(imageId);
        }
        return sb.toString();
    }

    private String buildSnapshotKey(String roomId, String source, Long imageId) {
        StringBuilder sb = new StringBuilder(WhiteboardConstants.WHITEBOARD_SNAPSHOT_PREFIX);
        sb.append(roomId);
        sb.append(":").append(source);
        if (imageId != null) {
            sb.append(":").append(imageId);
        }
        return sb.toString();
    }

    private String buildSnapshotObjectName(WhiteboardSnapshotDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("consultation/").append(dto.getRoomId()).append("/");
        sb.append(dto.getSource()).append("_");
        if (dto.getImageId() != null) {
            sb.append(dto.getImageId()).append("_");
        }
        sb.append(dto.getOperatorId()).append("_");
        sb.append(System.currentTimeMillis()).append(".png");
        return sb.toString();
    }

    private WhiteboardOpVO parseOpTuple(ZSetOperations.TypedTuple<Object> tuple) {
        try {
            Object value = tuple.getValue();
            if (value == null) return null;

            WhiteboardOpDTO dto = objectMapper.readValue(value.toString(), WhiteboardOpDTO.class);
            WhiteboardOpVO vo = new WhiteboardOpVO();
            BeanUtils.copyProperties(dto, vo);
            if (tuple.getScore() != null) {
                vo.setScore(tuple.getScore());
            }
            return vo;
        } catch (Exception e) {
            log.warn("解析白板操作记录失败", e);
            return null;
        }
    }

    private byte[] decodeBase64(String data) {
        String base64 = data;
        if (base64.contains(",")) {
            base64 = base64.substring(base64.indexOf(",") + 1);
        }
        return java.util.Base64.getDecoder().decode(base64);
    }
}
