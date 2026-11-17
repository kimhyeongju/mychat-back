package com.khj.playground.invest.service;

import com.khj.playground.common.event.UserWithdrawnEvent;
import com.khj.playground.invest.repository.InvestAccountRepository;
import com.khj.playground.invest.repository.StockCommentRepository;
import com.khj.playground.invest.repository.StockPostLikeRepository;
import com.khj.playground.invest.repository.StockPostRepository;
import com.khj.playground.invest.repository.WatchlistRepository;
import com.khj.playground.invest.repository.WeeklyResultRepository;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 회원이 탈퇴(또는 운영자가 삭제)하면 모의투자 데이터를 모두 지운다.
 * 이 리스너는 탈퇴 트랜잭션 안에서 동기로 실행되므로, 여기서 실패하면 탈퇴도 취소된다.
 *
 * - 계좌를 지우면 DB 의 ON DELETE CASCADE 로 보유 종목과 거래 내역이 함께 지워진다.
 * - 종목 토론의 글·댓글·좋아요도 지운다. 지운 글에 달린 다른 사람의 댓글은 함께 사라진다.
 * - 관심 종목과 주간 성적(포인트)도 지운다. 이미 정산된 주의 다른 사람 순위는 바뀌지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvestUserCleanup {

  private final InvestAccountRepository accountRepository;
  private final WatchlistRepository watchlistRepository;
  private final WeeklyResultRepository resultRepository;
  private final RankingService rankingService;
  private final ScoreService scoreService;
  private final StockPostRepository postRepository;
  private final StockCommentRepository commentRepository;
  private final StockPostLikeRepository likeRepository;

  @EventListener
  public void onUserWithdrawn(UserWithdrawnEvent event) {
    accountRepository.findByUserId(event.userId()).ifPresent(accountRepository::delete);
    int watched = watchlistRepository.deleteAllByUserId(event.userId());
    int results = resultRepository.deleteAllByUserId(event.userId());
    int discussion = deleteDiscussion(event);
    rankingService.invalidate();
    scoreService.invalidate();
    log.info(
      "탈퇴 회원의 모의투자 데이터 삭제 userId={} 관심종목={} 주간성적={} 토론글={}",
      event.userId(),
      watched,
      results,
      discussion
    );
  }

  /** 남의 글에 남긴 댓글·좋아요를 지운 뒤, 영향받은 글의 건수를 다시 맞춘다. */
  private int deleteDiscussion(UserWithdrawnEvent event) {
    Set<Long> affected = new HashSet<>(likeRepository.findPostIdsByUserId(event.userId()));
    affected.addAll(commentRepository.findPostIdsByUserId(event.userId()));
    int posts = postRepository.deleteAllByUserId(event.userId());
    likeRepository.deleteAllByUserId(event.userId());
    commentRepository.deleteAllByUserId(event.userId());
    for (Long postId : affected) {
      postRepository.recountLikes(postId);
      postRepository.recountComments(postId);
    }
    return posts;
  }
}
