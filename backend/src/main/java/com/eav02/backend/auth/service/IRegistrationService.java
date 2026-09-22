package com.eav02.backend.auth.service;

import com.eav02.backend.auth.dto.RegistrationRequest;
import com.eav02.backend.auth.dto.RegistrationResponse;

public interface IRegistrationService {

    RegistrationResponse register(RegistrationRequest request);
}