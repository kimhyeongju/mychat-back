package com.khj.mychatback.api.chat.service;

import com.khj.mychatback.api.chat.dto.CreateLocationRoomRequest;
import com.khj.mychatback.api.chat.dto.DirectRoomSummaryResponse;
import com.khj.mychatback.api.chat.dto.JoinRoomResponse;
import com.khj.mychatback.api.chat.dto.RoomSummaryResponse;
import com.khj.mychatback.entity.jpa.ChatRoom;
import com.khj.mychatback.entity.jpa.ChatRoomMember;
import com.khj.mychatback.entity.jpa.User;
import com.khj.mychatback.enums.RoomType;
import com.khj.mychatback.repo.jpa.ChatRoomMemberRepository;
import com.khj.mychatback.repo.jpa.ChatRoomRepository;
import com.khj.mychatback.repo.jpa.UserRepository;
import com.khj.mychatback.utils.AnonymousNicknameGenerator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

  /** 이 시간 이내에 메시지가 있어야 "활성화된 방"으로 검색된다 */
  private static final long ACTIVE_MINUTES = 30;

  private final ChatRoomRepository chatRoomRepository;
  private final ChatRoomMemberRepository chatRoomMemberRepository;
  private final UserRepository userRepository;

  public List<RoomSummaryResponse> findNearbyActiveRooms(
    double latitude,
    double longitude
  ) {
    LocalDateTime activeSince = LocalDateTime
      .now()
      .minusMinutes(ACTIVE_MINUTES);
    return chatRoomRepository
      .findActiveLocationRoomsNear(latitude, longitude, activeSince)
      .stream()
      .map(RoomSummaryResponse::from)
      .toList();
  }

  /** 위치 기반 방을 새로 만들고, 만든 사람을 그 방의 첫 참여자로 등록한다. */
  @Transactional
  public JoinRoomResponse createLocationRoomAndJoin(
    CreateLocationRoomRequest request,
    String username
  ) {
    ChatRoom room = ChatRoom.createLocationRoom(
      request.latitude(),
      request.longitude(),
      request.radiusMeters()
    );
    chatRoomRepository.save(room);

    ChatRoomMember member = joinInternal(room, username);
    return JoinRoomResponse.from(member);
  }

  /** 이미 존재하는 방(반경 검색 결과 등)에 입장한다. */
  @Transactional
  public JoinRoomResponse joinRoom(Long roomId, String username) {
    ChatRoom room = chatRoomRepository
      .findById(roomId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "존재하지 않는 방입니다."
        )
      );

    ChatRoomMember member = joinInternal(room, username);
    return JoinRoomResponse.from(member);
  }

  /**
   * 회원간 1:1 DM 방을 가져오거나 없으면 새로 만든다.
   * 두 회원이 같은 방을 다시 찾을 수 있도록 dmKey(정렬된 "userId_userId")로 식별한다.
   */
  @Transactional
  public JoinRoomResponse getOrCreateDirectRoomAndJoin(
    String username,
    Long targetUserId
  ) {
    User me = userRepository
      .findByUsername(username)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "존재하지 않는 사용자입니다."
        )
      );
    User target = userRepository
      .findById(targetUserId)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "상대방을 찾을 수 없습니다."
        )
      );

    if (me.getId().equals(target.getId())) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "자기 자신과는 DM을 만들 수 없습니다."
      );
    }

    String dmKey = buildDmKey(me.getId(), target.getId());

    ChatRoom room = chatRoomRepository
      .findByDmKey(dmKey)
      .orElseGet(() -> {
        ChatRoom created = ChatRoom.createDirectRoom(dmKey);
        chatRoomRepository.save(created);
        // 상대방도 같은 방의 참여자로 미리 등록해둬야 상대방 쪽에서도 메시지를 주고받을 수 있다.
        chatRoomMemberRepository.save(ChatRoomMember.ofMember(created, target));
        return created;
      });

    ChatRoomMember myMembership = chatRoomMemberRepository
      .findByChatRoomAndUser(room, me)
      .orElseGet(() ->
        chatRoomMemberRepository.save(ChatRoomMember.ofMember(room, me))
      );

    return JoinRoomResponse.from(myMembership);
  }

  /** 로그인한 회원의 DM 방 목록을, 최근 활동순으로 상대방 닉네임과 함께 반환한다. */
  public List<DirectRoomSummaryResponse> listMyDirectRooms(String username) {
    User me = userRepository
      .findByUsername(username)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "존재하지 않는 사용자입니다."
        )
      );

    List<ChatRoomMember> myMemberships = chatRoomMemberRepository.findByUserAndChatRoom_TypeOrderByChatRoom_LastActivityAtDesc(
      me,
      RoomType.DIRECT
    );

    return myMemberships
      .stream()
      .map(myMembership -> {
        ChatRoom room = myMembership.getChatRoom();
        ChatRoomMember partner = chatRoomMemberRepository
          .findByChatRoomAndUserNot(room, me)
          .orElse(null);

        return new DirectRoomSummaryResponse(
          room.getId(),
          partner != null ? partner.getUser().getId() : null,
          partner != null ? partner.getNickname() : "(알 수 없음)",
          room.getLastActivityAt()
        );
      })
      .toList();
  }

  private ChatRoomMember joinInternal(ChatRoom room, String username) {
    if (username != null) {
      User user = userRepository
        .findByUsername(username)
        .orElseThrow(() ->
          new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "존재하지 않는 사용자입니다."
          )
        );

      return chatRoomMemberRepository
        .findByChatRoomAndUser(room, user)
        .orElseGet(() ->
          chatRoomMemberRepository.save(ChatRoomMember.ofMember(room, user))
        );
    }

    // 익명 참여: 매 입장마다 새로운 익명 신원을 발급한다.
    String anonymousId = UUID.randomUUID().toString();
    String nickname = uniqueAnonymousNickname(room);
    return chatRoomMemberRepository.save(
      ChatRoomMember.ofAnonymous(room, anonymousId, nickname)
    );
  }

  private String uniqueAnonymousNickname(ChatRoom room) {
    String nickname = AnonymousNicknameGenerator.generate();
    int suffix = 1;
    while (
      chatRoomMemberRepository.existsByChatRoomAndNickname(room, nickname)
    ) {
      nickname = AnonymousNicknameGenerator.generateWithSuffix(suffix++);
    }
    return nickname;
  }

  private String buildDmKey(Long userIdA, Long userIdB) {
    long min = Math.min(userIdA, userIdB);
    long max = Math.max(userIdA, userIdB);
    return min + "_" + max;
  }
}
