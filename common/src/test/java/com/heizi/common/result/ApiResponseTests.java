package com.heizi.common.result;

import com.heizi.common.enums.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTests {

    @Test
    void shouldBuildSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertEquals(ResultCode.SUCCESS.getCode(), response.getCode());
        assertEquals(ResultCode.SUCCESS.getMessage(), response.getMessage());
        assertEquals("ok", response.getData());
    }

    @Test
    void shouldBuildFailedResponse() {
        ApiResponse<Object> response = ApiResponse.fail(ResultCode.BAD_REQUEST);

        assertEquals(ResultCode.BAD_REQUEST.getCode(), response.getCode());
        assertEquals(ResultCode.BAD_REQUEST.getMessage(), response.getMessage());
        assertNull(response.getData());
    }

}
