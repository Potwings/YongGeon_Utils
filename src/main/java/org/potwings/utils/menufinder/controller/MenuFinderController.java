package org.potwings.utils.menufinder.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.potwings.utils.menufinder.dto.MenuFinderRequest;
import org.potwings.utils.menufinder.service.MenuFinderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(("/store"))
@RequiredArgsConstructor
public class MenuFinderController {

  private final MenuFinderService menuFinderService;

  @PostMapping
  public ResponseEntity<List> crawlMenu(@RequestBody MenuFinderRequest request) {
    List<String> results = menuFinderService.findMenuHaveStore(request.getStore(),
        request.getMenu());
    return ResponseEntity.ok(results);
  }

}
