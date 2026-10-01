package com.khj.playground.chat.service;

import com.khj.playground.chat.dto.DirectRoomResponse;
import com.khj.playground.chat.entity.ChatMessage;
import com.khj.playground.chat.entity.ChatParticipant;
import com.khj.playground.chat.entity.ChatRoom;
import com.khj.playground.chat.repository.ChatMessageRepository;
import com.khj.playground.chat.repository.ChatParticipantRepository;
import com.khj.playground.chat.repository.ChatRoomRepository;
import com.khj.playground.common.security.AuthenticatedUser;
import com.khj.playground.common.security.MemberLookup;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DirectChatService {

  private static final int PREVIEW_LENGTH = 40;

  private final ChatRoomRepository chatRoomRepository;
  private final ChatParticipantRepository participantRepository;
  private final ChatMessageRepository messageRepository;
  private final MemberLookup memberLookup;

  /** 내 DM 목록. 사이드바에 표시한다. */
  public List<DirectRoomResponse> listMyRooms(UUID myId) {
    return participantRepository
      .findMyRooms(myId)
      .stream()
      .map(participant -> toResponse(participant, myId))
      .toList();
  }

  /** 상대와의 DM 방을 가져오거나 없으면 만든다. */
  @Transactional
  public DirectRoomResponse startDirect(UUID myId, UUID targetUserId) {
    if (myId.equals(targetUserId)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "자기 자신과는 대화할 수 없습니다."
      );
    }

    memberLookup
      .findById(targetUserId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "상대방을 찾을 수 없습니다."
        )
      );

    String dmKey = buildDmKey(myId, targetUserId);

    ChatRoom room = chatRoomRepository
      .findByDmKey(dmKey)
      .orElseGet(() -> {
        ChatRoom created = chatRoomRepository.save(
          ChatRoom.createDirect(dmKey)
        );
        // 상대방도 미리 참여자로 등록해야 상대 쪽 목록에도 방이 보인다.
        participantRepository.save(ChatParticipant.create(created, myId));
        participantRepository.save(
          ChatParticipant.create(created, targetUserId)
        );
        return created;
      });

    ChatParticipant me = participantRepository
      .findByRoomIdAndUserId(room.getId(), myId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "참여 정보가 없습니다."
        )
      );

    return toResponse(me, myId);
  }

  /**
   * DM 방 접근 권한을 확인한다.
   * 참여자가 아니면 404를 반환해 방 존재 여부 자체를 숨긴다.
   */
  public ChatRoom requireParticipant(UUID roomId, UUID myId) {
    if (!participantRepository.existsByRoomIdAndUserId(roomId, myId)) {
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "존재하지 않는 방입니다."
      );
    }
    return chatRoomRepository
      .findById(roomId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "존재하지 않는 방입니다."
        )
      );
  }

  /** 방에 들어가거나 새 메시지를 확인했을 때 호출한다. */
  @Transactional
  public void markRead(UUID roomId, UUID myId) {
    ChatParticipant participant = participantRepository
      .findByRoomIdAndUserId(roomId, myId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "존재하지 않는 방입니다."
        )
      );

    messageRepository
      .findFirstByRoomIdOrderByIdDesc(roomId)
      .ifPresent(latest -> participant.markRead(latest.getId()));
  }

  /** 참여자 목록. WebSocket에서 상대에게 알림을 보낼 때 쓴다. */
  public List<UUID> findPartnerIds(UUID roomId, UUID myId) {
    return participantRepository
      .findByRoomIdAndUserIdNot(roomId, myId)
      .stream()
      .map(ChatParticipant::getUserId)
      .toList();
  }

  private DirectRoomResponse toResponse(ChatParticipant me, UUID myId) {
    ChatRoom room = me.getRoom();

    UUID partnerId = participantRepository
      .findByRoomIdAndUserIdNot(room.getId(), myId)
      .stream()
      .findFirst()
      .map(ChatParticipant::getUserId)
      .orElse(null);

    String partnerNickname = partnerId == null
      ? "(알 수 없음)"
      : memberLookup
        .findById(partnerId)
        .map(AuthenticatedUser::nickname)
        .orElse("(탈퇴한 사용자)");

    String preview = messageRepository
      .findFirstByRoomIdOrderByIdDesc(room.getId())
      .map(ChatMessage::getContent)
      .map(this::truncate)
      .orElse(null);

    long unread = messageRepository.countUnread(
      room.getId(),
      myId,
      me.getLastReadMessageId()
    );

    return new DirectRoomResponse(
      room.getId(),
      partnerId,
      partnerNickname,
      preview,
      room.getLastMessageAt(),
      unread
    );
  }

  private String truncate(String content) {
    return content.length() <= PREVIEW_LENGTH
      ? content
      : content.substring(0, PREVIEW_LENGTH) + "...";
  }

  /** UUID를 사전식으로 정렬해 두 사람이 항상 같은 키를 얻도록 한다. */
  private String buildDmKey(UUID a, UUID b) {
    return a.compareTo(b) <= 0 ? a + "_" + b : b + "_" + a;
  }
}
