package com.cms.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeviceRegisterRequest(
        @NotBlank String pairingCode
) {}
