package com.zigu.ziguwas.domains.chatparticipant.repository;

import com.zigu.ziguwas.domains.chatparticipant.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, String> {
}
