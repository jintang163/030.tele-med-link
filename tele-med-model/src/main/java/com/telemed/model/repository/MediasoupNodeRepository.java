package com.telemed.model.repository;

import com.telemed.model.entity.MediasoupNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediasoupNodeRepository extends JpaRepository<MediasoupNode, Long> {

    List<MediasoupNode> findByStatus(Integer status);

    List<MediasoupNode> findByRegion(String region);

    Optional<MediasoupNode> findByNodeIpAndNodePort(String ip, Integer port);

    List<MediasoupNode> findByStatusAndRegion(Integer status, String region);

    List<MediasoupNode> findAllByOrderByWeightDesc();

    List<MediasoupNode> findAllByStatusOrderByActiveConsumersAsc(Integer status);
}
