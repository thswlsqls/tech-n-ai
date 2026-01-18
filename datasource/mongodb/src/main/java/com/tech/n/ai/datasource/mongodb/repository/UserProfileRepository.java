package com.tech.n.ai.datasource.mongodb.repository;

import com.tech.n.ai.datasource.mongodb.document.UserProfileDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserProfileRepository
 */
@Repository
public interface UserProfileRepository extends MongoRepository<UserProfileDocument, ObjectId> {
    Optional<UserProfileDocument> findByUserTsid(String userTsid);
    Optional<UserProfileDocument> findByUserId(String userId);
    Optional<UserProfileDocument> findByUsername(String username);
    Optional<UserProfileDocument> findByEmail(String email);
    void deleteByUserTsid(String userTsid);
}
