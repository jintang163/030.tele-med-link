package com.telemed.service;

import com.telemed.common.dto.mediasoup.MediasoupNodeHeartbeatDTO;
import com.telemed.common.dto.mediasoup.MediasoupNodeRegisterDTO;
import com.telemed.common.vo.mediasoup.MediasoupNodeVO;
import com.telemed.common.vo.mediasoup.NearestNodeVO;
import com.telemed.model.entity.MediasoupNode;

import java.util.List;

public interface MediasoupNodeService {

    MediasoupNode registerNode(MediasoupNodeRegisterDTO dto);

    void heartbeat(MediasoupNodeHeartbeatDTO dto);

    List<MediasoupNodeVO> listOnlineNodes();

    List<MediasoupNodeVO> listNodesByRegion(String region);

    NearestNodeVO getNearestNode(String clientIp, String clientRegion, String preferredRegion);

    void updateNodeStatus(Long nodeId, Integer status);

    void offlineStaleNodes();

    MediasoupNode pickBestNodeForRoom(String region);

    void healthCheck();
}
