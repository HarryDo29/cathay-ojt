package com.cathay.apigateway.enums;

public enum Status {
    FOUND,              // Tìm thấy (200)
    PATH_NOT_FOUND,     // Sai đường dẫn (404)
    METHOD_NOT_ALLOWED  // Đúng đường dẫn, sai Method (405)
}
