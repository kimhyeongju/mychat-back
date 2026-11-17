package com.khj.playground.invest.controller;

import com.khj.playground.invest.dto.NewsItemResponse;
import com.khj.playground.invest.service.StockNewsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invest")
@RequiredArgsConstructor
public class StockNewsController {

  private final StockNewsService newsService;

  /** 비로그인도 볼 수 있다. 최신순, 최대 15건. */
  @GetMapping("/symbols/{symbol}/news")
  public List<NewsItemResponse> news(@PathVariable("symbol") String symbol) {
    return newsService.getNews(symbol);
  }
}
