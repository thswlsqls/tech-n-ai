package com.tech.n.ai.datasource.mongodb.document;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * UserProfileDocument
 */
@Document(collection = "user_profiles")
@Getter
@Setter
public class UserProfileDocument {

    @Id
    private ObjectId id;

    @Field("user_tsid")
    @Indexed(unique = true)
    private String userTsid;

    @Field("user_id")
    @Indexed(unique = true)
    private String userId;

    @Field("username")
    @Indexed(unique = true)
    private String username;

    @Field("email")
    @Indexed(unique = true)
    private String email;

    @Field("profile_image_url")
    private String profileImageUrl;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("created_by")
    private String createdBy;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("updated_by")
    private String updatedBy;
}
