package com.serviceportal.manager.service

import org.springframework.stereotype.Service

/**
 * Versionamento sequencial (1 → 2 → 3) para integrations, contracts e validations.
 * Separado do VersioningService (SemVer) usado em workflows.
 */
@Service
class SequentialVersioningService {

    fun nextVersion(currentVersion: Int): Int = currentVersion + 1
}
