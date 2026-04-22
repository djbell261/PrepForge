package com.derwin.prepforge.auth.service;

import com.derwin.prepforge.auth.dto.AuthRequest;
import com.derwin.prepforge.auth.dto.AuthResponse;
import com.derwin.prepforge.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(AuthRequest request);
}
