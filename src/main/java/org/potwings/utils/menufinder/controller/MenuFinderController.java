package org.potwings.utils.menufinder.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.potwings.utils.menufinder.dto.StoreInfo;
import org.potwings.utils.menufinder.service.MenuFinderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(("/search/store"))
@RequiredArgsConstructor
public class MenuFinderController {

  private final MenuFinderService menuFinderService;

  @GetMapping
  public ResponseEntity<List<StoreInfo>> crawlMenu(@RequestParam String storeFindKeyword,
      @RequestParam String menu) {
    if (menu == null || menu.trim().isEmpty()) {
      return ResponseEntity.badRequest().body(List.of());
    }
    if (storeFindKeyword == null || storeFindKeyword.trim().isEmpty()) {
      return ResponseEntity.badRequest().body(List.of());
    }
    List<StoreInfo> results = menuFinderService.findMenuHaveStore(storeFindKeyword, menu);
    return ResponseEntity.ok(results);
  }

}
