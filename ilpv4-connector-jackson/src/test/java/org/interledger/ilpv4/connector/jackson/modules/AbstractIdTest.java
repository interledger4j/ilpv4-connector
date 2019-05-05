package com.ripple.ripplenet.idms.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ripple.service.identitymanagement.api.OrganizationId;
import com.ripple.service.identitymanagement.api.ProductId;
import com.ripple.service.identitymanagement.api.RoleId;
import com.ripple.service.identitymanagement.api.UserId;
import org.immutables.value.Value.Immutable;
import org.testng.annotations.BeforeMethod;

/**
 * Abstract class that holds common functionality for testing Id (de)serialization using Jackson.
 */
public abstract class AbstractIdTest {

    protected ObjectMapper objectMapper;

    @BeforeMethod
    public void setup() {
        objectMapper = new ObjectMapper()
            .registerModule(new OrganizationIdModule())
            .registerModule(new UserIdModule())
            .registerModule(new ProductIdModule())
            .registerModule(new RoleIdModule());
    }

    @Immutable
    @JsonSerialize(as = ImmutableOrganizationIdContainer.class)
    @JsonDeserialize(as = ImmutableOrganizationIdContainer.class)
    public interface OrganizationIdContainer {

        @JsonProperty("organization_id")
        OrganizationId getOrganizationId();
    }

    @Immutable
    @JsonSerialize(as = ImmutableUserIdContainer.class)
    @JsonDeserialize(as = ImmutableUserIdContainer.class)
    public interface UserIdContainer {

        @JsonProperty("user_id")
        UserId getUserId();
    }

    @Immutable
    @JsonSerialize(as = ImmutableProductIdContainer.class)
    @JsonDeserialize(as = ImmutableProductIdContainer.class)
    public interface ProductIdContainer {

        @JsonProperty("product_id")
        ProductId getProductId();
    }

    @Immutable
    @JsonSerialize(as = ImmutableRoleIdContainer.class)
    @JsonDeserialize(as = ImmutableRoleIdContainer.class)
    public interface RoleIdContainer {

        @JsonProperty("role_id")
        RoleId getRoleId();
    }

}
