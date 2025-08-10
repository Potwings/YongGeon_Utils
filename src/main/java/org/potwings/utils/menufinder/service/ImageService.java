package org.potwings.utils.menufinder.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class ImageService {

  private final Map<String, Long> imageLastUsed = new ConcurrentHashMap<>();
  private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
  private final Path storageDirectory;

  public ImageService() {
    // 임시 디렉토리에 이미지 저장
    this.storageDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "menu-finder-images");
    try {
      Files.createDirectories(storageDirectory);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create storage directory", e);
    }

    // 30분마다 1시간 이상 된 이미지 정리
    cleanupExecutor.scheduleAtFixedRate(this::cleanupOldImages, 30, 30, TimeUnit.MINUTES);
  }

  public boolean isStoreImage(String imageId) {
    if (imageLastUsed.containsKey(imageId)) {
      // 이미지가 존재하면 마지막 사용 시간 갱신
      imageLastUsed.put(imageId, System.currentTimeMillis());
      return true;
    }
    return false;
  }

  /**
   * 음식점의 URL을 키 값으로하여 이미지 저장
   *
   * @param storeId  음식점 ID (음식점 URL 마지막 인자)
   * @param imageFile 음식점 대표 이미지
   */
  public void storeImage(String storeId, File imageFile) {
    try {
      Path targetPath = storageDirectory.resolve(storeId);
      Files.copy(imageFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
      imageLastUsed.put(storeId, System.currentTimeMillis());
      log.debug("Image stored with ID: {}", storeId);
    } catch (IOException e) {
      log.error("Failed to store image: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to store image", e);
    }
  }

  public byte[] getImage(String storeId) {
    if (imageLastUsed.containsKey(storeId)) {
      Path imagePath = storageDirectory.resolve(storeId);
      if (Files.exists(imagePath)) {
        // 접근 시간 업데이트
        imageLastUsed.put(storeId, System.currentTimeMillis());
        try {
          return Files.readAllBytes(imagePath);
        } catch (IOException e) {
          log.error("Failed to read image file: {}", e.getMessage(), e);
        }
      }
    }
    return null;
  }


  public void removeImage(String imageId) {
    imageLastUsed.remove(imageId);
    Path imagePath = storageDirectory.resolve(imageId);
    try {
      Files.deleteIfExists(imagePath);
      log.debug("Image removed with ID: {}", imageId);
    } catch (IOException e) {
      log.error("Failed to delete image file: {}", e.getMessage(), e);
    }
  }

  private void cleanupOldImages() {
    long currentTime = System.currentTimeMillis();
    long oneHourAgo = currentTime - TimeUnit.HOURS.toMillis(1);

    imageLastUsed.entrySet().removeIf(entry -> {
      boolean shouldRemove = entry.getValue() < oneHourAgo;
      if (shouldRemove) {
        log.debug("Removing old image with ID: {}", entry.getKey());
        removeImage(entry.getKey());
      }
      return shouldRemove;
    });
  }
}