package org.potwings.utils.menufinder.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.potwings.utils.menufinder.service.ImageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Log4j2
public class ImageController {

  private final ImageService imageService;

  @GetMapping("/{imageId}")
  public ResponseEntity<byte[]> getImage(@PathVariable String imageId) {
    byte[] imageData = imageService.getImage(imageId);
    
    if (imageData == null) {
      log.warn("Image not found with ID: {}", imageId);
      return ResponseEntity.notFound().build();
    }
    
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_PNG);
    headers.setContentLength(imageData.length);
    headers.setCacheControl("max-age=3600"); // 1시간 캐싱
    
    return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
  }
}