package com.telemed.model.repository;

import com.telemed.model.entity.KnowledgeDisease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeDiseaseRepository extends JpaRepository<KnowledgeDisease, Long> {

    List<KnowledgeDisease> findByDepartmentAndStatus(String department, String status);

    List<KnowledgeDisease> findByStatus(String status);

    @Query("SELECT k FROM KnowledgeDisease k WHERE k.status = 'ACTIVE' AND (k.keywords LIKE %:keyword% OR k.symptoms LIKE %:keyword% OR k.diseaseName LIKE %:keyword%)")
    List<KnowledgeDisease> searchByKeyword(@Param("keyword") String keyword);

    KnowledgeDisease findByIcdCode(String icdCode);
}
