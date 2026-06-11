package com.telemed.service.impl;

import com.telemed.common.constant.MediasoupConstants;
import com.telemed.common.dto.mediasoup.MediasoupNodeHeartbeatDTO;
import com.telemed.common.dto.mediasoup.MediasoupNodeRegisterDTO;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.vo.mediasoup.MediasoupNodeVO;
import com.telemed.common.vo.mediasoup.NearestNodeVO;
import com.telemed.model.entity.MediasoupNode;
import com.telemed.model.repository.MediasoupNodeRepository;
import com.telemed.service.MediasoupNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediasoupNodeServiceImpl implements MediasoupNodeService {

    private final MediasoupNodeRepository mediasoupNodeRepository;

    @Override
    @Transactional
    public MediasoupNode registerNode(MediasoupNodeRegisterDTO dto) {
        Optional<MediasoupNode> existing = mediasoupNodeRepository.findByNodeIpAndNodePort(
                dto.getNodeIp(), dto.getNodePort());

        if (existing.isPresent()) {
            MediasoupNode node = existing.get();
            node.setNodeName(dto.getNodeName());
            node.setNodeUrl(dto.getNodeUrl());
            node.setRegion(dto.getRegion());
            node.setWeight(dto.getWeight() != null ? dto.getWeight() : 100);
            node.setStatus(MediasoupConstants.NODE_STATUS_ONLINE);
            node.setLastHeartbeat(LocalDateTime.now());
            return mediasoupNodeRepository.save(node);
        }

        MediasoupNode node = new MediasoupNode();
        node.setNodeName(dto.getNodeName());
        node.setNodeIp(dto.getNodeIp());
        node.setNodePort(dto.getNodePort());
        node.setNodeUrl(dto.getNodeUrl());
        node.setRegion(dto.getRegion());
        node.setWeight(dto.getWeight() != null ? dto.getWeight() : 100);
        node.setStatus(MediasoupConstants.NODE_STATUS_ONLINE);
        node.setCpuUsage(0.0);
        node.setMemoryUsage(0.0);
        node.setActiveConsumers(0);
        node.setActiveProducers(0);
        node.setLastHeartbeat(LocalDateTime.now());
        return mediasoupNodeRepository.save(node);
    }

    @Override
    @Transactional
    public void heartbeat(MediasoupNodeHeartbeatDTO dto) {
        MediasoupNode node = null;

        if (dto.getNodeId() != null) {
            node = mediasoupNodeRepository.findById(dto.getNodeId()).orElse(null);
        }

        if (node == null && dto.getNodeIp() != null && dto.getNodePort() != null) {
            node = mediasoupNodeRepository.findByNodeIpAndNodePort(
                    dto.getNodeIp(), dto.getNodePort()).orElse(null);
        }

        if (node == null) {
            throw new BusinessException("节点不存在，请先注册");
        }

        if (dto.getCpuUsage() != null) {
            node.setCpuUsage(dto.getCpuUsage());
        }
        if (dto.getMemoryUsage() != null) {
            node.setMemoryUsage(dto.getMemoryUsage());
        }
        if (dto.getActiveConsumers() != null) {
            node.setActiveConsumers(dto.getActiveConsumers());
        }
        if (dto.getActiveProducers() != null) {
            node.setActiveProducers(dto.getActiveProducers());
        }
        node.setLastHeartbeat(LocalDateTime.now());
        node.setStatus(MediasoupConstants.NODE_STATUS_ONLINE);

        mediasoupNodeRepository.save(node);
    }

    @Override
    public List<MediasoupNodeVO> listOnlineNodes() {
        List<MediasoupNode> nodes = mediasoupNodeRepository.findByStatus(
                MediasoupConstants.NODE_STATUS_ONLINE);
        return nodes.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<MediasoupNodeVO> listNodesByRegion(String region) {
        List<MediasoupNode> nodes = mediasoupNodeRepository.findByStatusAndRegion(
                MediasoupConstants.NODE_STATUS_ONLINE, region);
        return nodes.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public NearestNodeVO getNearestNode(String clientIp, String clientRegion, String preferredRegion) {
        List<MediasoupNode> candidates = new ArrayList<>();

        if (preferredRegion != null && !preferredRegion.isEmpty()) {
            candidates = mediasoupNodeRepository.findByStatusAndRegion(
                    MediasoupConstants.NODE_STATUS_ONLINE, preferredRegion);
        }

        if (candidates.isEmpty() && clientRegion != null && !clientRegion.isEmpty()) {
            candidates = mediasoupNodeRepository.findByStatusAndRegion(
                    MediasoupConstants.NODE_STATUS_ONLINE, clientRegion);
        }

        if (candidates.isEmpty()) {
            candidates = mediasoupNodeRepository.findByStatus(
                    MediasoupConstants.NODE_STATUS_ONLINE);
        }

        if (candidates.isEmpty()) {
            throw new BusinessException("没有可用的在线节点");
        }

        MediasoupNode best = candidates.stream()
                .sorted(Comparator.comparingInt(MediasoupNode::getActiveConsumers))
                .findFirst()
                .orElse(candidates.get(0));

        NearestNodeVO vo = new NearestNodeVO();
        vo.setNodeId(best.getId());
        vo.setNodeName(best.getNodeName());
        vo.setNodeIp(best.getNodeIp());
        vo.setNodePort(best.getNodePort());
        vo.setNodeUrl(best.getNodeUrl());
        vo.setRegion(best.getRegion());
        vo.setLatencyMs(50);
        return vo;
    }

    @Override
    @Transactional
    public void updateNodeStatus(Long nodeId, Integer status) {
        MediasoupNode node = mediasoupNodeRepository.findById(nodeId)
                .orElseThrow(() -> new BusinessException("节点不存在"));
        node.setStatus(status);
        mediasoupNodeRepository.save(node);
    }

    @Override
    @Transactional
    public void offlineStaleNodes() {
        LocalDateTime threshold = LocalDateTime.now()
                .minusSeconds(MediasoupConstants.HEARTBEAT_TIMEOUT_SECONDS);
        List<MediasoupNode> onlineNodes = mediasoupNodeRepository.findByStatus(
                MediasoupConstants.NODE_STATUS_ONLINE);

        int count = 0;
        for (MediasoupNode node : onlineNodes) {
            if (node.getLastHeartbeat() == null || node.getLastHeartbeat().isBefore(threshold)) {
                node.setStatus(MediasoupConstants.NODE_STATUS_OFFLINE);
                mediasoupNodeRepository.save(node);
                count++;
                log.warn("节点 {} ({}:{}) 已超时下线", node.getNodeName(), node.getNodeIp(), node.getNodePort());
            }
        }
        if (count > 0) {
            log.info("已将 {} 个超时节点标记为离线", count);
        }
    }

    @Override
    public MediasoupNode pickBestNodeForRoom(String region) {
        List<MediasoupNode> candidates;
        if (region != null && !region.isEmpty()) {
            candidates = mediasoupNodeRepository.findByStatusAndRegion(
                    MediasoupConstants.NODE_STATUS_ONLINE, region);
        } else {
            candidates = mediasoupNodeRepository.findByStatus(
                    MediasoupConstants.NODE_STATUS_ONLINE);
        }

        if (candidates.isEmpty()) {
            throw new BusinessException("没有可用的在线节点");
        }

        return candidates.stream()
                .sorted(Comparator.comparingInt(MediasoupNode::getActiveConsumers)
                        .thenComparing((n1, n2) -> Integer.compare(
                                n2.getWeight() != null ? n2.getWeight() : 0,
                                n1.getWeight() != null ? n1.getWeight() : 0)))
                .findFirst()
                .orElse(candidates.get(0));
    }

    @Override
    public void healthCheck() {
        log.info("开始执行 Mediasoup 节点健康检查");
        offlineStaleNodes();
        List<MediasoupNode> onlineNodes = mediasoupNodeRepository.findByStatus(
                MediasoupConstants.NODE_STATUS_ONLINE);
        log.info("节点健康检查完成，当前在线节点数量: {}", onlineNodes.size());
    }

    private MediasoupNodeVO convertToVO(MediasoupNode node) {
        MediasoupNodeVO vo = new MediasoupNodeVO();
        vo.setId(node.getId());
        vo.setNodeName(node.getNodeName());
        vo.setNodeIp(node.getNodeIp());
        vo.setNodePort(node.getNodePort());
        vo.setNodeUrl(node.getNodeUrl());
        vo.setRegion(node.getRegion());
        vo.setStatus(node.getStatus());
        vo.setWeight(node.getWeight());
        vo.setCpuUsage(node.getCpuUsage());
        vo.setMemoryUsage(node.getMemoryUsage());
        vo.setActiveConsumers(node.getActiveConsumers());
        vo.setActiveProducers(node.getActiveProducers());
        vo.setLastHeartbeat(node.getLastHeartbeat());
        vo.setCreateTime(node.getCreateTime());
        return vo;
    }
}
