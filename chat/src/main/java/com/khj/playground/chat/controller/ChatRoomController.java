package com.khj.playground.chat.controller;

import com.khj.playground.chat.dto.ChatRoomResponse;
import com.khj.playground.chat.service.ChatRoomService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

  private final ChatRoomService chatRoomService;

  @GetMapping("/open")
  public List<ChatRoomResponse> listOpenRooms() {
    return chatRoomService.listOpenRooms();
  }

  @GetMapping("/open/{roomId}")
  public ChatRoomResponse getOpenRoom(@PathVariable("roomId") UUID roomId) {
    return chatRoomService.getOpenRoom(roomId);
  }
}
