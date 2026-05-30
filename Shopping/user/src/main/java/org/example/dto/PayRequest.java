package org.example.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record PayRequest(@JsonAlias("payChannel") Integer channel) {
}
