package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.OfflineEntryManifest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfflineEntryManifestRepository extends JpaRepository<OfflineEntryManifest, String> {
}