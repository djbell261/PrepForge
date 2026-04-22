package com.derwin.prepforge.dashboard.repository;

import com.derwin.prepforge.dashboard.entity.DashboardSnapshot;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DashboardSnapshotRepository extends JpaRepository<DashboardSnapshot, UUID> {
}
