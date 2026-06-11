package com.telemed.web.controller;

import com.telemed.common.dto.mediasoup.ConsumerCreateDTO;
import com.telemed.common.dto.mediasoup.MediasoupNodeHeartbeatDTO;
import com.telemed.common.dto.mediasoup.MediasoupNodeRegisterDTO;
import com.telemed.common.dto.mediasoup.ProducerCreateDTO;
import com.telemed.common.dto.mediasoup.QualityReportDTO;
import com.telemed.common.dto.mediasoup.TransportCreateDTO;
import com.telemed.common.result.Result;
import com.telemed.common.vo.mediasoup.ConsumerVO;
import com.telemed.common.vo.mediasoup.MediasoupNodeVO;
import com.telemed.common.vo.mediasoup.NearestNodeVO;
import com.telemed.common.vo.mediasoup.ProducerVO;
import com.telemed.common.vo.mediasoup.QualityAdviceVO;
import com.telemed.common.vo.mediasoup.RouterRtpCapabilitiesVO;
import com.telemed.common.vo.mediasoup.TransportConnectVO;
import com.telemed.common.vo.mediasoup.TurnServerVO;
import com.telemed.model.entity.MediasoupNode;
import com.telemed.service.MediasoupNodeService;
import com.telemed.service.SignalingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mediasoup")
@RequiredArgsConstructor
public class MediasoupController {

    private final SignalingService signalingService;
    private final MediasoupNodeService mediasoupNodeService;

    @GetMapping("/node/nearest")
    public Result<NearestNodeVO> getNearestNode(
            @RequestParam(required = false) String clientIp,
            @RequestParam(required = false) String clientRegion,
            @RequestParam(required = false) String preferredRegion) {
        NearestNodeVO vo = signalingService.getNearestMediasoupNode(clientIp, clientRegion, preferredRegion);
        return Result.ok(vo);
    }

    @GetMapping("/node/list")
    public Result<List<MediasoupNodeVO>> listNodes() {
        List<MediasoupNodeVO> nodes = mediasoupNodeService.listOnlineNodes();
        return Result.ok(nodes);
    }

    @GetMapping("/turn-config")
    public Result<TurnServerVO> getTurnConfig() {
        TurnServerVO vo = signalingService.getTurnServerConfig();
        return Result.ok(vo);
    }

    @GetMapping("/router-capabilities")
    public Result<RouterRtpCapabilitiesVO> getRouterCapabilities(@RequestParam Long nodeId) {
        RouterRtpCapabilitiesVO vo = signalingService.getRouterRtpCapabilities(nodeId);
        return Result.ok(vo);
    }

    @PostMapping("/transport/create")
    public Result<TransportConnectVO> createTransport(
            @RequestParam Long nodeId,
            @RequestBody TransportCreateDTO dto) {
        TransportConnectVO vo = signalingService.createWebRtcTransport(nodeId, dto);
        return Result.ok(vo);
    }

    @PostMapping("/producer/create")
    public Result<ProducerVO> createProducer(
            @RequestParam Long nodeId,
            @RequestBody ProducerCreateDTO dto) {
        ProducerVO vo = signalingService.createProducer(nodeId, dto);
        return Result.ok(vo);
    }

    @PostMapping("/consumer/create")
    public Result<ConsumerVO> createConsumer(
            @RequestParam Long nodeId,
            @RequestBody ConsumerCreateDTO dto) {
        ConsumerVO vo = signalingService.createConsumer(nodeId, dto);
        return Result.ok(vo);
    }

    @PostMapping("/quality/report")
    public Result<QualityAdviceVO> reportQuality(@RequestBody QualityReportDTO dto) {
        QualityAdviceVO vo = signalingService.reportQualityAndGetAdvice(dto);
        return Result.ok(vo);
    }

    @PostMapping("/node/register")
    public Result<MediasoupNodeVO> registerNode(@RequestBody MediasoupNodeRegisterDTO dto) {
        MediasoupNode node = mediasoupNodeService.registerNode(dto);
        MediasoupNodeVO vo = convertToVO(node);
        return Result.ok(vo);
    }

    @PostMapping("/node/heartbeat")
    public Result<Void> heartbeat(@RequestBody MediasoupNodeHeartbeatDTO dto) {
        mediasoupNodeService.heartbeat(dto);
        return Result.ok();
    }

    private MediasoupNodeVO convertToVO(MediasoupNode node) {
        MediasoupNodeVO vo = new MediasoupNodeVO();
        vo.setId(node.getId());
        vo.setNodeName(node.getNodeName());
        vo.setNodeIp(node.getNodeIp());
        vo.setNodePort(node.getNodePort());
        vo.setHttpPort(node.getHttpPort());
        vo.setRegion(node.getRegion());
        vo.setWeight(node.getWeight());
        vo.setStatus(node.getStatus());
        vo.setCpuUsage(node.getCpuUsage());
        vo.setMemoryUsage(node.getMemoryUsage());
        vo.setActiveConsumers(node.getActiveConsumers());
        vo.setActiveProducers(node.getActiveProducers());
        vo.setLastHeartbeat(node.getLastHeartbeat());
        vo.setCreateTime(node.getCreateTime());
        vo.setUpdateTime(node.getUpdateTime());
        return vo;
    }
}
