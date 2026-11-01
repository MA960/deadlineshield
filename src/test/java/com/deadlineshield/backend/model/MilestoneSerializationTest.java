package com.deadlineshield.backend.model;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class MilestoneSerializationTest {

    @Test
    void doesNotSerializeMagicReviewToken() throws Exception {
        String serialized = new ObjectMapper().writeValueAsString(new Milestone());

        assertFalse(serialized.contains("token"));
    }
}
