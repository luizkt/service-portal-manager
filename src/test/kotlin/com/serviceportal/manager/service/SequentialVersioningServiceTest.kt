package com.serviceportal.manager.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SequentialVersioningServiceTest {

    private val service = SequentialVersioningService()

    @Test fun `version 1 bumps to 2`() = assertThat(service.nextVersion(1)).isEqualTo(2)
    @Test fun `version 5 bumps to 6`() = assertThat(service.nextVersion(5)).isEqualTo(6)
    @Test fun `version 99 bumps to 100`() = assertThat(service.nextVersion(99)).isEqualTo(100)
}
