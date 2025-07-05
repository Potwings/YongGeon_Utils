package org.potwings.utils.menufinder.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class MenuFinderService {

  public List<String> findMenuHaveStore(String findStore, String findMenu) {

    System.setProperty("webdriver.chrome.driver", "C:/zzz/chromedriver-win64/chromedriver.exe");

    List<String> findMenuList = new ArrayList<>();
    if (findMenu.contains(",")) {
      // 메뉴가 여러개 들어가 있는 경우
      String[] findMenus = findMenu.split(",");
      for (String findMenuItem : findMenus) {
        findMenuList.add(findMenuItem.trim());
      }
    } else {
      findMenuList.add(findMenu);
    }

    ChromeOptions options = new ChromeOptions();
    options.addArguments("--start-maximized");
    options.addArguments("--remote-allow-origins=*");
    WebDriver driver = new ChromeDriver(options);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    List<String> resultList = null;
    try {
      driver.get("https://map.naver.com");  // 네이버 지도 접속

      // 1. 찾으려는 음식점 검색
      WebElement inputSearch = wait.until(
          ExpectedConditions.presenceOfElementLocated(By.className("input_search")));
      inputSearch.clear();
      inputSearch.sendKeys(findStore);
      inputSearch.sendKeys(Keys.ENTER);

      WebElement searchIframe = wait.until(
          ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#searchIframe")));
      driver.switchTo().frame(searchIframe); // 검색 결과 iframe으로 이동

      resultList = new ArrayList<>();
      // 음식점 목록 끝 페이지까지 반복하여 진행
      while (true) {

        // 모든 음식점 목록을 불러오기 위해 스크롤 진행
        // 빠른 테스트를 위해 비활성화
        // TODO 향후 주석 해제할 것
        // scrollWholeList(driver, wait);

        // 음식점 목록 로딩 완료 후 결과 추출
        List<WebElement> storeList = driver.findElements(By.className("iHXbN"));
        for (WebElement store : storeList) {
          wait.until(ExpectedConditions.elementToBeClickable(store)).click();
          Thread.sleep(3000); // 음식점 클릭 후 로딩 대기

          // 음식점에 특정 메뉴 포함하고 있는지 여부 확인
          String matchedStore = checkStoreMenus(driver, wait, findMenuList);
          if (matchedStore != null) {
            resultList.add(matchedStore);
            // 빠른 테스트를 위해 음식점 검색 성공했을 경우 중단
            // TODO 향후 주석 제거할 것
            return resultList;
          }
          driver.switchTo().frame(searchIframe); // 검색 iframe으로 이동
        }

        // [이전 페이지, 다음 페이지] 버튼 목록 중 다음 페이지 선택
        WebElement nextPageBtn = driver.findElements(By.className("eUTV2")).get(1);
        boolean hasNextPage = nextPageBtn.getAttribute("aria-disabled").equals("false");
        if (!hasNextPage) {
          // 더 이상 확인할 다음 페이지가 없는 경우 중단
          break;
        }
        nextPageBtn.click(); // 다음 페이지로 이동
      }
    } catch (Exception e) {
      log.error("findMenu fail : {}", e.getMessage(), e);
    } finally {
      driver.quit();
    }
    return resultList;
  }

  /**
   * 무한 스크롤 방식에서 전체 목록을 불러오기 위해 목록 스크롤
   *
   * @param driver
   * @param wait
   * @throws InterruptedException
   */
  private void scrollWholeList(WebDriver driver, WebDriverWait wait) {
    JavascriptExecutor js = (JavascriptExecutor) driver;
    WebElement scrollDiv = wait.until(ExpectedConditions.visibilityOfElementLocated(
        By.cssSelector("#_pcmap_list_scroll_container")));

    long scrollTop = 0;
    while (true) {
      // 컨텐츠 로딩 속도를 위해 스크롤 천천히 아래로 이동
      js.executeScript("arguments[0].scrollTop += arguments[1];", scrollDiv, 700);

      // 약간의 대기 시간 (렌더링 시간 고려)
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ie) {
        log.debug("Thread Sleep fail : {}", ie.getMessage(), ie);
      }

      long newScrollTop = (long) js.executeScript("return arguments[0].scrollTop", scrollDiv);
      if (scrollTop == newScrollTop) {
        break;
      }
      scrollTop = newScrollTop;
    }
  }

  private String checkStoreMenus(WebDriver driver, WebDriverWait wait,
      List<String> findMenuList) {

    String matchResult = null;
    try {
      driver.switchTo().defaultContent(); // 다음 store 찾을 수 있도록 전체 화면으로 focus 이동
      WebElement storeIframe = wait.until(
          ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#entryIframe")));
      driver.switchTo().frame(storeIframe); // 음식점 iframe으로 이동

      // 음식점 페이지 중단 선택 메뉴
      List<WebElement> spans = driver.findElements(By.className("veBoZ"));
      for (WebElement span : spans) {
        // 그 중 메뉴 버튼 클릭
        if (span.getText().equals("메뉴")) {
          span.click();
          break;
        }
      }

      // 모든 메뉴 목록을 불러오기 위해 더보기 클릭
      WebElement moreMenu = null;
      // 메뉴가 하나라도 로딩될 떄까지 대기
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("lPzHi")));
      while (true) {
        try {
          moreMenu = driver.findElement(By.className("TeItc"));
        } catch (NoSuchElementException e) {
          break;
        }
        moreMenu.click();
        Thread.sleep(3000L); // 로딩 대기
      }

      // 메뉴 목록 추출
      List<WebElement> menuList = driver.findElements(By.className("lPzHi"));

      List<WebElement> findResult = menuList.stream()
          .filter(el -> {
            String text = el.getText();
            return findMenuList.stream().anyMatch(text::contains);
          })
          .toList();

      if (!findResult.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append("음식점 명 : ").append(driver.findElement(By.className("GHAhO")).getText());
        findResult.forEach(result -> sb.append(result).append(", "));
        matchResult = sb.toString();
      }

      driver.switchTo().defaultContent(); // 다음 store 검색을 위해 현재 store Iframe에서 빠져나옴
    } catch (Exception e) {
      log.error("메뉴 클릭 처리 중 오류 발생 : {}", e.getMessage(), e);
    }

    // 메뉴 없는 경우
    return matchResult;
  }
}
