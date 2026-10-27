package com.colab.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InvitacionRequest(@NotBlank @Email String email) {}
