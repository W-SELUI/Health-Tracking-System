package com.group15.health_tracking_system.message.repository;

import com.group15.health_tracking_system.message.entity.Message;
import com.group15.health_tracking_system.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = {"sender", "recipient"})
    @Query("""
            select m
            from Message m
            where (
                m.sender.id = :userId
                and m.recipient.id = :otherUserId
            )
            or (
                m.sender.id = :otherUserId
                and m.recipient.id = :userId
            )
            order by m.createdAt asc, m.id asc
            """)
    List<Message> findConversation(
            @Param("userId") Long userId,
            @Param("otherUserId") Long otherUserId
    );

    @Query("""
            select u
            from User u
            where u.id in (
                select m.recipient.id
                from Message m
                where m.sender.id = :userId
            )
            or u.id in (
                select m.sender.id
                from Message m
                where m.recipient.id = :userId
            )
            order by u.firstName asc, u.lastName asc, u.id asc
            """)
    List<User> findConversationPartners(
            @Param("userId") Long userId
    );

    @Query("""
            select case when count(m) > 0 then true else false end
            from Message m
            where (
                m.sender.id = :userId
                and m.recipient.id = :otherUserId
            )
            or (
                m.sender.id = :otherUserId
                and m.recipient.id = :userId
            )
            """)
    boolean existsConversation(
            @Param("userId") Long userId,
            @Param("otherUserId") Long otherUserId
    );
}