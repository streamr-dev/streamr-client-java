package com.streamr.client.protocol.message_layer;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class GroupKeyResponseTest {
    @Test
    void equalsContract() {
        EqualsVerifier.forClass(GroupKeyResponse.class).verify();
    }
}
