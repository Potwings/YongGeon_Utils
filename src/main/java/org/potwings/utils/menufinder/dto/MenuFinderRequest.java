package org.potwings.utils.menufinder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MenuFinderRequest {

  // 찾으려는 음식점의 종류 (ex. 강남역 호프, 종로 술집)
  @NotBlank
  private String store;

  // 찾으려는 메뉴 (ex. 아이스크림, 치킨)
  @NotBlank
  private String menu;
}
