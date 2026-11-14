package com.khj.playground.invest.service;

import com.khj.playground.invest.entity.InvestAccount;
import com.khj.playground.invest.repository.InvestAccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 계좌는 별도 가입 절차 없이 처음 투자 화면에 들어올 때 자동으로 열린다. */
@Service
public class InvestAccountService {

  private final InvestAccountRepository accountRepository;
  private final BigDecimal seedMoney;

  public InvestAccountService(
    InvestAccountRepository accountRepository,
    @Value("${invest.seed-money}") BigDecimal seedMoney
  ) {
    this.accountRepository = accountRepository;
    this.seedMoney = seedMoney.setScale(2, RoundingMode.HALF_UP);
  }

  @Transactional
  public InvestAccount getOrOpen(UUID userId) {
    return accountRepository
      .findByUserId(userId)
      .orElseGet(() ->
        accountRepository.save(InvestAccount.open(userId, seedMoney))
      );
  }

  /** 주문 처리용. 행 잠금을 걸고 가져온다. 계좌가 없으면 먼저 연다. */
  @Transactional
  public InvestAccount lockForTrade(UUID userId) {
    return accountRepository
      .findByUserIdForUpdate(userId)
      .orElseGet(() -> {
        // 첫 주문이 계좌 개설보다 먼저 들어온 경우. 저장 후 다시 잠금 조회한다.
        accountRepository.saveAndFlush(InvestAccount.open(userId, seedMoney));
        return accountRepository.findByUserIdForUpdate(userId).orElseThrow();
      });
  }
}
