package com.banking.audit.repository;

import com.banking.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(String resourceType, String resourceId);

    boolean existsByActionAndResourceTypeAndResourceIdAndStatus(String action, String resourceType, String resourceId, String status);

    java.util.Optional<AuditLog> findFirstByActionAndResourceTypeAndResourceIdAndStatus(String action, String resourceType, String resourceId, String status);

    boolean existsByCorrelationIdAndActionAndResourceTypeAndResourceId(String correlationId, String action, String resourceType, String resourceId);

    java.util.Optional<AuditLog> findFirstByCorrelationIdAndActionAndResourceTypeAndResourceId(String correlationId, String action, String resourceType, String resourceId);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actor IS NULL OR a.actor = :actor) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:resourceType IS NULL OR a.resourceType = :resourceType)")
    Page<AuditLog> searchLogs(@Param("actor") String actor,
                              @Param("action") String action,
                              @Param("resourceType") String resourceType,
                              Pageable pageable);
}
